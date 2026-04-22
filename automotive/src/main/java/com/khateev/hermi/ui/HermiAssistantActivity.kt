package com.khateev.hermi.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class HermiAssistantActivity : AppCompatActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val viewModel: CommandListViewModel by viewModels()

    private fun checkAndRequestMicrophone() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            log("Request permission for mic")
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            log("Start STT")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       checkAndRequestMicrophone()

        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

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


                LaunchedEffect(Unit) {
                    viewModel.commandExecutionEvent.collect { event ->
                        handleCommandEvent(event)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                log("Granted")
            } else {
                log("Not granted")
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun handleCommandEvent(event: Any) {
        when (event) {
            is CommandExecutionEvent.Execute -> {
                // TODO: Execute command
                showToast("Executing command: ${event.command}")
            }
            is CommandExecutionEvent.ShowProRequiredDialog -> {
                showDialog(
                    title = "Pro Feature Required",
                    message = "The command '${event.commandName}' is not included in the free package.\n\n" +
                            "Upgrade to Pro to access this feature.",
                    positiveButtonText = "Upgrade",
                    onPositiveClick = { showUpgradeDialog() }
                )
            }
            is CommandExecutionEvent.ShowUnavailableDialog -> {
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
     * Show dialog for purchasing Pro version
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
                    "Only $6.99/month",
            positiveButtonText = "Upgrade Now",
            onPositiveClick = {
                // TODO: Integrate Google Play Billing
                showToast("Opening Google Play Billing...")
            },
            negativeButtonText = "Maybe Later"
        )
    }

    /**
     * Show dialog
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
     * Show toast message
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun log(message: String) = Log.d("HermiAssistant", message)