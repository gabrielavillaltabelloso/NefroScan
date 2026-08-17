package com.insamt.nefroscan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpedienteAdapter(
    private val listaExpedientes: List<DiagnosticEntity>
) : RecyclerView.Adapter<ExpedienteAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePacienteItem)
        val tvSeveridad: TextView = itemView.findViewById(R.id.tvSeveridadItem)
        val tvPatologia: TextView = itemView.findViewById(R.id.tvPatologiaItem)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvDetallesItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expediente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaExpedientes[position]

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaFormateada = dateFormat.format(Date(item.fechaRegistroTimestamp))

        holder.tvNombre.text = "${item.nombrePaciente} (${item.edadPaciente} años)"
        holder.tvPatologia.text = "Patología: ${item.patologiaDetectada}"
        holder.tvSeveridad.text = item.nivelSeveridad

        val danoFormateado = "%.1f".format(Locale.US, item.porcentajeDano)
        val egfrFormateado = "%.0f".format(Locale.US, item.egfrEstimado5Anios)

        holder.tvDetalles.text = "Daño: $danoFormateado% | eGFR 5 años: $egfrFormateado | Fecha: $fechaFormateada"

        when {
            item.nivelSeveridad.contains("Alto", ignoreCase = true) ||
                    item.nivelSeveridad.contains("Rojo", ignoreCase = true) ||
                    item.porcentajeDano >= 50.0 -> {
                holder.tvSeveridad.setTextColor(Color.parseColor("#EF4444"))
            }
            item.nivelSeveridad.contains("Moder", ignoreCase = true) -> {
                holder.tvSeveridad.setTextColor(Color.parseColor("#F59E0B"))
            }
            else -> {
                holder.tvSeveridad.setTextColor(Color.parseColor("#10B981"))
            }
        }
    }

    override fun getItemCount(): Int = listaExpedientes.size
}