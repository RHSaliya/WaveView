package com.rhs.waveviewapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rhs.waveview.WaveView
import com.rhs.waveview.rememberPainterAsImageBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                ) {
                    WaveDemo()
                }
            }
        }
    }
}

@Composable
private fun WaveDemo() {
    val primary = colorResource(id = R.color.colorPrimary)
    val mask = rememberPainterAsImageBitmap(
        painter = painterResource(id = R.drawable.ic_android),
        width = 240.dp,
        height = 240.dp,
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WaveView(
            modifier = Modifier.fillMaxSize(),
            numberOfWaves = 5,
            frequency = 2.0f,
            amplitude = 10.25f,
            phaseShift = -0.05f,
            density = 5.0f,
            primaryWaveLineWidth = 3.0f,
            secondaryWaveLineWidth = 1.0f,
            backgroundColor = Color.White,
            waveColor = primary,
            xAxisPositionMultiplier = 0.5f,
            mask = mask,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WaveDemoPreview() {
    MaterialTheme {
        WaveDemo()
    }
}
