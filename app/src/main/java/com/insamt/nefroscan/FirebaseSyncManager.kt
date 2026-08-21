package com.insamt.nefroscan

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val localDao = NefroScanDatabase.getDatabase(context).diagnosticDao()

    suspend fun sincronizarExpedientesPendientes(): Int = withContext(Dispatchers.IO) {
        val pendientes = localDao.obtenerPendientesDeSincronizar()
        var contSincronizados = 0

        for (expediente in pendientes) {
            val datosFirebase = hashMapOf(
                "idLocal" to expediente.id,
                "idPaciente" to expediente.idPaciente,
                "idRegistrador" to expediente.idRegistrador,
                "rolRegistrador" to expediente.rolRegistrador,
                "idMedicoAsignado" to expediente.idMedicoAsignado,
                "nombrePaciente" to expediente.nombrePaciente,
                "edadPaciente" to expediente.edadPaciente,
                "porcentajeDano" to expediente.porcentajeDano,
                "patologiaDetectada" to expediente.patologiaDetectada,
                "nivelSeveridad" to expediente.nivelSeveridad,
                "litrosAguaDiarios" to expediente.litrosAguaDiarios,
                "nivelSodio" to expediente.nivelSodio,
                "egfrEstimado5Anios" to expediente.egfrEstimado5Anios,
                "egfrEstimado10Anios" to expediente.egfrEstimado10Anios,
                "fechaRegistroTimestamp" to expediente.fechaRegistroTimestamp
            )

            try {
                db.collection("expedientes_nefroscan")
                    .add(datosFirebase)
                    .await()

                localDao.marcarComoSincronizado(expediente.id)
                contSincronizados++
                Log.d("NefroScanSync", "Expediente ${expediente.id} sincronizado exitosamente.")
            } catch (e: Exception) {
                Log.e("NefroScanSync", "Error al sincronizar expediente ${expediente.id}: ${e.message}")
            }
        }

        return@withContext contSincronizados
    }
}