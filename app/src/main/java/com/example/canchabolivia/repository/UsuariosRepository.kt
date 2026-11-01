package com.example.canchabolivia.repository

import com.example.canchabolivia.Usuario
import com.example.canchabolivia.database.Seguridad
import com.example.canchabolivia.database.UsuarioDao
import com.example.canchabolivia.database.UsuarioEntity

// Usuarios guardados en el teléfono. La clave se compara por hash y nunca sale de aquí.
class UsuariosRepository(private val dao: UsuarioDao) {

    suspend fun iniciarSesion(correo: String, clave: String): Usuario? =
        dao.buscar(correo.trim(), Seguridad.hash(clave.trim()))?.aModelo()

    suspend fun porId(id: Int): Usuario? = dao.porId(id)?.aModelo()

    private fun UsuarioEntity.aModelo() = Usuario(id, nombre, correo, "", rol)
}
