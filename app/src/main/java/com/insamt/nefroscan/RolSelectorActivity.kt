package com.insamt.nefroscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RolSelectorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rol_selector)

        val btnPromotor = findViewById<Button>(R.id.btnRolPromotor)
        val btnMedico = findViewById<Button>(R.id.btnRolMedico)
        val btnPaciente = findViewById<Button>(R.id.btnRolPaciente)

        // Si es Promotor de Salud (Abre la ficha de tamizaje comunitario)
        btnPromotor.setOnClickListener {
            val intent = Intent(this, PromotorActivity::class.java)
            startActivity(intent)
        }

        // Si es Médico (Abre el registro previo al escaneo e IA)
        btnMedico.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java).apply {
                putExtra("EXTRA_ROL", "MEDICO")
            }
            startActivity(intent)
        }

        // Si es Paciente (Abre el historial médico local)
        btnPaciente.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }
    }
}