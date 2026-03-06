package com.khateev.hermi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.khateev.hermi.ui.CommandListScreen
import com.khateev.hermi.ui.CommandListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val viewModel: CommandListViewModel by viewModels()

    private fun checkAndRequestMicrophone() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestMicrophone()
        enableEdgeToEdge()

        requestPermissions(arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            "android.car.permission.CAR_INFO"
        ), 101)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CommandListScreen(
                        versionManager = viewModel.versionManager,
                        onCommandClick = { command ->
                            viewModel.onCommandClick(command)
                        },
                        onUpgradeClick = {
                            viewModel.onUpgradeClick()
                        }
                    )
                }
            }

            LaunchedEffect(Unit) {
                viewModel.commandExecutionEvent.collect { event ->
                    handleCommandEvent(event)
                }
            }
        }
    }

    /**
     * Обработчик событий команд
     */
    private fun handleCommandEvent(event: Any) {
        when (event) {
            is com.khateev.hermi.ui.CommandExecutionEvent.Execute -> {
                // TODO: Выполнить команду
                showToast("Executing command: ${event.command}")
            }
            is com.khateev.hermi.ui.CommandExecutionEvent.ShowProRequiredDialog -> {
                showDialog(
                    title = "Pro Feature Required",
                    message = "The command '${event.commandName}' is not included in the free package.\n\n" +
                            "Upgrade to Pro to access this feature.",
                    positiveButtonText = "Upgrade",
                    onPositiveClick = { showUpgradeDialog() }
                )
            }
            is com.khateev.hermi.ui.CommandExecutionEvent.ShowUnavailableDialog -> {
                showDialog(
                    title = "Command Unavailable",
                    message = "This command is not available at the moment."
                )
            }
            is com.khateev.hermi.ui.CommandExecutionEvent.ShowUpgradeDialog -> {
                showUpgradeDialog()
            }
        }
    }

    /**
     * Показать диалог для покупки Pro версии
     */
    private fun showUpgradeDialog() {
        showDialog(
            title = "Upgrade to Pro",
            message = "Get access to all premium features including:\n\n" +
                    "• Vehicle Status Check\n" +
                    "• Engine Diagnostics\n" +
                    "• Tire & Fuel Monitoring\n" +
                    "• Vehicle Control (Lock/Unlock)\n" +
                    "• And more...\n\n" +
                    "Only \$4.99/month",
            positiveButtonText = "Upgrade Now",
            onPositiveClick = {
                // TODO: Интегрировать Google Play Billing
                showToast("Opening Google Play Billing...")
            },
            negativeButtonText = "Maybe Later"
        )
    }

    /**
     * Показать диалог
     */
    private fun showDialog(
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        negativeButtonText: String = "Cancel",
        onPositiveClick: () -> Unit = {},
        onNegativeClick: () -> Unit = {}
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick() }
            .setNegativeButton(negativeButtonText) { _, _ -> onNegativeClick() }
            .show()
    }

    /**
     * Показать toast сообщение
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}