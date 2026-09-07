package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegistroActivity : AppCompatActivity() {

    private var registradorId: String = ""
    private var registradorRol: String = "MEDICO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
        registradorId = intent.getStringExtra("EXTRA_REGISTRADOR_ID")
            ?: prefs.getString("ID_USUARIO", "medico@nefroscan.sv") ?: "medico@nefroscan.sv"
        registradorRol = intent.getStringExtra("EXTRA_ROL")
            ?: prefs.getString("ROL_USUARIO", "MEDICO") ?: "MEDICO"

        val etNombre = findViewById<TextInputEditText>(R.id.etNombrePaciente)
        val etIdPaciente = findViewById<TextInputEditText>(R.id.etIdPacienteDui)
        val etEdad = findViewById<TextInputEditText>(R.id.etEdadPaciente)
        val btnIniciar = findViewById<MaterialButton>(R.id.btnIniciarEscaneo)

        btnIniciar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val idPaciente = etIdPaciente.text.toString().trim()
            val edadString = etEdad.text.toString().trim()

            if (nombre.isEmpty() || idPaciente.isEmpty() || edadString.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val edadInt = edadString.toIntOrNull()
            if (edadInt == null || edadInt <= 0 || edadInt > 120) {
                Toast.makeText(this, "Por favor introduce una edad válida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXTRA_NOMBRE", nombre)
                putExtra("EXTRA_EDAD", edadInt)
                putExtra("EXTRA_PACIENTE_ID", idPaciente)
                putExtra("EXTRA_REGISTRADOR_ID", registradorId)
                putExtra("EXTRA_ROL", registradorRol)
                putExtra("EXTRA_MEDICO_ASIGNADO", if (registradorRol == "MEDICO") registradorId else null)
            }
            startActivity(intent)
            finish()
        }
    }
}