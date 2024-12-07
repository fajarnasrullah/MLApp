package com.jer.mlapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jer.mlapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnImageLabelling.setOnClickListener {
            val intent = Intent(this, ImageLabellingActivity::class.java)
            startActivity(intent)
        }

        binding.btnTextRecognition.setOnClickListener {
            val intent = Intent(this, TextRecognitionActivity::class.java)
            startActivity(intent)
        }

        binding.btnObjectDetector.setOnClickListener {
            val intent = Intent(this, ObjectDetectorActivity::class.java)
            startActivity(intent)
        }

    }
}