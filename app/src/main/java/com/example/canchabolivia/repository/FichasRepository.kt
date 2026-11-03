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

    // El id de una ficha nueva lo pone Room (llega en 0); se devuelve ya con su id.
    suspend fun agregar(ficha: FichaMedica): FichaMedica =
        ficha.copy(id = dao.insertar(ficha.aEntity()).toInt())

    suspend fun actualizar(ficha: FichaMedica) = dao.actualizar(ficha.aEntity())
}
