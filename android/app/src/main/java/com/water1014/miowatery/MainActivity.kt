package com.water1014.miowatery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.water1014.miowatery.ui.MioWateryApp
import com.water1014.miowatery.ui.theme.MioWateryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MioWateryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MioWateryApp()
                }
            }
        }
    }
}
