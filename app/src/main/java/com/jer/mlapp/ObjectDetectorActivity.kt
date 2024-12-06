package com.jer.mlapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jer.mlapp.databinding.ActivityObjectDetectorBinding
import com.jer.mlapp.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class ObjectDetectorActivity : AppCompatActivity() {


    private lateinit var imageProcessor: ImageProcessor
    private lateinit var binding: ActivityObjectDetectorBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var cameraDevice: CameraDevice
    private lateinit var bitmap: Bitmap
    private lateinit var model: SsdMobilenetV11Metadata1
    val paint = Paint()
    var color = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )
    private lateinit var labels: List<String>


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission() ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Izin kamera diberikan", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityObjectDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCameraPermission()
        setTextureListener()

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        model = SsdMobilenetV11Metadata1.newInstance(this)
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        labels = FileUtil.loadLabels(this, "labels.txt")

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
//        handler = Handler(handler.looper)
        handlerThread.looper?.let { looper ->
            handler = Handler(looper)
        } ?: throw IllegalStateException("Looper tidak tersedia untuk HandlerThread")



    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object :
            CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                var surfaceTexture = binding.textureView.surfaceTexture
                var surface = Surface(surfaceTexture)
                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                TODO("Not yet implemented")
            }

        }, handler)
    }

    private fun setTextureListener() {
        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {




            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = binding.textureView.bitmap!!


                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/15f
                paint.strokeWidth = h/85f
                var x = 0

                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if (fl > 0.5) {
                        paint.setColor(color.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect( RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels.get(classes.get(index).toInt()) + " " + fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                    }
                }

                binding.imageView.setImageBitmap(mutable)


            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    fun getCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getCameraPermission()
        }
    }








    private fun checkSelfCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this,
                    "Izin kamera diperlukan untuk mengambil gambar",
                    Toast.LENGTH_SHORT
                ).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun open_camera() {
        val intentCamera = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intentCamera.resolveActivity(packageManager) != null) {
            startActivityForResult(intentCamera, REQUEST_IMAGE_CAPTURE)
        }
    }



}