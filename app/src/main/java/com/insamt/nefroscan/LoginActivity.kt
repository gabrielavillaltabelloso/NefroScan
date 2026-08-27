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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: TextInputEditText
    private lateinit var etContrasena: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Enlazar vistas de manera segura según el XML proporcionado
        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnCrearCuenta = findViewById<Button>(R.id.btnSelectorRoles)

        val btnDemoMedico = findViewById<Button>(R.id.btnDemoMedico)
        val btnDemoPromotor = findViewById<Button>(R.id.btnDemoPromotor)
        val btnDemoPaciente = findViewById<Button>(R.id.btnDemoPaciente)

        btnCrearCuenta.text = "Crear Cuenta Nueva"
        btnCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnDemoMedico.setOnClickListener {
            etUsuario.setText("binnivillalta@gmail.com")
            etContrasena.setText("200826")
            Toast.makeText(this, "Demo Medico cargado", Toast.LENGTH_SHORT).show()
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

                val firebaseUser = authResult.user
                val uidFirebase = firebaseUser?.uid ?: ""
                val emailFirebase = firebaseUser?.email ?: emailParaAuth

                // Definir el rol correcto de manera estricta según el correo
                val rolCorrecto = when {
                    emailFirebase.equals("binnivillalta@gmail.com", ignoreCase = true) -> "MEDICO"
                    emailFirebase.contains("promotor", ignoreCase = true) -> "PROMOTOR"
                    else -> "PACIENTE"
                }

                // 2. Obtener los datos del usuario desde Room local
                val dbGeneral = UserDatabaseFactory.getDatabaseForUser(applicationContext, emailFirebase)
                var userEntity = dbGeneral.userDao().obtenerUsuarioPorId(emailFirebase)
                    ?: dbGeneral.userDao().obtenerUsuarioPorId(usuario)

                // 3. Si no existe localmente, lo creamos con el rol correcto
                if (userEntity == null) {
                    val nombreGenerado = emailFirebase.substringBefore("@")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    userEntity = UserEntity(
                        idUsuario = emailFirebase,
                        nombreCompleto = nombreGenerado,
                        contrasena = pass,
                        rol = rolCorrecto
                    )
                    dbGeneral.userDao().insertarUsuario(userEntity)
                } else {
                    // Si ya existía pero tenía un rol incorrecto, lo actualizamos
                    if (userEntity.rol != rolCorrecto) {
                        userEntity = userEntity.copy(rol = rolCorrecto, contrasena = pass)
                        dbGeneral.userDao().insertarUsuario(userEntity)
                    }
                }

                val finalUserEntity = userEntity

                withContext(Dispatchers.Main) {
                    // 4. Guardar la sesión activa limpiando preferencias anteriores
                    val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply {
                        putString("ID_USUARIO", finalUserEntity.idUsuario)
                        putString("UID_FIREBASE", uidFirebase)
                        putString("NOMBRE_USUARIO", finalUserEntity.nombreCompleto)
                        putString("ROL_USUARIO", finalUserEntity.rol)
                        apply()
                    }

                    // 5. Inicializar la base de datos privada del usuario
                    UserDatabaseFactory.getDatabaseForUser(applicationContext, finalUserEntity.idUsuario)

                    // 6. Redirigir al Dashboard exacto según su rol
                    val intent = when (finalUserEntity.rol) {
                        "MEDICO" -> Intent(this@LoginActivity, MedicoDashboardActivity::class.java)
                        "PROMOTOR" -> Intent(this@LoginActivity, PromotorDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, PacienteDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error de acceso: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}