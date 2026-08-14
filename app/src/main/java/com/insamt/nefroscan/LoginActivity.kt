package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText
    private val database: NefroScanDatabase by lazy { NefroScanDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnQuickMedico = findViewById<Button>(R.id.btnQuickMedico)
        val btnQuickPromotor = findViewById<Button>(R.id.btnQuickPromotor)
        val btnQuickPaciente = findViewById<Button>(R.id.btnQuickPaciente)
        val btnSelectorRoles = findViewById<Button>(R.id.btnSelectorRoles)

        // Inicializar usuarios por defecto para pruebas/demo
        prepararUsuariosPorDefecto()

        btnIngresar.setOnClickListener {
            val user = etUsuario.text.toString().trim()
            val pass = etContrasena.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            autenticarUsuario(user, pass)
        }

        // Acceso rápido instantáneo para pruebas
        btnQuickMedico.setOnClickListener {
            etUsuario.setText("medico@nefroscan.sv")
            etContrasena.setText("123456")
            autenticarUsuario("medico@nefroscan.sv", "123456")
        }

        btnQuickPromotor.setOnClickListener {
            etUsuario.setText("promotor@nefroscan.sv")
            etContrasena.setText("123456")
            autenticarUsuario("promotor@nefroscan.sv", "123456")
        }

        btnQuickPaciente.setOnClickListener {
            etUsuario.setText("paciente@nefroscan.sv")
            etContrasena.setText("123456")
            autenticarUsuario("paciente@nefroscan.sv", "123456")
        }

        // Abrir el Selector de Roles directamente
        btnSelectorRoles.setOnClickListener {
            startActivity(Intent(this, RolSelectorActivity::class.java))
        }
    }

    private fun prepararUsuariosPorDefecto() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (database.userDao().contarUsuarios() == 0) {
                    database.userDao().insertarUsuario(
                        UserEntity("medico@nefroscan.sv", "Dr. Martínez", "123456", "MEDICO", "Especialidad Nefrología")
                    )
                    database.userDao().insertarUsuario(
                        UserEntity("promotor@nefroscan.sv", "Promotor San Miguel", "123456", "PROMOTOR", "San Miguel Tepezontes")
                    )
                    database.userDao().insertarUsuario(
                        UserEntity("paciente@nefroscan.sv", "Carlos Pérez", "123456", "PACIENTE", "Caserío El Espino")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun autenticarUsuario(usuario: String, pass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userEntity = database.userDao().autenticar(usuario, pass)
                withContext(Dispatchers.Main) {
                    if (userEntity != null) {
                        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("ID_USUARIO", userEntity.idUsuario)
                            putString("NOMBRE_USUARIO", userEntity.nombreCompleto)
                            putString("ROL_USUARIO", userEntity.rol)
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${userEntity.nombreCompleto}!", Toast.LENGTH_SHORT).show()

                        val intent = when (userEntity.rol) {
                            "MEDICO" -> Intent(this@LoginActivity, MedicoDashboardActivity::class.java)
                            "PROMOTOR" -> Intent(this@LoginActivity, PromotorDashboardActivity::class.java)
                            else -> Intent(this@LoginActivity, PacienteDashboardActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error en el sistema de autenticación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}