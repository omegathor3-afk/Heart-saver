package com.omegathor3.heartsaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HeartSaverApp() }
    }
}

@Composable
private fun HeartSaverApp() {
    val background = Color(0xFF090D12)
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFFF5268),
        background = background,
        surface = Color(0xFF141B23),
        onBackground = Color(0xFFEAF0F6),
        onSurface = Color(0xFFEAF0F6)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).background(background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HEART-SAVER", color = Color(0xFFFF5268), fontSize = 13.sp, letterSpacing = 4.sp)
                Spacer(Modifier.height(28.dp))
                Text("Your pulse, measured\nwith care.", color = Color(0xFFEAF0F6),
                    fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Text("Camera-based fingertip PPG is being prepared.",
                    color = Color(0xFF9AA8B7), fontSize = 14.sp)
                Spacer(Modifier.height(34.dp))
                Text("Measurement is not available yet",
                    color = Color(0xFFFFC46B), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                Text("This app will show a result only when signal quality is sufficient. It is not a medical diagnosis.",
                    color = Color(0xFF9AA8B7), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
