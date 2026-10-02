package com.insamt.nefroscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.insamt.nefroscan.data.model.EdemaEvaluacion

@Dao
interface EdemaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEvaluacion(evaluacion: EdemaEvaluacion): Long

    @Query("SELECT * FROM evaluaciones_edema ORDER BY fecha DESC")
    suspend fun obtenerTodasEvaluaciones(): List<EdemaEvaluacion>
}