package com.kwos.dronepilotapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ImpostazioniActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()
    }
}
