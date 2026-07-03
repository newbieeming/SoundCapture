package com.newbieeming.soundcapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.newbieeming.soundcapture.ui.screens.RecordingScreen
import com.newbieeming.soundcapture.ui.theme.SoundCaptureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundCaptureTheme {
                RecordingScreen()
            }
        }
    }
}