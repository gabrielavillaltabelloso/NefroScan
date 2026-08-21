package com.insamt.nefroscan

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
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

    // 🔑 Pega aquí tu API Key de Google AI Studio (si la dejas en blanco, usará el motor local inteligente)
    private val API_KEY = "TU_API_KEY_AQUI"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = API_KEY
        )
    }

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

        agregarMensajeChat("Hola, soy tu asistente de salud renal NefroScan. Selecciona un tema para orientarte o escribe tu consulta:", esUsuario = false)

        mostrarCategoriasPrincipales()

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                procesarMensajeEscrito(texto)
                etMensaje.setText("")
            } else {
                Toast.makeText(this, "Escribe una pregunta para consultar.", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun mostrarCategoriasPrincipales() {
        chipGroup.removeAllViews()

        crearChip("🩺 Sobre la Enfermedad") {
            enviarInteraccionBoton("Quiero saber sobre la Enfermedad Renal")
            mostrarMenuEnfermedad()
        }
        crearChip("⚠️ Factores de Riesgo") {
            enviarInteraccionBoton("Quiero conocer los Factores de Riesgo")
            mostrarMenuRiesgo()
        }
        crearChip("🧪 Pruebas y Exámenes") {
            enviarInteraccionBoton("Información sobre Pruebas y Análisis")
            mostrarMenuPruebas()
        }
        crearChip("🥗 Prevención y Dieta") {
            enviarInteraccionBoton("Consejos de Prevención y Estilo de Vida")
            mostrarMenuPrevencion()
        }
    }

    private fun mostrarMenuEnfermedad() {
        chipGroup.removeAllViews()
        crearChip("¿Qué hacen los riñones?") { enviarInteraccionBoton("¿Qué hacen los riñones?") }
        crearChip("¿Qué es la ERC?") { enviarInteraccionBoton("¿Qué es la enfermedad renal crónica?") }
        crearChip("¿Cuáles son los síntomas?") { enviarInteraccionBoton("¿Cuáles son los síntomas de la enfermedad?") }
        crearChip("¿Tiene cura?") { enviarInteraccionBoton("¿La enfermedad tiene cura?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuRiesgo() {
        chipGroup.removeAllViews()
        crearChip("¿La Diabetes afecta?") { enviarInteraccionBoton("¿La diabetes puede afectar mis riñones?") }
        crearChip("¿Y la Presión Alta?") { enviarInteraccionBoton("¿La presión alta puede dañar los riñones?") }
        crearChip("¿Medicamentos peligrosos?") { enviarInteraccionBoton("¿Tomar muchos medicamentos daña los riñones?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuPruebas() {
        chipGroup.removeAllViews()
        crearChip("¿Qué es el eGFR?") { enviarInteraccionBoton("¿Qué es el eGFR?") }
        crearChip("¿Qué es Creatinina?") { enviarInteraccionBoton("¿Qué significa la creatinina alta?") }
        crearChip("Examen de Orina") { enviarInteraccionBoton("¿Para qué sirve el examen de orina?") }
        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    private fun mostrarMenuPrevencion() {
        chipGroup.removeAllViews()
        crearChip("¿Cuánta agua tomar?") { enviarInteraccionBoton("¿Cuánta agua debo tomar al día?") }
        crearChip("¿Cómo reducir la sal?") { enviarInteraccionBoton("¿Cómo puedo reducir el consumo de sal?") }
        crearChip("¿Alimentos a evitar?") { enviarInteraccionBoton("¿Qué alimentos debo limitar?") }
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

    private fun enviarInteraccionBoton(pregunta: String) {
        agregarMensajeChat(pregunta, esUsuario = true)
        val respuesta = obtenerRespuestaEspecifica(pregunta)
        agregarMensajeChat(respuesta, esUsuario = false)
    }

    private fun procesarMensajeEscrito(pregunta: String) {
        agregarMensajeChat(pregunta, esUsuario = true)

        lifecycleScope.launch(Dispatchers.IO) {
            var respuestaFinal: String? = null

            // Intento con IA si se configuró una API Key real
            if (API_KEY.isNotEmpty() && !API_KEY.contains("TU_API_KEY")) {
                try {
                    val prompt = "Eres el asistente médico virtual NefroScan. Responde de forma clara, breve (máximo 3 oraciones), profesional y empática en español a la siguiente consulta sobre salud renal: $pregunta"
                    val response = generativeModel.generateContent(prompt)
                    val textoIA = response.text?.trim()
                    if (!textoIA.isNullOrEmpty()) {
                        respuestaFinal = textoIA
                    }
                } catch (e: Exception) {
                    Log.e("ChatbotActivity", "Error al consultar Gemini API: ${e.localizedMessage}")
                }
            }

            // Si no hay API Key o falló la conexión, usa el motor local ampliado
            if (respuestaFinal == null) {
                respuestaFinal = obtenerRespuestaEspecifica(pregunta)
            }

            withContext(Dispatchers.Main) {
                agregarMensajeChat(respuestaFinal, esUsuario = false)
            }
        }
    }

    private fun agregarMensajeChat(mensaje: String, esUsuario: Boolean) {
        listaMensajes.add(ChatMessage(mensaje, esUsuario))
        adapter.notifyItemInserted(listaMensajes.size - 1)
        rvChat.smoothScrollToPosition(listaMensajes.size - 1)
    }

    // 🧠 Motor Local Inteligente y Ampliado
    private fun obtenerRespuestaEspecifica(pregunta: String): String {
        val p = normalizarTexto(pregunta)

        return when {
            // Un solo riñón / Pérdida / Donación
            p.contains("pierdo") || p.contains("un solo rinon") || p.contains("un rinon") || p.contains("donar") || p.contains("donacion") ->
                "Es completamente posible tener una vida normal y saludable con un solo riñón. El riñón restante aumenta su tamaño y capacidad de filtrado. Solo debes cuidarlo manteniendo una dieta baja en sal, buena hidratación y evitando la automedicación."

            // Diálisis / Trasplante
            p.contains("dialisis") || p.contains("hemodialisis") || p.contains("trasplante") ->
                "La diálisis y el trasplante son tratamientos para la etapa más avanzada de la enfermedad renal. La diálisis sustituye artificialmente la función del riñón filtrando toxinas de la sangre."

            // Insuficiencia / Gravedad
            p.contains("insuficiencia") || p.contains("rebal") || p.contains("tan mala") || p.contains("peligro") || p.contains("grave") || p.contains("muerte") ->
                "La insuficiencia renal es grave porque los riñones dejan de limpiar la sangre, acumulando toxinas y exceso de líquidos en el cuerpo, lo cual sobrecarga el corazón y altera la presión arterial si no se trata a tiempo."

            // Dolor / Espalda / Cintura
            p.contains("dolor") || p.contains("espalda") || p.contains("cintura") || p.contains("lumbar") ->
                "El dolor renal suele sentirse en la parte media o alta de la espalda, a los lados de la columna. Si se acompaña de fiebre, ardor al orinar o sangre, debes consultar al médico para descartar infecciones o cálculos."

            // Cálculos / Piedras
            p.contains("calculo") || p.contains("piedra") || p.contains("arenilla") ->
                "Los cálculos renales son depósitos duros de minerales y sales. Se previenen bebiendo abundante agua al día y reduciendo el consumo excesivo de sal y proteínas animales."

            // Infección urinaria / Ardor
            p.contains("infeccion") || p.contains("ardor") || p.contains("mal de orin") || p.contains("cistitis") ->
                "Las infecciones urinarias recurrentes pueden ascender y dañar los riñones (pielonefritis). Consulta a un médico para recibir el antibiótico adecuado y no te automediques."

            // Alcohol / Cerveza / Fumar
            p.contains("alcohol") || p.contains("cerveza") || p.contains("fumar") || p.contains("cigarro") || p.contains("tabaco") ->
                "El alcohol en exceso deshidrata el cuerpo y eleva la presión arterial. El tabaco deteriora los vasos sanguíneos renales y acelera el daño en los riñones."

            // Categorías Principales
            p.contains("saber sobre la enfermedad") ->
                "La salud renal comprende el cuidado de los riñones y el diagnóstico oportuno de la Enfermedad Renal Crónica (ERC). Selecciona una pregunta para más detalles."

            p.contains("factores de riesgo") ->
                "Los principales factores de riesgo son la diabetes, hipertensión, consumo excesivo de sal, automedicación y edad avanzada. Elige una opción para ver recomendaciones."

            p.contains("pruebas y analisis") || p.contains("pruebas y examenes") ->
                "Las pruebas principales incluyen la Creatinina en sangre, el cálculo de eGFR y el Examen General de Orina. ¿Sobre cuál deseas información?"

            p.contains("prevencion y estilo") || p.contains("prevencion y dieta") ->
                "La prevención se basa en hidratación adecuada, reducción de sodio, control de glucosa y actividad física diaria. Consulta las opciones disponibles."

            // Conceptos Específicos
            p.contains("hacen los rinones") || p.contains("funcion") ->
                "Los riñones filtran toxinas y exceso de agua de la sangre para convertirlos en orina. También regulan la presión arterial y producen hormonas esenciales para los huesos y la sangre."

            p.contains("enfermedad renal cronica") || p.contains("erc") ->
                "La Enfermedad Renal Crónica es la pérdida gradual y progresiva de la función de los riñones por más de 3 meses. Si no se trata a tiempo, los desechos se acumulan en el cuerpo."

            p.contains("sintomas de la enfermedad") || p.contains("sintoma") || p.contains("hinchazon") || p.contains("edema") ->
                "Presta atención a hinchazón en tobillos o cara (edema), fatiga persistente, espuma en la orina o cambios al orinar. Si los notas, consulta a tu médico."

            p.contains("tiene cura") || p.contains("cura") ->
                "El daño renal crónico no suele ser reversible, pero un tratamiento médico adecuado, control de presión y buena alimentación pueden detener o retrasar su avance."

            p.contains("diabetes") || p.contains("azucar") ->
                "Los niveles altos de glucosa dañan los filtros sanguíneos de los riñones (nefronas). Mantener la glucosa en rango protege la función renal."

            p.contains("presion alta") || p.contains("hipertension") ->
                "La presión alta debilita y endurece las arterias renales, impidiendo que filtren los desechos de forma adecuada."

            p.contains("medicamento") || p.contains("analgesico") || p.contains("pastilla") || p.contains("ibuprofeno") ->
                "El abuso sin receta de analgésicos comunes (como ibuprofeno o ketorolaco) puede generar toxicidad y daño renal severo."

            p.contains("egfr") || p.contains("filtrado") ->
                "El eGFR mide la capacidad de filtración de tus riñones. Un valor superior a 60 suele ser favorable; menor a 60 por más de 3 meses indica ERC."

            p.contains("creatinina") ->
                "La creatinina es un residuo del trabajo muscular. Si los riñones no filtran bien, sus niveles en sangre se elevan."

            p.contains("examen de orina") || p.contains("orina") ->
                "El análisis de orina detecta si se están perdiendo proteínas (proteinuria) o sangre, señales tempranas de daño en el tejido renal."

            p.contains("cuanta agua") || p.contains("agua") || p.contains("liquido") || p.contains("tomar") ->
                "Se aconseja consumir entre 1.5 y 2 litros diarios de agua natural, salvo que tu médico te haya ordenado restricción por retención de líquidos."

            p.contains("reducir el consumo de sal") || p.contains("sal") || p.contains("sodio") ->
                "Reducir el consumo de sal ayuda a controlar la presión arterial. Evita embutidos y sazona con hierbas como orégano, ajo o limón."

            p.contains("alimentos debo limitar") || p.contains("limitar") || p.contains("comida") || p.contains("alimento") ->
                "Limita productos ultraprocesados, bebidas gaseosas/energéticas y carnes muy procesadas. Consulta a tu médico sobre el potasio si tienes daño renal."

            else ->
                "Como recomendación preventiva de NefroScan: mantén una hidratación adecuada con agua natural, controla tus niveles de presión y glucosa, y asiste a tus revisiones médicas periódicas."
        }
    }

    // Remueve tildes y signos para facilitar la coincidencia
    private fun normalizarTexto(texto: String): String {
        return texto.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace("¿", "").replace("?", "").replace("¡", "").replace("!", "")
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