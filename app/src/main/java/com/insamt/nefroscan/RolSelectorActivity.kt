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

        // 1. Rol: Promotor de Salud (Abre el Dashboard de Trabajo de Campo)
        btnPromotor.setOnClickListener {
            val intent = Intent(this, PromotorDashboardActivity::class.java)
            startActivity(intent)
        }

        // 2. Rol: Médico (Abre la Estación de Diagnóstico Médico e IA)
        btnMedico.setOnClickListener {
            val intent = Intent(this, MedicoDashboardActivity::class.java)
            startActivity(intent)
        }

        // 3. Rol: Paciente (Abre el Portal de Autogestión del Paciente)
        btnPaciente.setOnClickListener {
            val intent = Intent(this, PacienteDashboardActivity::class.java)
            startActivity(intent)
        }
    }
}