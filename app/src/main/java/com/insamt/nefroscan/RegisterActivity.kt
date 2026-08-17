package com.insamt.nefroscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var spRol: Spinner
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

        val roles = arrayOf("PACIENTE", "MEDICO", "PROMOTOR")
        spRol.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        btnGuardar.setOnClickListener { registrarNuevoUsuario() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun registrarNuevoUsuario() {
        val rol = spRol.selectedItem.toString()
        val nombre = etNombre.text.toString().trim()
        val idUsuario = etIdUsuario.text.toString().trim()
        val detalle = etDetalle.text.toString().trim()
        val pass = etContrasena.text.toString().trim()

        if (nombre.isEmpty() || idUsuario.isEmpty() || pass.isEmpty()) {
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
                // 1. Crear usuario en Firebase Authentication (Para que aparezca en la lista oficial)
                val authResult = auth.createUserWithEmailAndPassword(emailParaAuth, pass).await()
                val uidFirebase = authResult.user?.uid ?: idUsuario

                // 2. Guardar en Room local general
                val dbGeneral = NefroScanDatabase.getDatabase(applicationContext)
                val nuevoUsuario = UserEntity(
                    idUsuario = idUsuario,
                    nombreCompleto = nombre,
                    contrasena = pass,
                    rol = rol,
                    detalleAdicional = detalle
                )
                dbGeneral.userDao().insertarUsuario(nuevoUsuario)

                // 3. Crear base de datos aislada local
                UserDatabaseFactory.getDatabaseForUser(applicationContext, idUsuario)

                // 4. Guardar sesión
                val prefs = getSharedPreferences("SesionNefroScan", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("ID_USUARIO", idUsuario)
                    putString("UID_FIREBASE", uidFirebase)
                    putString("NOMBRE_USUARIO", nombre)
                    putString("ROL_USUARIO", rol)
                    apply()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "¡Cuenta creada en Firebase Auth!", Toast.LENGTH_SHORT).show()

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