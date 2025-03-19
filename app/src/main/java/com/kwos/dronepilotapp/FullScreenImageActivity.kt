package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView
import com.squareup.picasso.Picasso

class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        //val imageView: ImageView = findViewById(R.id.fullScreenImage)
        val imageView: PhotoView = findViewById(R.id.fullScreenImage)
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        imageUrl?.let {
            Picasso.get().load(it).into(imageView)
        }

        // Chiudi l'activity con un click sull'immagine
        imageView.setOnClickListener {
            finish()
        }
    }
}
