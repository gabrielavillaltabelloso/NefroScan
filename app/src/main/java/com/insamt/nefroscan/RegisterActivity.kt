package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var spRol: AutoCompleteTextView
    private lateinit var etNombre: EditText
    private lateinit var etIdUsuario: EditText
    private lateinit var etDetalle: EditText
    private lateinit var etContrasena: EditText
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        spRol = findViewById(R.id.spRolRegistro)
        etNombre = findViewById(R.id.etNombreRegistro)
        etIdUsuario = findViewById(R.id.etIdUsuarioRegistro)
        etDetalle = findViewById(R.id.etDetalleRegistro)
        etContrasena = findViewById(R.id.etContrasenaRegistro)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarRegistro)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        // Llenamos el menú desplegable con las opciones
        val roles = arrayOf("PACIENTE", "MEDICO", "PROMOTOR")
        spRol.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles))

        btnGuardar.setOnClickListener { registrarNuevoUsuario() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun registrarNuevoUsuario() {
        val rol = spRol.text.toString().trim().uppercase(Locale.getDefault())
        val nombre = etNombre.text.toString().trim()
        val idUsuario = etIdUsuario.text.toString().trim()
        val detalle = etDetalle.text.toString().trim()
        val pass = etContrasena.text.toString().trim()

        if (nombre.isEmpty() || idUsuario.isEmpty() || pass.isEmpty() || rol.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Si ingresan DUI en vez de correo, adaptamos el formato para Firebase Auth
        val emailParaAuth = if (idUsuario.contains("@")) idUsuario else "$idUsuario@nefroscan.sv"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Crear usuario en Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(emailParaAuth, pass).await()
                val uidFirebase = authResult.user?.uid ?: idUsuario

                // 2. GUARDAR EN FIRESTORE (Nube) - Permite sincronizar el rol
                val dbFirestore = FirebaseFirestore.getInstance()
                val usuarioMap = hashMapOf(
                    "idUsuario" to emailParaAuth,
                    "uid" to uidFirebase,
                    "nombreCompleto" to nombre,
                    "rol" to rol,
                    "detalleAdicional" to detalle
                )
                dbFirestore.collection("usuarios").document(uidFirebase).set(usuarioMap).await()

                // 3. Guardar en Room local general
                val dbGeneral = NefroScanDatabase.getDatabase(applicationContext)
                val nuevoUsuario = UserEntity(
                    idUsuario = emailParaAuth,
                    nombreCompleto = nombre,
                    contrasena = pass,
                    rol = rol,
                    detalleAdicional = detalle
                )
                dbGeneral.userDao().insertarUsuario(nuevoUsuario)

                // 4. Crear base de datos aislada local
                UserDatabaseFactory.getDatabaseForUser(applicationContext, emailParaAuth)

                // 5. Guardar sesión activa
                val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("ID_USUARIO", emailParaAuth)
                    putString("UID_FIREBASE", uidFirebase)
                    putString("NOMBRE_USUARIO", nombre)
                    putString("ROL_USUARIO", rol)
                    apply()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()

                    val intent = when (rol) {
                        "MEDICO" -> Intent(this@RegisterActivity, MedicoDashboardActivity::class.java)
                        "PROMOTOR" -> Intent(this@RegisterActivity, PromotorDashboardActivity::class.java)
                        else -> Intent(this@RegisterActivity, PacienteDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("RegisterAuthError", "Error al crear cuenta en Firebase Auth", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}