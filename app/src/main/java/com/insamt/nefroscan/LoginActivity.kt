package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etContrasena: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnCrearCuenta = findViewById<Button>(R.id.btnSelectorRoles)

        // Botones de Modo Demo agregados en el layout
        val btnDemoMedico = findViewById<Button>(R.id.btnDemoMedico)
        val btnDemoPromotor = findViewById<Button>(R.id.btnDemoPromotor)
        val btnDemoPaciente = findViewById<Button>(R.id.btnDemoPaciente)

        btnCrearCuenta.text = "Crear Cuenta Nueva"
        btnCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Lógica de Autocompletado para el Modo Demo
        btnDemoMedico.setOnClickListener {
            etUsuario.setText("binnivillalta@gmail.com")
            etContrasena.setText("200826")
            Toast.makeText(this, "Demo Médico cargado", Toast.LENGTH_SHORT).show()
        }

        btnDemoPromotor.setOnClickListener {
            etUsuario.setText("promotor_demo@nefroscan.sv")
            etContrasena.setText("123456")
            Toast.makeText(this, "Demo Promotor cargado", Toast.LENGTH_SHORT).show()
        }

        btnDemoPaciente.setOnClickListener {
            etUsuario.setText("paciente_demo@nefroscan.sv")
            etContrasena.setText("123456")
            Toast.makeText(this, "Demo Paciente cargado", Toast.LENGTH_SHORT).show()
        }

        btnIngresar.setOnClickListener {
            val user = etUsuario.text.toString().trim()
            val pass = etContrasena.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            autenticar(user, pass)
        }
    }

    private fun autenticar(usuario: String, pass: String) {
        val emailParaAuth = if (usuario.contains("@")) usuario else "$usuario@nefroscan.sv"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Autenticar en Firebase Authentication
                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(emailParaAuth, pass)
                    .await()

                val uidFirebase = authResult.user?.uid ?: ""

                // 2. Obtener los datos del usuario desde Room local
                val dbGeneral = NefroScanDatabase.getDatabase(applicationContext)
                val userEntity = dbGeneral.userDao().obtenerUsuarioPorId(usuario)
                    ?: dbGeneral.userDao().autenticar(usuario, pass)

                withContext(Dispatchers.Main) {
                    if (userEntity != null) {
                        // 3. Guardar la sesión activa
                        val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("ID_USUARIO", userEntity.idUsuario)
                            putString("UID_FIREBASE", uidFirebase)
                            putString("NOMBRE_USUARIO", userEntity.nombreCompleto)
                            putString("ROL_USUARIO", userEntity.rol)
                            apply()
                        }

                        // 4. Inicializar/Conectar a su base de datos privada
                        UserDatabaseFactory.getDatabaseForUser(applicationContext, userEntity.idUsuario)

                        val intent = when (userEntity.rol) {
                            "MEDICO" -> Intent(this@LoginActivity, MedicoDashboardActivity::class.java)
                            "PROMOTOR" -> Intent(this@LoginActivity, PromotorDashboardActivity::class.java)
                            else -> Intent(this@LoginActivity, PacienteDashboardActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Sesión validada, pero no se encontró el perfil local.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas o error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}