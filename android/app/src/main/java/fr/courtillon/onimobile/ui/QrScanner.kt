package fr.courtillon.onimobile.ui

import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors

/** The back camera reading QR codes; the first `oni-link://` payload is handed over once. */
@Composable
fun QrScanner(modifier: Modifier = Modifier, found: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val deliver by rememberUpdatedState(found)
    val provider = remember { ProcessCameraProvider.getInstance(context) }
    val worker = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { viewContext ->
            val view = PreviewView(viewContext).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            provider.addListener({
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val main = Handler(Looper.getMainLooper())
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(worker, QrAnalyzer { code -> main.post { deliver(code) } }) }
                // A phone without a back camera keeps the paste field rather than crash.
                runCatching {
                    provider.get().apply {
                        unbindAll()
                        bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                }
            }, ContextCompat.getMainExecutor(viewContext))
            view
        },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            if (provider.isDone) provider.get().unbindAll()
            worker.shutdown()
        }
    }
}

/** Reads the luminance plane alone: a QR code needs no colour, and YUV's first plane is exactly that. */
private class QrAnalyzer(private val found: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)
    private var done = false

    override fun analyze(image: ImageProxy) {
        image.use {
            if (done) return
            val plane = image.planes[0]
            val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
            val source = PlanarYUVLuminanceSource(
                bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false,
            )
            try {
                val text = reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
                if (text.startsWith("oni-link://")) {
                    done = true
                    found(text)
                }
            } catch (_: ReaderException) {
                // No code in this frame.
            } finally {
                reader.reset()
            }
        }
    }
}
