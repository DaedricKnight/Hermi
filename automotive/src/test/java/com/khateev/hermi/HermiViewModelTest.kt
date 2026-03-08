package com.khateev.hermi

import com.khateev.hermi.shared.interfaces.AIProcessor
import com.khateev.hermi.shared.interfaces.AudioRecorder
import com.khateev.hermi.shared.interfaces.RecognitionManager
import com.khateev.hermi.shared.interfaces.VersionManager
import com.khateev.hermi.shared.manager.VoiceInterfaceManager
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.viewmodel.HermiViewModel
import com.qubit.core_vhal.VhalParamsState
import com.qubit.core_vhal.repository.VhalReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
        
        `when`(vhalReader.carState).thenReturn(fakeCarState)
        // По умолчанию пользователь Pro, чтобы тесты команд проходили
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
        versionManager = versionManager
    )

    // Хелпер для типичного сценария — инит + transcribe
    private suspend fun TestScope.setupAndTranscribe(text: String, command: Command): HermiViewModel {
        val emptyFloatArray = FloatArray(0)
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(emptyFloatArray)
        `when`(voskManager.recognizeAudio(emptyFloatArray)).thenReturn(text)
        `when`(aiProcessor.parseCommand(anyString())).thenReturn(command)
        val vm = createViewModel()
        advanceUntilIdle()
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
        assertEquals("\uD83C\uDFA4 Ready to record", vm.transcription)
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
        vm.startRecording()
        advanceUntilIdle()
        assertTrue(vm.isRecording.value)
    }

    @Test
    fun `startRecording sets transcription to Recording`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.startRecording()
        advanceUntilIdle()
        assertEquals("Recording...", vm.transcription)
    }

    @Test
    fun `startRecording calls audioRecorder startRecording`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.startRecording()
        advanceUntilIdle()
        verify(audioRecorder).startRecording()
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
        val vm = setupAndTranscribe("find food", Command.FindFood)
        assertFalse(vm.isRecording.value)
    }

    @Test
    fun `stopRecordingAndTranscribe calls audioRecorder stopRecording`() = runTest {
        setupAndTranscribe("find food", Command.FindFood)
        verify(audioRecorder, atLeastOnce()).stopRecording()
    }

    @Test
    fun `stopRecordingAndTranscribe with null result sets failed transcription`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn(null)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        assertEquals("Failed to transcribe", vm.transcription)
    }

    @Test
    fun `stopRecordingAndTranscribe sets transcription to recognized text`() = runTest {
        val vm = setupAndTranscribe("check engine", Command.CheckEngineStatus)
        assertEquals("check engine", vm.transcription)
    }

    @Test
    fun `stopRecordingAndTranscribe calls aiProcessor with recognized text`() = runTest {
        setupAndTranscribe("check engine", Command.CheckEngineStatus)
        verify(aiProcessor).parseCommand("check engine")
    }

    @Test
    fun `stopRecordingAndTranscribe sets isProcessing false after completion`() = runTest {
        val vm = setupAndTranscribe("find food", Command.FindFood)
        assertFalse(vm.isProcessing.value)
    }

    @Test
    fun `stopRecordingAndTranscribe exception sets error transcription`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenThrow(RuntimeException("Audio error"))
        val vm = createViewModel()
        advanceUntilIdle()
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        assertEquals("Error: Audio error", vm.transcription)
    }

    @Test
    fun `stopRecordingAndTranscribe exception sets isProcessing false`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenThrow(RuntimeException("fail"))
        val vm = createViewModel()
        advanceUntilIdle()
        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()
        assertFalse(vm.isProcessing.value)
    }

    @Test
    fun `FindFood speaks about restaurants`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("find food", Command.FindFood)
        assertTrue(spokenText.contains("restaurant"))
    }

    @Test
    fun `FindGasStation speaks about gas stations`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("find gas", Command.FindGasStation)
        assertTrue(spokenText.contains("gas"))
    }

    @Test
    fun `FindChargingStation speaks about charging`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("find charging", Command.FindChargingStation)
        assertTrue(spokenText.contains("charging"))
    }

    @Test
    fun `FindParking speaks about parking`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("find parking", Command.FindParking)
        assertTrue(spokenText.contains("parking"))
    }

    @Test
    fun `Unknown command speaks unknown command`() = runTest {
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        setupAndTranscribe("xyzzy", Command.Unknown("xyzzy"))
        assertEquals("Sorry, I didn't understand that command", spokenText)
    }

    // ────────────────────────────────────────────────
    // navigationEvent TESTS
    // ────────────────────────────────────────────────

    @Test
    fun `FindFood emits OpenNavigation with restaurants`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn("find food")
        `when`(aiProcessor.parseCommand("find food")).thenReturn(Command.FindFood)
        val vm = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<NavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()

        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "restaurants" })
        job.cancel()
    }

    @Test
    fun `FindGasStation emits OpenNavigation with gas stations`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn("find gas")
        `when`(aiProcessor.parseCommand("find gas")).thenReturn(Command.FindGasStation)
        val vm = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<NavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()

        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "gas stations" })
        job.cancel()
    }

    @Test
    fun `FindParking emits OpenNavigation with parking`() = runTest {
        `when`(voskManager.initialize()).thenReturn(true)
        `when`(audioRecorder.stopRecording()).thenReturn(FloatArray(0))
        `when`(voskManager.recognizeAudio(FloatArray(0))).thenReturn("find parking")
        `when`(aiProcessor.parseCommand("find parking")).thenReturn(Command.FindParking)
        val vm = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<NavigationEvent>()
        val job = launch { vm.navigationEvent.collect { events.add(it) } }

        vm.stopRecordingAndTranscribe()
        advanceUntilIdle()

        assertTrue(events.any { it is NavigationEvent.OpenNavigation && it.searchQuery == "parking" })
        job.cancel()
    }

    @Test
    fun `CheckDrivingRange speaks current range from carState`() = runTest {
        fakeCarState.value = VhalParamsState(range = 350f)
        setupAndTranscribe("get range", Command.CheckDrivingRange)
        val captor = argumentCaptor<String>()
        verify(voiceManager).speak(captor.capture())
        assertTrue(captor.firstValue.contains("350"))
    }

    @Test
    fun `CheckOdometer speaks odometer from carState`() = runTest {
        fakeCarState.value = VhalParamsState(odometer = 12345f)
        setupAndTranscribe("get odometer", Command.CheckOdometer)
        val captor = argumentCaptor<String>()
        verify(voiceManager).speak(captor.capture())
        assertTrue(captor.firstValue.contains("12345"))
    }

    @Test
    fun `CheckOutsideTemperature speaks temperature with unit`() = runTest {
        fakeCarState.value = VhalParamsState(outsideTemp = 22f, tempUnit = "Celsius")
        setupAndTranscribe("get temp", Command.CheckOutsideTemperature)
        val captor = argumentCaptor<String>()
        verify(voiceManager).speak(captor.capture())
        assertTrue(captor.firstValue.contains("22"))
        assertTrue(captor.firstValue.contains("Celsius"))
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
        
        setupAndTranscribe("check engine", Command.CheckEngineStatus)
        
        assertTrue(spokenText.contains("upgrade to Pro"))
    }

    @Test
    fun `PRO command for PRO user executes normally`() = runTest {
        `when`(versionManager.isProNow()).thenReturn(true)
        fakeCarState.value = VhalParamsState(isCheckEngineOn = false, coolantTemp = 90f, oilLevel = 85f)
        
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        
        setupAndTranscribe("check engine", Command.CheckEngineStatus)
        
        assertTrue(spokenText.contains("normally"))
    }

    @Test
    fun `FREE command for FREE user executes normally`() = runTest {
        `when`(versionManager.isProNow()).thenReturn(false)
        
        var spokenText = ""
        Mockito.doAnswer { spokenText = it.arguments[0] as String; null }
            .`when`(voiceManager).speak(anyString())
        
        setupAndTranscribe("find food", Command.FindFood)
        
        assertTrue(spokenText.contains("restaurant"))
    }
}
