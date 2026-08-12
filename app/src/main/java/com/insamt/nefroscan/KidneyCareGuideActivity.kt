package com.insamt.nefroscan

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity

class KidneyCareGuideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kidney_care_guide)

        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverGuia)
        btnVolver.setOnClickListener { finish() }
    }
}