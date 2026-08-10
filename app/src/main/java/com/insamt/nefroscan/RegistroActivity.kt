package com.insamt.nefroscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombrePaciente)
        val etEdad = findViewById<EditText>(R.id.etEdadPaciente)
        val btnIniciar = findViewById<Button>(R.id.btnIniciarEscaneo)

        btnIniciar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val edadString = etEdad.text.toString().trim()

            if (nombre.isEmpty() || edadString.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Corrección aplicada: Uso de toIntOrNull para prevenir crashes y pantallas blancas por conversión frágil
            val edadInt = edadString.toIntOrNull()
            if (edadInt == null) {
                Toast.makeText(this, "Por favor introduce una edad válida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXTRA_NOMBRE", nombre)
                putExtra("EXTRA_EDAD", edadInt)
            }
            startActivity(intent)
        }
    }
}