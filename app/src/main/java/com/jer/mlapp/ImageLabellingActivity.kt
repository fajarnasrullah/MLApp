package com.jer.mlapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabelerOptionsBase
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.jer.mlapp.databinding.ActivityImageLabellingBinding
import java.io.ByteArrayOutputStream
import java.io.IOException

class ImageLabellingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageLabellingBinding

    private lateinit var imageLabelling: ImageLabeler

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageLabellingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnUpload.setOnClickListener {
            openGallery()
        }

        imageLabelling = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7F)
            .build()
        )

        checkCameraPermissionAndOpenCamera()

        binding.btnCamera.setOnClickListener {
            openCamera()
        }



    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Izin kamera diberikan", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            binding.imageFromGallery.setImageBitmap(imageBitmap)
            val imageByteArray = convertBitmapToByteArray(imageBitmap)
            getImageFromDB(imageByteArray)
            startClassification(imageBitmap)
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }

    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
//                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    this,
                    "Izin kamera diperlukan untuk mengambil gambar",
                    Toast.LENGTH_SHORT
                ).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                // Minta izin langsung
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun getImageFromDB(byteArray: ByteArray?) {
        byteArray?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.imageFromGallery.setImageBitmap(bitmap)
        }
    }


    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncherGallery.launch(galleryIntent)
    }

    private var resultLauncherGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                val imageUri = data!!.data!!
                binding.imageFromGallery.setImageURI(imageUri)
                convertUriToByteArray(imageUri)
                val byteArray = convertUriToByteArray(imageUri)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

                startClassification(bitmap)

            } else {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show()
            }
        }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun convertUriToByteArray(uri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { convertBitmapToByteArray(it) }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun startClassification(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabelling.process(inputImage)
            .addOnSuccessListener { imageLabel ->
                if (imageLabel.size >  0) {
                    val stringBuilder = StringBuilder()
                    for (label in imageLabel) {
                        stringBuilder
                            .append(label.text)
                            .append(" : ")
                            .append(label.confidence)
                            .append("\n")
                    }
                    binding.tvResult.text = stringBuilder.toString()
                } else {
                    binding.tvResult.text = "Could not classfy"
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show()
            }
    }

}