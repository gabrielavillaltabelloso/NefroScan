package com.insamt.nefroscan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("SpellCheckingInspection")
class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button
    private lateinit var chipGroup: ChipGroup
    private val listaMensajes = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        rvChat = findViewById(R.id.rvChatMessages)
        etMensaje = findViewById(R.id.etChatMessage)
        btnEnviar = findViewById(R.id.btnSendChat)
        chipGroup = findViewById(R.id.chipGroupPreguntas)
        val btnVolver = findViewById<Button>(R.id.btnVolverHistorial)

        adapter = ChatAdapter(listaMensajes)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        agregarMensajeChat("Hola, soy tu asistente de salud renal NefroScan. Selecciona un tema para orientarte:", esUsuario = false)

        mostrarCategoriasPrincipales()

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                procesarPreguntaUsuario(texto)
            } else {
                Toast.makeText(this, "Escribe una pregunta para consultar.", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun mostrarCategoriasPrincipales() {
        chipGroup.removeAllViews()

        crearChip("🩺 Sobre la Enfermedad") {
            procesarPreguntaUsuario("Quiero saber sobre la Enfermedad Renal")
            mostrarMenuEnfermedad()
        }
        crearChip("⚠️ Factores de Riesgo") {
            procesarPreguntaUsuario("Quiero conocer los Factores de Riesgo")
            mostrarMenuRiesgo()
        }
        crearChip("🧪 Pruebas y Exámenes") {
            procesarPreguntaUsuario("Información sobre Pruebas y Análisis")
            mostrarMenuPruebas()
        }
        crearChip("🥗 Prevención y Dieta") {
            procesarPreguntaUsuario("Consejos de Prevención y Estilo de Vida")
            mostrarMenuPrevencion()
        }
    }

    private fun mostrarMenuEnfermedad() {
        chipGroup.removeAllViews()
        crearChip("¿Qué hacen los riñones?") { procesarPreguntaUsuario("¿Qué hacen los riñones?") }
        crearChip("¿Qué es la ERC?") { procesarPreguntaUsuario("¿Qué es la enfermedad renal crónica?") }
        crearChip("¿Cuáles son los síntomas?") { procesarPreguntaUsuario("¿Cuáles son los síntomas de la enfermedad?") }
        crearChip("¿Tiene cura?") { procesarPreguntaUsuario("¿La enfermedad tiene cura?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuRiesgo() {
        chipGroup.removeAllViews()
        crearChip("¿La Diabetes afecta?") { procesarPreguntaUsuario("¿La diabetes puede afectar mis riñones?") }
        crearChip("¿Y la Presión Alta?") { procesarPreguntaUsuario("¿La presión alta puede dañar los riñones?") }
        crearChip("¿Medicamentos peligrosos?") { procesarPreguntaUsuario("¿Tomar muchos medicamentos daña los riñones?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuPruebas() {
        chipGroup.removeAllViews()
        crearChip("¿Qué es el eGFR?") { procesarPreguntaUsuario("¿Qué es el eGFR?") }
        crearChip("¿Qué es Creatinina?") { procesarPreguntaUsuario("¿Qué significa la creatinina alta?") }
        crearChip("Examen de Orina") { procesarPreguntaUsuario("¿Para qué sirve el examen de orina?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuPrevencion() {
        chipGroup.removeAllViews()
        crearChip("¿Cuánta agua tomar?") { procesarPreguntaUsuario("¿Cuánta agua debo tomar al día?") }
        crearChip("¿Cómo reducir la sal?") { procesarPreguntaUsuario("¿Cómo puedo reducir el consumo de sal?") }
        crearChip("¿Alimentos a evitar?") { procesarPreguntaUsuario("¿Qué alimentos debo limitar?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun crearChip(texto: String, onClick: () -> Unit) {
        val chip = Chip(this).apply {
            this.text = texto
            setChipBackgroundColorResource(android.R.color.transparent)
            setTextColor(getColor(android.R.color.white))
            chipStrokeWidth = 3f
            setOnClickListener { onClick() }
        }
        chipGroup.addView(chip)
    }

    private fun procesarPreguntaUsuario(pregunta: String) {
        agregarMensajeChat(pregunta, esUsuario = true)
        etMensaje.setText("")

        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            val respuestaIA = generarRespuestaPreventiva(pregunta)
            withContext(Dispatchers.Main) {
                agregarMensajeChat(respuestaIA, esUsuario = false)
            }
        }
    }

    private fun agregarMensajeChat(mensaje: String, esUsuario: Boolean) {
        listaMensajes.add(ChatMessage(mensaje, esUsuario))
        adapter.notifyItemInserted(listaMensajes.size - 1)
        rvChat.smoothScrollToPosition(listaMensajes.size - 1)
    }

    private fun generarRespuestaPreventiva(pregunta: String): String {
        val p = pregunta.lowercase()
        return when {
            // Evaluaciones específicas primero
            p.contains("hacen los riñones") || p.contains("función de los riñones") ->
                "Los riñones filtran toxinas y exceso de agua de la sangre para convertirlos en orina. También regulan la presión arterial y producen hormonas esenciales para los huesos y la sangre."

            p.contains("síntoma") || p.contains("hinchazón") || p.contains("dolor") ->
                "Presta atención a hinchazón en tobillos o cara (edema), fatiga persistente, espuma en la orina o cambios al orinar. Si los notas, consulta a tu médico."

            p.contains("cura") || p.contains("reversible") ->
                "El daño renal crónico no suele ser reversible, pero un tratamiento médico adecuado, control de presión y buena alimentación pueden detener o retrasar su avance."

            p.contains("diabetes") || p.contains("azúcar") ->
                "Los niveles altos de glucosa dañan los filtros sanguíneos de los riñones (nefronas). Mantener la glucosa en rango protege la función renal."

            p.contains("presión") || p.contains("hipertensión") ->
                "La presión alta debilita y endurece las arterias renales, impidiendo que filtren los desechos de forma adecuada."

            p.contains("medicamento") || p.contains("analgésico") || p.contains("ibuprofeno") ->
                "El abuso sin receta de analgésicos comunes (como ibuprofeno o ketorolaco) puede generar toxicidad y daño renal severo."

            p.contains("egfr") || p.contains("filtrado") ->
                "El eGFR mide la capacidad de filtración de tus riñones. Un valor superior a 60 suele ser favorable; menor a 60 por más de 3 meses indica ERC."

            p.contains("creatinina") ->
                "La creatinina es un residuo del trabajo muscular. Si los riñones no filtran bien, sus niveles en sangre se elevan."

            p.contains("orina") ->
                "El análisis de orina detecta si se están perdiendo proteínas (proteinuria) o sangre, señales tempranas de daño en el tejido renal."

            p.contains("agua") || p.contains("líquido") || p.contains("hidratación") ->
                "Se aconseja consumir entre 1.5 y 2 litros diarios de agua natural, salvo que tu médico te haya ordenado restricción por retención de líquidos."

            p.contains("sal") || p.contains("sodio") ->
                "Reducir el consumo de sal ayuda a controlar la presión arterial. Evita embutidos y sazona con hierbas como orégano, ajo o limón."

            p.contains("alimento") || p.contains("limitar") || p.contains("comida") ->
                "Limita productos ultraprocesados, bebidas gaseosas/energéticas y carnes muy procesadas. Consulta a tu médico sobre el potasio si tienes daño renal."

            p.contains("erc") || p.contains("enfermedad renal") || p.contains("crónica") ->
                "La Enfermedad Renal Crónica es la pérdida gradual y progresiva de la función de los riñones por más de 3 meses. Si no se trata a tiempo, los desechos se acumulan en el cuerpo."

            else ->
                "Para proteger tus riñones: mantén una hidratación balanceada, modera la sal, controla tu presión y asiste a tus evaluaciones médicas periódicas."
        }
    }

    data class ChatMessage(val texto: String, val esUsuario: Boolean)

    class ChatAdapter(private val lista: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvMensaje: TextView = view.findViewById(R.id.tvMessageText)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val layout = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_bot
            val view = android.view.LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvMensaje.text = lista[position].texto
        }

        override fun getItemViewType(position: Int): Int = if (lista[position].esUsuario) 1 else 0

        override fun getItemCount(): Int = lista.size
    }
}