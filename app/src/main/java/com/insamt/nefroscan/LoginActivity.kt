package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
            etContrasena.setText("ale555")
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
            var uidFirebase = ""
            var emailFirebase = emailParaAuth
            var rolFinal = "PACIENTE"
            var accesoConcedido = false

            // 1. Intentar autenticar con Firebase (Requiere Internet)
            try {
                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(emailParaAuth, pass)
                    .await()

                val firebaseUser = authResult.user
                uidFirebase = firebaseUser?.uid ?: ""
                emailFirebase = firebaseUser?.email ?: emailParaAuth
                accesoConcedido = true

                // Obtener el rol real directamente desde Firestore si hay conexión
                try {
                    val firestoreDoc = FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uidFirebase)
                        .get()
                        .await()

                    if (firestoreDoc.exists()) {
                        rolFinal = firestoreDoc.getString("rol")?.uppercase(Locale.getDefault()) ?: "PACIENTE"
                    } else {
                        rolFinal = when {
                            emailFirebase.equals("binnivillalta@gmail.com", ignoreCase = true) -> "MEDICO"
                            emailFirebase.contains("promotor", ignoreCase = true) || emailFirebase.contains("prom") -> "PROMOTOR"
                            else -> "PACIENTE"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginFirestore", "No se pudo leer de Firestore, usando respaldo local", e)
                    val dbFallback = UserDatabaseFactory.getDatabaseForUser(applicationContext, emailFirebase)
                    val localUser = dbFallback.userDao().obtenerUsuarioPorId(emailFirebase)
                    rolFinal = localUser?.rol?.uppercase(Locale.getDefault()) ?: "PACIENTE"
                }

            } catch (e: Exception) {
                // 2. MODO OFFLINE: Si Firebase falla (sin internet), validamos en la base de datos local Room
                Log.w("LoginAuth", "Fallo autenticación online, intentando modo offline...", e)

                try {
                    val dbFallback = UserDatabaseFactory.getDatabaseForUser(applicationContext, emailParaAuth)
                    val usuarioLocal = dbFallback.userDao().autenticar(emailParaAuth, pass)

                    if (usuarioLocal != null) {
                        rolFinal = usuarioLocal.rol.uppercase(Locale.getDefault())
                        emailFirebase = usuarioLocal.idUsuario
                        uidFirebase = "offline_${usuarioLocal.idUsuario}"
                        accesoConcedido = true
                    }
                } catch (localEx: Exception) {
                    Log.e("LoginLocal", "Error al validar en base de datos local", localEx)
                }
            }

            // 3. Si se concedió el acceso (por Firebase o por Room offline)
            if (accesoConcedido) {
                try {
                    val dbGeneral = UserDatabaseFactory.getDatabaseForUser(applicationContext, emailFirebase)
                    val nombreGenerado = emailFirebase.substringBefore("@")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    val userEntity = UserEntity(
                        idUsuario = emailFirebase,
                        nombreCompleto = nombreGenerado,
                        contrasena = pass,
                        rol = rolFinal
                    )
                    dbGeneral.userDao().insertarUsuario(userEntity)
                } catch (ex: Exception) {
                    Log.e("RoomSave", "No se pudo actualizar la BD local", ex)
                }

                withContext(Dispatchers.Main) {
                    // 4. Guardar la sesión activa con el rol correcto
                    val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply {
                        putString("ID_USUARIO", emailFirebase)
                        putString("UID_FIREBASE", uidFirebase)
                        putString("NOMBRE_USUARIO", emailFirebase.substringBefore("@"))
                        putString("ROL_USUARIO", rolFinal)
                        apply()
                    }

                    // 5. Inicializar la base de datos privada del usuario
                    UserDatabaseFactory.getDatabaseForUser(applicationContext, emailFirebase)

                    // 6. Redirigir al Dashboard exacto según el rol real
                    val intent = when (rolFinal) {
                        "MEDICO" -> Intent(this@LoginActivity, MedicoDashboardActivity::class.java)
                        "PROMOTOR" -> Intent(this@LoginActivity, PromotorDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, PacienteDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Acceso denegado. Verifique sus credenciales o conéctese a internet por primera vez en este dispositivo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}