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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button
    private val listaMensajes = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        rvChat = findViewById(R.id.rvChatMessages)
        etMensaje = findViewById(R.id.etChatMessage)
        btnEnviar = findViewById(R.id.btnSendChat)
        val btnVolver = findViewById<Button>(R.id.btnVolverHistorial)

        adapter = ChatAdapter(listaMensajes)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // Mensaje inicial de bienvenida
        agregarMensajeChat("Hola, soy tu asistente de salud renal NefroScan. ¿En que puedo ayudarte hoy con el cuidado de tus riñones?", esUsuario = false)

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

    private fun procesarPreguntaUsuario(pregunta: String) {
        // 1. Mostrar la pregunta del usuario en pantalla
        agregarMensajeChat(pregunta, esUsuario = true)
        etMensaje.setText("")

        // 2. Simulación de procesamiento de lenguaje natural / Gemini
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000) // Simulación de respuesta asíncrona

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
            p.contains("agua") || p.contains("líquido") || p.contains("hidratación") ->
                "Para adultos en climas cálidos o trabajo de campo, se recomienda un consumo de 2 a 3 litros de agua diarios, evitando bebidas azucaradas o carbonatadas."
            p.contains("sodio") || p.contains("sal") ->
                "Reducir el consumo de sal previene la hipertensión arterial, un factor critico de riesgo para la enfermedad renal cronica."
            p.contains("síntoma") || p.contains("dolor") || p.contains("hinchazón") ->
                "Los síntomas como hinchazón en piernas (edema), fatiga o cambios en la orina requieren evaluación médica. Si tu riesgo en el tamizaje dio Rojo, acude a tu unidad de salud."
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