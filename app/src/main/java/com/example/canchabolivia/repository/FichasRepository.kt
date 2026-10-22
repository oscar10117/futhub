package com.example.canchabolivia.repository

import com.example.canchabolivia.FichaMedica
import com.example.canchabolivia.database.FichaDao
import com.example.canchabolivia.mapper.aEntity
import com.example.canchabolivia.mapper.aModelo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Fichas médicas guardadas en el teléfono.
class FichasRepository(private val dao: FichaDao) {

    val fichas: Flow<List<FichaMedica>> =
        dao.observarFichas().map { lista -> lista.map { it.aModelo() } }

    // El id de una ficha nueva lo pone Room (llega en 0).
    suspend fun agregar(ficha: FichaMedica) = dao.insertar(ficha.aEntity())

    suspend fun actualizar(ficha: FichaMedica) = dao.actualizar(ficha.aEntity())
}
