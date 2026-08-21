package com.insamt.nefroscan

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

@Suppress("SpellCheckingInspection")
class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private val listaMensajes = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        rvChat = findViewById(R.id.rvChatMessages)
        chipGroup = findViewById(R.id.chipGroupPreguntas)
        val btnVolver = findViewById<Button>(R.id.btnVolverHistorial)

        adapter = ChatAdapter(listaMensajes)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // Mensaje inicial de bienvenida
        agregarMensajeChat("Hola, soy tu asistente de salud renal NefroScan. Selecciona un tema para orientarte:", esUsuario = false)

        // Cargar menú principal
        mostrarCategoriasPrincipales()

        btnVolver.setOnClickListener { finish() }
    }

    // 🚀 MENÚ PRINCIPAL: 4 Categorías
    private fun mostrarCategoriasPrincipales() {
        chipGroup.removeAllViews()

        crearChip("🩺 Sobre la Enfermedad") {
            responderOpcion(
                pregunta = "Quiero saber sobre la Enfermedad Renal",
                respuesta = "La salud renal comprende el cuidado de los riñones y la detección oportuna de la Enfermedad Renal Crónica (ERC). Selecciona una consulta para más detalles:"
            )
            mostrarMenuEnfermedad()
        }

        crearChip("⚠️ Factores de Riesgo") {
            responderOpcion(
                pregunta = "Quiero conocer los Factores de Riesgo",
                respuesta = "Los principales factores de riesgo son la diabetes, hipertensión, exceso de sal y la automedicación. Selecciona una opción:"
            )
            mostrarMenuRiesgo()
        }

        crearChip("🧪 Pruebas y Exámenes") {
            responderOpcion(
                pregunta = "Información sobre Pruebas y Análisis",
                respuesta = "Las pruebas fundamentales son la Creatinina en sangre, el eGFR y el Examen General de Orina. ¿Sobre cuál deseas información?"
            )
            mostrarMenuPruebas()
        }

        crearChip("🥗 Prevención y Dieta") {
            responderOpcion(
                pregunta = "Consejos de Prevención y Estilo de Vida",
                respuesta = "Cuidar los riñones requiere buena hidratación, reducir el consumo de sal y evitar el tabaco. Elige un tema de prevención:"
            )
            mostrarMenuPrevencion()
        }
    }

    // 🚀 SUBMENÚ 1: Sobre la Enfermedad
    private fun mostrarMenuEnfermedad() {
        chipGroup.removeAllViews()

        crearChip("¿Qué hacen los riñones?") {
            responderOpcion(
                pregunta = "¿Qué hacen los riñones?",
                respuesta = "Los riñones filtran toxinas y exceso de agua de la sangre para convertirlos en orina. También regulan la presión arterial, producen glóbulos rojos y equilibran los minerales del cuerpo."
            )
        }

        crearChip("¿Qué es la ERC?") {
            responderOpcion(
                pregunta = "¿Qué es la enfermedad renal crónica?",
                respuesta = "La Enfermedad Renal Crónica (ERC) es la pérdida progresiva y continua de la capacidad de filtración de los riñones por más de 3 meses. Detectarla a tiempo permite aplicar tratamientos que frenan su avance."
            )
        }

        crearChip("¿Cuáles son los síntomas?") {
            responderOpcion(
                pregunta = "¿Cuáles son los síntomas de la enfermedad?",
                respuesta = "Los síntomas de alerta son: hinchazón en pies, tobillos o cara (edema), fatiga persistente, orina espumosa o con sangre y cambios al orinar. Consulta a tu médico si los presentas."
            )
        }

        crearChip("¿Tiene cura?") {
            responderOpcion(
                pregunta = "¿La enfermedad tiene cura?",
                respuesta = "El daño renal crónico no suele ser reversible, pero un tratamiento médico adecuado, control de presión y buena alimentación detienen o retrasan su avance para evitar llegar a diálisis."
            )
        }

        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 SUBMENÚ 2: Factores de Riesgo
    private fun mostrarMenuRiesgo() {
        chipGroup.removeAllViews()

        crearChip("¿La Diabetes afecta?") {
            responderOpcion(
                pregunta = "¿La diabetes puede afectar mis riñones?",
                respuesta = "La diabetes es la causa principal de daño renal: el exceso de azúcar en sangre desgasta progresivamente los filtros del riñón (nefronas). Mantener la glucosa controlada protege los riñones."
            )
        }

        crearChip("¿Y la Presión Alta?") {
            responderOpcion(
                pregunta = "¿La presión alta puede dañar los riñones?",
                respuesta = "La presión arterial alta endurece y estrecha las arterias renales, impidiendo que limpien la sangre adecuadamente y acelerando el deterioro del tejido renal."
            )
        }

        crearChip("¿Medicamentos peligrosos?") {
            responderOpcion(
                pregunta = "¿Tomar muchos medicamentos daña los riñones?",
                respuesta = "El consumo frecuente de analgésicos e antiinflamatorios (como ibuprofeno o ketorolaco) sin receta médica puede causar toxicidad y daño renal severo. Evita automedicarte."
            )
        }

        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 SUBMENÚ 3: Pruebas y Exámenes
    private fun mostrarMenuPruebas() {
        chipGroup.removeAllViews()

        crearChip("¿Qué es el eGFR?") {
            responderOpcion(
                pregunta = "¿Qué es el eGFR?",
                respuesta = "El eGFR indica el porcentaje de funcionamiento de tus riñones: más de 90 es normal, entre 60 y 89 requiere observación médica, y menos de 60 por 3 meses confirma ERC."
            )
        }

        crearChip("¿Qué es Creatinina?") {
            responderOpcion(
                pregunta = "¿Qué significa la creatinina alta?",
                respuesta = "La creatinina es un desecho natural del trabajo muscular. Si los riñones no filtran bien, sus niveles en sangre se elevan, indicando una menor función renal."
            )
        }

        crearChip("Examen de Orina") {
            responderOpcion(
                pregunta = "¿Para qué sirve el examen de orina?",
                respuesta = "El examen de orina detecta si se están perdiendo proteínas (proteinuria) o glóbulos rojos, que son las primeras señales de que los filtros renales tienen fugas o daño."
            )
        }

        crearChip("🏠 Menú Principal") { mostrarCategoriasPrincipales() }
    }

    // 🚀 SUBMENÚ 4: Prevención y Dieta
    private fun mostrarMenuPrevencion() {
        chipGroup.removeAllViews()

        crearChip("¿Cuánta agua tomar?") {
            responderOpcion(
                pregunta = "¿Cuánta agua debo tomar al día?",
                respuesta = "Se aconseja consumir entre 1.5 y 2 litros de agua natural al día para facilitar la eliminación de toxinas. Si el paciente ya tiene retención o hinchazón, debe consultar su límite exacto con el médico."
            )
        }

        crearChip("¿Cómo reducir la sal?") {
            responderOpcion(
                pregunta = "¿Cómo puedo reducir el consumo de sal?",
                respuesta = "El exceso de sal retiene líquidos y sube la presión arterial. Evita consomés artificiales y embutidos; sazona tus comidas con limón, hierbas, ajo u orégano."
            )
        }

        crearChip("¿Alimentos a evitar?") {
            responderOpcion(
                pregunta = "¿Qué alimentos debo limitar?",
                respuesta = "Limita productos ultraprocesados, bebidas energéticas, gaseosas y exceso de carnes rojas. Si tienes daño renal avanzado, tu nutricionista también regulará el potasio y fósforo."
            )
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

    private fun responderOpcion(pregunta: String, respuesta: String) {
        agregarMensajeChat(pregunta, esUsuario = true)
        agregarMensajeChat(respuesta, esUsuario = false)
    }

    private fun agregarMensajeChat(mensaje: String, esUsuario: Boolean) {
        listaMensajes.add(ChatMessage(mensaje, esUsuario))
        adapter.notifyItemInserted(listaMensajes.size - 1)
        rvChat.smoothScrollToPosition(listaMensajes.size - 1)
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