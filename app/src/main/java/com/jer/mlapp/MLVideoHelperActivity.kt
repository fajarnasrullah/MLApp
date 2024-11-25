package com.jer.mlapp

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.jer.mlapp.databinding.ActivityMlVideoHelperBinding
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors


abstract class MLVideoHelperActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMlVideoHelperBinding

    val REQUEST_CAMERA: Int = 1001
    protected var previewView: PreviewView? = null
    protected var graphicOverlay: GraphicOverlay? = null
    private var outputTextView: TextView? = null
    private var addFaceButton: ExtendedFloatingActionButton? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var executor: Executor = Executors.newSingleThreadExecutor()

    private var processor: VisionBaseProcessor<*>? = null
    private var imageAnalysis: ImageAnalysis? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMlVideoHelperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)

        processor = setPrecessor()



    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initSource()
        }
    }

    private fun initSource() {
        cameraProviderFuture!!.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(applicationContext))
    }

    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val lensFacing: Int = getLensFacing()
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        preview.surfaceProvider = previewView!!.surfaceProvider

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        imageAnalysis =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

//        setFaceDetector(lensFacing)
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
    }

    private fun getLensFacing(): Int {
        return CameraSelector.LENS_FACING_BACK
    }

    override fun onDestroy() {
        super.onDestroy()
        if (processor != null) {
            processor!!.stop()
        }
    }

    private fun toBitmap(image: Image): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)

        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun setOutputText(text: String) {
        binding.outputTextView.text = text
    }

    private fun makeAddFaceButton() {
        binding.buttonAddFace.visibility = View.VISIBLE
    }

    abstract fun setPrecessor(): VisionBaseProcessor<*>

    private fun onAddFaceClicked(view: View) {}

}