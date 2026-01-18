package com.example.kuladig_app.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kuladig_app.R

@Composable
fun AudioPlayer(
    audioFileName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(audioFileName) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }
    
    if (audioFileName == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Keine Audio-Datei verfügbar")
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            if (errorMessage != null) {
                Text(errorMessage ?: "Fehler beim Laden der Audio-Datei")
            } else {
                IconButton(
                    onClick = {
                        try {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (mediaPlayer == null) {
                                    // Lade Audio-Datei aus Assets
                                    val assetManager = context.assets
                                    val fileDescriptor = assetManager.openFd(audioFileName)
                                    
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
                                        prepare()
                                        setOnCompletionListener {
                                            isPlaying = false
                                        }
                                        setOnErrorListener { _, what, extra ->
                                            Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                                            errorMessage = "Fehler beim Abspielen der Audio-Datei"
                                            true
                                        }
                                    }
                                    fileDescriptor.close()
                                }
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            Log.e("AudioPlayer", "Error playing audio", e)
                            errorMessage = "Fehler: ${e.message}"
                        }
                    }
                ) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoFileName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(videoFileName) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }
    
    if (videoFileName == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Keine Video-Datei verfügbar")
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (errorMessage != null) {
                Text(errorMessage ?: "Fehler beim Laden der Video-Datei")
            } else {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            try {
                                // Lade Video-Datei aus Assets
                                val assetManager = context.assets
                                val fileDescriptor = assetManager.openFd(videoFileName)
                                val uri = Uri.parse("file://${fileDescriptor.fileDescriptor}")
                                
                                setVideoURI(uri)
                                setOnErrorListener { _, what, extra ->
                                    Log.e("VideoPlayer", "VideoView error: what=$what, extra=$extra")
                                    errorMessage = "Fehler beim Abspielen der Video-Datei"
                                    true
                                }
                                
                                fileDescriptor.close()
                            } catch (e: Exception) {
                                Log.e("VideoPlayer", "Error loading video", e)
                                errorMessage = "Fehler: ${e.message}"
                            }
                            videoView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    update = { view ->
                        // Update video view if needed
                    }
                )
            }
        }
    }
}
