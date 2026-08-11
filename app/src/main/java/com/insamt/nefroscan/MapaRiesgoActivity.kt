package com.insamt.nefroscan

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MapaRiesgoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_riesgo)

        val btnVolver = findViewById<Button>(R.id.btnVolverMenu)
        btnVolver.setOnClickListener {
            finish()
        }
    }
}