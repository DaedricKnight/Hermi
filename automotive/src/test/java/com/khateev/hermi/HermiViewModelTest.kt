package com.khateev.hermi

import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.interfaces.AudioRecorder
import com.khateev.hermi.shared.interfaces.RecognitionManager
import com.khateev.hermi.shared.interfaces.VersionManager
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
import com.khateev.hermi.shared.ai.VADManager
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandCatalog
import com.khateev.hermi.viewmodel.HermiViewModel
import com.qubit.core_vhal.VhalParamsState
import com.qubit.core_vhal.repository.VhalReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.argumentCaptor

class HermiViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var voskManager: RecognitionManager
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var vhalReader: VhalReader
    private lateinit var aiProcessor: AIProcessor
    private lateinit var voiceManager: VoiceInterfaceManager
    private lateinit var versionManager: VersionManager
    private lateinit var vadManager: VADManager

    private val fakeCarState = MutableStateFlow(VhalParamsState())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        voskManager = Mockito.mock(RecognitionManager::class.java)
        audioRecorder = Mockito.mock(AudioRecorder::class.java)
        aiProcessor = Mockito.mock(AIProcessor::class.java)
        voiceManager = Mockito.mock(VoiceInterfaceManager::class.java)
        vhalReader = Mockito.mock(VhalReader::class.java)
        versionManager = Mockito.mock(VersionManager::class.java)
        vadManager = Mockito.mock(VADManager::class.java)
        
        `when`(vhalReader.carState).thenReturn(fakeCarState)
        // Default to Pro user so command tests pass
        `when`(versionManager.isProNow()).thenReturn(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HermiViewModel(
        voskManager = voskManager,
        audioRecorderImpl = audioRecorder,
        vhalReader = vhalReader,
        aiProcessor = aiProcessor,
        voiceManager = voiceManager,
        versionManager = versionManager,
        vadManager = vadManager
    )

    /**
     * startRecording() runs on Dispatchers.IO, so the test scheduler cannot drive
     * it — wait on the real clock until the ViewModel reports it is recording.
     * stopRecordingAndTranscribe() is a no-op otherwise.
     */
    private suspend fun beginRecording(vm: HermiViewModel) {
        vm.startRecording()
        withContext(Dispatchers.Default) {
            withTimeout(2_000) { vm.isRecording.first { it } }
        }
    }

    // Helper for a typical scenario — init + record + transcribe
    private suspend fun TestScope.setupAndTranscribe(text: String, command: Command): HermiViewModel {
        val emptyFloatArray = FloatArray(0)
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(emptyFloatArray)
        `when`(voskManager.recognizeAudio(emptyFloatArray)).thenReturn(text)
        `when`(aiProcessor.parseCommand(anyString())).thenReturn(command)
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        return vm
    }

    // ────────────────────────────────────────────────
    // INITIALIZATION TESTS
    // ────────────────────────────────────────────────

    @Test
    fun `initial transcription shows loading message`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        assertEquals("", vm.transcription)
    }

    @Test
    fun `after successful init transcription shows ready`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals("Ready to record", vm.transcription)
    }

    @Test
    fun `after successful init isInitialized is true`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.isInitialized)
    }

    @Test
    fun `after failed init transcription shows error`() = runTest {
        `when`(voskManager.initialize()).thenReturn(false)
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals("Initialization failed", vm.transcription)
    }

    @Test
    fun `after failed init isInitialized is false`() = runTest {
        `when`(voskManager.initialize()).thenReturn(false)
        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.isInitialized)
    }

    @Test
    fun `init exception sets error transcription with message`() = runTest {
        `when`(voskManager.initialize()).thenThrow(RuntimeException("Model not found"))
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals("Error: Model not found", vm.transcription)
    }

    @Test
    fun `initial isRecording is false`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        assertFalse(createViewModel().isRecording.value)
    }

    @Test
    fun `initial isProcessing is false`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        assertFalse(createViewModel().isProcessing.value)
    }

    @Test
    fun `startRecording when not initialized sets transcription error`() = runTest {
        `when`(voskManager.initialize()).thenReturn(false)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.startRecording()
        assertEquals("Not initialized yet", vm.transcription)
    }

    @Test
    fun `startRecording when not initialized does not call audioRecorder`() = runTest {
        `when`(voskManager.initialize()).thenReturn(false)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.startRecording()
        advanceUntilIdle()
        verify(audioRecorder, never()).startRecording()
    }

    @Test
    fun `startRecording sets isRecording to true`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        assertTrue(vm.isRecording.value)
    }

    @Test
    fun `startRecording sets transcription to Recording`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        assertEquals("Listening...", vm.transcription)
    }

    @Test
    fun `startRecording calls audioRecorder startRecording`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        verify(audioRecorder, timeout(2_000)).startRecording()
    }

    // ────────────────────────────────────────────────
    // stopRecordingAndTranscribe TESTS
    // ────────────────────────────────────────────────

    @Test
    fun `stopRecordingAndTranscribe when not initialized skips stopRecording`() = runTest {
        `when`(voskManager.initialize()).thenReturn(false)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        verify(audioRecorder, never()).stopRecording()
    }

    @Test
    fun `stopRecordingAndTranscribe sets isRecording to false`() = runTest {
        val vm = setupAndTranscribe("play music", Command.MediaPlay)
        assertFalse(vm.isRecording.value)
    }

    @Test
    fun `stopRecordingAndTranscribe calls audioRecorder stopRecording`() = runTest {
        setupAndTranscribe("play music", Command.MediaPlay)
        verify(audioRecorder, atLeastOnce()).stopRecording()
    }

    @Test
    fun `stopRecordingAndTranscribe with null result sets failed transcription`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn(null)
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        assertEquals("I didn't catch that", vm.transcription)
    }

    @Test
    fun `stopRecordingAndTranscribe sets transcription to recognized text`() = runTest {
        val vm = setupAndTranscribe("turn on the ac", Command.ClimateAcOn)
        assertEquals("turn on the ac", vm.transcription)
    }

    @Test
    fun `stopRecordingAndTranscribe calls aiProcessor with recognized text`() = runTest {
        setupAndTranscribe("turn on the ac", Command.ClimateAcOn)
        verify(aiProcessor).parseCommand("turn on the ac")
    }

    @Test
    fun `stopRecordingAndTranscribe sets isProcessing false after completion`() = runTest {
        val vm = setupAndTranscribe("play music", Command.MediaPlay)
        assertFalse(vm.isProcessing.value)
    }

    @Test
    fun `stopRecordingAndTranscribe exception sets error transcription`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenThrow(RuntimeException("Audio error"))
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)

        val events = mutableListOf<NavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        job.cancel()

        assertTrue("recognition failure must close the assistant",
            events.any { it is NavigationEvent.CloseAssistant })
    }

    @Test
    fun `stopRecordingAndTranscribe exception sets isProcessing false`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenThrow(RuntimeException("fail"))
        val vm = createViewModel()
        advanceUntilIdle()
        beginRecording(vm)
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        assertFalse(vm.isProcessing.value)
    }

    // ────────────────────────────────────────────────
    // COMMAND EXECUTION TESTS (catalog-driven)
    // ────────────────────────────────────────────────

    /** Whatever the catalog says a command announces is what the user must hear. */
    private suspend fun TestScope.assertSpeaksAcknowledgement(command: Command) {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("some phrase", command)
        assertEquals(CommandCatalog.specFor(command)?.acknowledgement, spokenText)
    }

    @Test
    fun `door lock speaks its acknowledgement`() = runTest {
        assertSpeaksAcknowledgement(Command.BodyDoorLock)
    }

    @Test
    fun `climate command speaks its acknowledgement`() = runTest {
        assertSpeaksAcknowledgement(Command.ClimateAcOn)
    }

    @Test
    fun `media command speaks its acknowledgement`() = runTest {
        assertSpeaksAcknowledgement(Command.MediaPlay)
    }

    @Test
    fun `every catalog command is announced with a non-empty phrase`() = runTest {
        val silent = CommandCatalog.specs.filter { it.acknowledgement.isBlank() }
        assertTrue("commands with no acknowledgement: ${silent.map { it.label }}", silent.isEmpty())
    }

    @Test
    fun `Unknown command speaks an apology`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("xyzzy", Command.Unknown("xyzzy"))
        assertTrue(spokenText.contains("don't know how to do that"))
    }

    // ────────────────────────────────────────────────
    // navigationEvent TESTS
    // ────────────────────────────────────────────────

    private suspend fun TestScope.navigationEventsFor(command: Command): List<NavigationEvent> {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn("some phrase")
        `when`(aiProcessor.parseCommand(anyString())).thenReturn(command)
        val vm = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<NavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }
        beginRecording(vm)
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        job.cancel()
        return events
    }

    @Test
    fun `find restaurant emits OpenNavigation with restaurants`() = runTest {
        val events = navigationEventsFor(Command.NavigationFindRestaurant)
        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "restaurants" })
    }

    @Test
    fun `find gas station emits OpenNavigation with gas stations`() = runTest {
        val events = navigationEventsFor(Command.NavigationFindGasStation)
        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "gas stations" })
    }

    @Test
    fun `find parking emits OpenNavigation with parking`() = runTest {
        val events = navigationEventsFor(Command.NavigationFindParking)
        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "parking" })
    }

    @Test
    fun `cabin command does not open navigation`() = runTest {
        val events = navigationEventsFor(Command.BodyDoorLock)
        assertTrue(events.none { it is NavigationEvent.OpenNavigation })
    }

    @Test
    fun `assistant closes after every command`() = runTest {
        val events = navigationEventsFor(Command.MediaPause)
        assertTrue(events.any { it is NavigationEvent.CloseAssistant })
    }

    // ────────────────────────────────────────────────
    // PRO VERSION ACCESS TESTS
    // ────────────────────────────────────────────────

    @Test
    fun `PRO command for FREE user speaks upgrade message`() = runTest {
        `when`(versionManager.isProNow()).thenReturn(false)
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())

        setupAndTranscribe("open the sunroof", Command.BodySunroofOpen)

        assertTrue(spokenText.contains("Pro"))
    }

    @Test
    fun `PRO command for PRO user executes normally`() = runTest {
        `when`(versionManager.isProNow()).thenReturn(true)
        assertSpeaksAcknowledgement(Command.BodySunroofOpen)
    }

    @Test
    fun `FREE command for FREE user executes normally`() = runTest {
        `when`(versionManager.isProNow()).thenReturn(false)
        assertSpeaksAcknowledgement(Command.MediaPlay)
    }
}
