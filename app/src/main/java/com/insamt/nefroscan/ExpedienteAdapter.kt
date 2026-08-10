package com.insamt.nefroscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpedienteAdapter(private val lista: List<DiagnosticEntity>) :
    RecyclerView.Adapter<ExpedienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePacienteItem)
        val tvSeveridad: TextView = view.findViewById(R.id.tvSeveridadItem)
        val tvPatologia: TextView = view.findViewById(R.id.tvPatologiaItem)
        val tvDetalles: TextView = view.findViewById(R.id.tvDetallesItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expediente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = "${item.nombrePaciente} (${item.edadPaciente} años)"
        holder.tvSeveridad.text = item.nivelSeveridad
        holder.tvPatologia.text = "Patología: ${item.patologiaDetectada}"
        holder.tvDetalles.text = "Daño: ${"%.1f".format(item.porcentajeDano)}% | eGFR 5 años: ${item.egfrEstimado5Anios}"
    }

    override fun getItemCount(): Int = lista.size
}