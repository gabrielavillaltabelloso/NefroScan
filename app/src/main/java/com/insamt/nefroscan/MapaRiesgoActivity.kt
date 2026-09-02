package com.insamt.nefroscan

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.insamt.nefroscan.models.PacienteMapa
import org.json.JSONArray
import org.json.JSONObject

class MapaRiesgoActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResumenZonasCriticas: TextView
    private val db = FirebaseFirestore.getInstance()

    // Referencia central: Unidad de Salud Comunitaria
    private val latUnidadSalud = 13.6265
    private val lonUnidadSalud = -89.0430

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_riesgo)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarMapaRiesgo)
        toolbar.setNavigationOnClickListener { finish() }

        val btnVolver = findViewById<MaterialButton>(R.id.btnVolverMenu)
        btnVolver.setOnClickListener { finish() }

        tvResumenZonasCriticas = findViewById(R.id.tvResumenZonasCriticas)
        progressBar = findViewById(R.id.progressBarMapa)
        webView = findViewById(R.id.webViewMapaRiesgo)

        configurarWebView()
        cargarPacientesDesdeFirestore()
    }

    private fun configurarWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        // Puente JavaScript <-> Kotlin
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun cargarPacientesDesdeFirestore() {
        progressBar.visibility = View.VISIBLE

        db.collection("usuarios") // O "pacientes" según tu colección
            .whereEqualTo("rol", "paciente")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    progressBar.visibility = View.GONE
                    val muestra = obtenerPacientesMuestraPiloto()
                    actualizarResumen(muestra)
                    renderizarMapaHtml(muestra)
                    return@addSnapshotListener
                }

                val listaPacientes = mutableListOf<PacienteMapa>()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots) {
                        val lat = doc.getDouble("latitud") ?: 0.0
                        val lon = doc.getDouble("longitud") ?: 0.0

                        // Solo mostramos en el mapa a quienes ya tengan ubicación asignada
                        if (lat != 0.0 && lon != 0.0) {
                            @Suppress("UNCHECKED_CAST")
                            val factores = doc.get("factoresExposicion") as? List<String> ?: emptyList()

                            listaPacientes.add(
                                PacienteMapa(
                                    uid = doc.id,
                                    correo = doc.getString("correo") ?: doc.getString("email") ?: doc.id,
                                    nombre = doc.getString("nombre") ?: "Paciente registrado",
                                    direccion = doc.getString("direccion") ?: "Cantón Las Flores",
                                    latitud = lat,
                                    longitud = lon,
                                    nivelRiesgo = (doc.getString("nivelRiesgo") ?: "BAJO").uppercase(),
                                    factoresExposicion = factores,
                                    telefono = doc.getString("telefono") ?: ""
                                )
                            )
                        }
                    }
                }

                val datosFinales = if (listaPacientes.isEmpty()) obtenerPacientesMuestraPiloto() else listaPacientes

                actualizarResumen(datosFinales)
                renderizarMapaHtml(datosFinales)
            }
    }

    private fun actualizarResumen(pacientes: List<PacienteMapa>) {
        val rojos = pacientes.count { it.nivelRiesgo == "ALTO" || it.nivelRiesgo == "ROJO" }
        val amarillos = pacientes.count { it.nivelRiesgo == "MEDIO" || it.nivelRiesgo == "AMARILLO" }
        val verdes = pacientes.count { it.nivelRiesgo == "BAJO" || it.nivelRiesgo == "VERDE" }

        tvResumenZonasCriticas.text = """
            • Alerta Roja (Alto Riesgo): $rojos viviendas
            • Riesgo Moderado (Amarillo): $amarillos viviendas
            • Bajo Riesgo (Verde): $verdes viviendas
            Total de viviendas asignadas por promotor: ${pacientes.size}
        """.trimIndent()
    }

    private fun renderizarMapaHtml(pacientes: List<PacienteMapa>) {
        val jsonArray = JSONArray()
        for (p in pacientes) {
            val obj = JSONObject().apply {
                put("nombre", p.nombre)
                put("correo", p.correo)
                put("direccion", p.direccion)
                put("lat", p.latitud)
                put("lng", p.longitud)
                put("riesgo", p.nivelRiesgo)
                put("factores", p.factoresExposicion.joinToString(", "))
                put("telefono", p.telefono)
            }
            jsonArray.put(obj)
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { width: 100vw; height: 100vh; }
                    .popup-card { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 13px; line-height: 1.4; color: #1e293b; }
                    .popup-header { font-weight: bold; font-size: 14px; margin-bottom: 2px; }
                    .popup-sub { font-size: 11px; color: #64748b; margin-bottom: 6px; word-break: break-all; }
                    .badge { display: inline-block; padding: 2px 6px; border-radius: 4px; color: #ffffff; font-size: 10px; font-weight: bold; margin-bottom: 6px; text-transform: uppercase; }
                    .badge-alto { background-color: #EF4444; }
                    .badge-medio { background-color: #F59E0B; }
                    .badge-bajo { background-color: #10B981; }
                    .btn-gps { display: block; width: 100%; margin-top: 8px; padding: 8px 0; background: #0077B6; color: #FFFFFF; text-align: center; font-weight: bold; border-radius: 6px; border: none; cursor: pointer; font-size: 12px; }
                    .instruccion { position: absolute; bottom: 15px; left: 50%; transform: translateX(-50%); background: rgba(3, 4, 94, 0.85); color: white; padding: 6px 14px; border-radius: 20px; font-size: 11px; font-family: sans-serif; z-index: 1000; pointer-events: none; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <div class="instruccion">👆 Toca en el mapa para ubicar la casa de un paciente</div>
                <script>
                    var map = L.map('map').setView([$latUnidadSalud, $lonUnidadSalud], 15);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    // Unidad de Salud
                    var clinicIcon = L.divIcon({
                        className: 'custom-clinic',
                        html: '<div style="background-color:#03045E; color:white; border-radius:50%; width:34px; height:34px; display:flex; align-items:center; justify-content:center; font-size:18px; border:2px solid white; box-shadow:0 2px 6px rgba(0,0,0,0.3);">🏥</div>',
                        iconSize: [34, 34],
                        iconAnchor: [17, 17]
                    });

                    L.marker([$latUnidadSalud, $lonUnidadSalud], { icon: clinicIcon })
                        .addTo(map)
                        .bindPopup("<b>Unidad de Salud Comunitaria</b><br>Centro de Referencia Cantón Las Flores");

                    // Marcadores de Pacientes
                    var pacientes = $jsonArray;
                    pacientes.forEach(function(p) {
                        var color = '#10B981';
                        var badgeClass = 'badge-bajo';
                        
                        if (p.riesgo === 'ALTO' || p.riesgo === 'ROJO') {
                            color = '#EF4444';
                            badgeClass = 'badge-alto';
                        } else if (p.riesgo === 'MEDIO' || p.riesgo === 'AMARILLO') {
                            color = '#F59E0B';
                            badgeClass = 'badge-medio';
                        }

                        var markerIcon = L.divIcon({
                            className: 'patient-marker',
                            html: '<div style="background-color:' + color + '; width:20px; height:20px; border-radius:50%; border:2px solid white; box-shadow:0 2px 5px rgba(0,0,0,0.4);"></div>',
                            iconSize: [20, 20],
                            iconAnchor: [10, 10]
                        });

                        var factoresText = p.factores ? '<div><b>Exposición:</b> ' + p.factores + '</div>' : '';

                        var popup = '<div class="popup-card">' +
                            '<span class="badge ' + badgeClass + '">RIESGO ' + p.riesgo + '</span>' +
                            '<div class="popup-header">' + p.nombre + '</div>' +
                            '<div class="popup-sub">✉️ ' + p.correo + '</div>' +
                            '<div><b>Vivienda:</b> ' + p.direccion + '</div>' +
                            factoresText +
                            '<button class="btn-gps" onclick="AndroidBridge.abrirRutaGps(' + p.lat + ',' + p.lng + ')">🚗 Ruta GPS (Promotor)</button>' +
                            '</div>';

                        L.marker([p.lat, p.lng], { icon: markerIcon })
                            .addTo(map)
                            .bindPopup(popup);
                    });

                    // Evento al tocar el mapa: llama a la función nativa de Android
                    map.on('click', function(e) {
                        AndroidBridge.onMapaTocado(e.latlng.lat, e.latlng.lng);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    // Muestra el cuadro de diálogo para que el promotor asigne las coordenadas
    private fun mostrarDialogoRegistrarVivienda(lat: Double, lng: Double) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_ubicacion, null)
        val etCorreo = dialogView.findViewById<EditText>(R.id.etCorreoPacienteDialog)
        val etDireccion = dialogView.findViewById<EditText>(R.id.etDireccionDialog)
        val tvCoordenadas = dialogView.findViewById<TextView>(R.id.tvCoordenadasDialog)

        tvCoordenadas.text = "GPS: %.5f, %.5f".format(lat, lng)

        MaterialAlertDialogBuilder(this)
            .setTitle("Asignar Vivienda a Paciente")
            .setView(dialogView)
            .setPositiveButton("Guardar Ubicación") { _, _ ->
                val correo = etCorreo.text.toString().trim()
                val direccion = etDireccion.text.toString().trim()

                if (correo.isEmpty()) {
                    Toast.makeText(this, "Ingresa el correo o identificador del paciente", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                guardarUbicacionEnFirestore(correo, direccion, lat, lng)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarUbicacionEnFirestore(correoOId: String, direccion: String, lat: Double, lng: Double) {
        progressBar.visibility = View.VISIBLE

        val datosUbicacion = hashMapOf(
            "latitud" to lat,
            "longitud" to lng,
            "direccion" to if (direccion.isEmpty()) "Cantón Las Flores" else direccion,
            "correo" to correoOId,
            "rol" to "paciente"
        )

        // Busca por correo o actualiza directamente si el documento tiene el correo/UID
        db.collection("usuarios")
            .whereEqualTo("correo", correoOId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val docId = querySnapshot.documents[0].id
                    db.collection("usuarios").document(docId).set(datosUbicacion, SetOptions.merge())
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "✅ Ubicación de vivienda guardada con éxito", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Si no existe un documento previo, lo registra directamente
                    db.collection("usuarios").document(correoOId).set(datosUbicacion, SetOptions.merge())
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "✅ Ubicación registrada para $correoOId", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun obtenerPacientesMuestraPiloto(): List<PacienteMapa> {
        return listOf(
            PacienteMapa("1", "carlos.mendoza@email.com", "Carlos Mendoza (Caso Piloto)", "Sector Agrícola 1", 13.6285, -89.0415, "ALTO", listOf("Agroquímicos")),
            PacienteMapa("2", "maria.dominguez@email.com", "María Domínguez (Caso Piloto)", "Ribera del Río", 13.6248, -89.0452, "ALTO", listOf("Agua de pozo"))
        )
    }

    // Métodos expuestos hacia JavaScript dentro del WebView
    inner class WebAppInterface {
        @JavascriptInterface
        fun onMapaTocado(lat: Double, lng: Double) {
            runOnUiThread {
                mostrarDialogoRegistrarVivienda(lat, lng)
            }
        }

        @JavascriptInterface
        fun abrirRutaGps(lat: Double, lng: Double) {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")))
            }
        }
    }
}