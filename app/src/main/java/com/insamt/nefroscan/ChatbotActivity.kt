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

        // Mensaje inicial de bienvenida
        agregarMensajeChat("Hola, soy tu asistente de salud renal NefroScan. Selecciona un tema para orientarte:", esUsuario = false)

        // Carga el menú principal de sugerencias
        mostrarCategoriasPrincipales()

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                procesarPreguntaUsuario(texto)
            } else {
                Toast.makeText(this, "Escribe una pregunta para consultar.", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    // 🚀 NIVEL 1: Categorías Principales
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

    // 🚀 NIVEL 2: Submenú "Sobre la Enfermedad"
    private fun mostrarMenuEnfermedad() {
        chipGroup.removeAllViews()
        crearChip("¿Qué hacen los riñones?") {
            procesarPreguntaUsuario("¿Qué hacen los riñones?")
        }
        crearChip("¿Qué es la ERC?") {
            procesarPreguntaUsuario("¿Qué es la enfermedad renal crónica?")
        }
        crearChip("¿Cuáles son los síntomas?") {
            procesarPreguntaUsuario("¿Cuáles son los síntomas de una enfermedad renal?")
        }
        crearChip("¿Tiene cura?") {
            procesarPreguntaUsuario("¿La enfermedad renal tiene cura?")
        }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 NIVEL 2: Submenú "Factores de Riesgo"
    private fun mostrarMenuRiesgo() {
        chipGroup.removeAllViews()
        crearChip("¿La Diabetes afecta mis riñones?") {
            procesarPreguntaUsuario("¿La diabetes puede afectar mis riñones?")
        }
        crearChip("¿Y la Presión Alta?") {
            procesarPreguntaUsuario("¿La presión alta puede dañar los riñones?")
        }
        crearChip("¿Medicamentos peligrosos?") {
            procesarPreguntaUsuario("¿Tomar muchos medicamentos puede dañar los riñones?")
        }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 NIVEL 2: Submenú "Pruebas y Exámenes"
    private fun mostrarMenuPruebas() {
        chipGroup.removeAllViews()
        crearChip("¿Qué es el eGFR?") {
            procesarPreguntaUsuario("¿Qué es el eGFR?")
        }
        crearChip("¿Qué es la Creatinina?") {
            procesarPreguntaUsuario("¿Qué significa la creatinina alta?")
        }
        crearChip("Examen de Orina") {
            procesarPreguntaUsuario("¿Para qué sirve un examen de orina?")
        }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 NIVEL 2: Submenú "Prevención y Dieta"
    private fun mostrarMenuPrevencion() {
        chipGroup.removeAllViews()
        crearChip("¿Cuánta agua tomar?") {
            procesarPreguntaUsuario("¿Cuánta agua debo tomar al día?")
        }
        crearChip("¿Cómo reducir la sal?") {
            procesarPreguntaUsuario("¿Cómo puedo reducir el consumo de sal?")
        }
        crearChip("¿Qué alimentos limitar?") {
            procesarPreguntaUsuario("¿Qué alimentos debo limitar?")
        }
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
            delay(500) // Simulación de procesamiento

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

    // 🚀 Motor Extendido de Respuestas
    private fun generarRespuestaPreventiva(pregunta: String): String {
        val p = pregunta.lowercase()
        return when {
            p.contains("hacen los riñones") ->
                "Los riñones filtran toxinas y exceso de agua de la sangre para convertirlos en orina. También regulan la presión arterial y producen hormonas para la sangre y huesos."

            p.contains("enfermedad renal") || p.contains("erc") ->
                "La Enfermedad Renal Crónica es la pérdida gradual de la función renal por más de 3 meses. Si no se controla, las toxinas se acumulan en el cuerpo."

            p.contains("cura") ->
                "El daño renal crónico no suele ser reversible, pero un tratamiento médico oportuno y cambios de hábitos pueden frenar su avance significativamente."

            p.contains("diabetes") ->
                "El nivel alto de azúcar en sangre daña los diminutos vasos sanguíneos del riñón con el tiempo. Controlar la glucosa es esencial."

            p.contains("presión") || p.contains("hipertensión") ->
                "La presión alta ejerce demasiada fuerza sobre las arterias renales, debilitándolas e impidiendo que filtren la sangre adecuadamente."

            p.contains("medicamento") || p.contains("analgésico") ->
                "El uso frecuente y sin receta médica de analgésicos (como ibuprofeno o ketorolaco) puede causar toxicidad renal severa."

            p.contains("egfr") || p.contains("filtrado") ->
                "El eGFR mide la capacidad de filtración de tus riñones. Un valor mayor a 90 es ideal; si es menor a 60 por 3 meses, indica enfermedad renal."

            p.contains("creatinina") ->
                "La creatinina es un desecho del trabajo muscular. Si los riñones no filtran bien, se acumula en la sangre, indicando menor función renal."

            p.contains("orina") ->
                "El examen de orina permite detectar si tus riñones dejan escapar proteínas (proteinuria) o sangre, señales tempranas de daño renal."

            p.contains("agua") || p.contains("líquido") || p.contains("hidratación") ->
                "Para adultos se recomienda un consumo de 1.5 a 2 litros de agua al día, evitando bebidas azucaradas. Si tienes restricción médica, respeta la dosis de tu doctor."

            p.contains("sodio") || p.contains("sal") ->
                "Reducir el consumo de sal previene la hipertensión arterial. Evita embutidos y sazona con hierbas naturales como ajo, orégano o limón."

            p.contains("limitar") || p.contains("alimentos") ->
                "Limita ultraprocesados, gaseosas, bebidas energéticas y carnes muy procesadas. Consulta a tu nutriólogo sobre los niveles de potasio si tienes daño renal."

            p.contains("síntoma") || p.contains("dolor") || p.contains("hinchazón") ->
                "Atento a hinchazón en piernas/rostro (edema), fatiga constante y cambios en la orina. Si notas estos síntomas, acude a tu unidad de salud."

            p.contains("cálculo") || p.contains("piedra") ->
                "Los cálculos renales se forman por baja ingesta de agua y exceso de sales. Beber suficiente líquido ayuda a prevenir su formación."

            else ->
                "Recuerda mantener una hidratación adecuada, controlar tu presión arterial y realizar evaluaciones médicas periódicas para cuidar tu salud renal."
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