package com.thesouravverse.mechar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.thesouravverse.mechar.ui.GameScreen
import com.thesouravverse.mechar.ui.PermissionGate
import com.thesouravverse.mechar.ui.theme.MechARTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MechARTheme {
                var cameraGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> cameraGranted = granted }

                if (cameraGranted) {
                    GameScreen()
                } else {
                    PermissionGate(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
                }
            }
        }
    }
}
