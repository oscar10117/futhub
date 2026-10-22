package com.example.canchabolivia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.canchabolivia.api.OpenMeteoApi
import com.example.canchabolivia.database.FutHubDatabase
import com.example.canchabolivia.model.Clima
import com.example.canchabolivia.repository.ClimaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClimaUiState(
    val clima: Clima? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

class ClimaViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = ClimaRepository(
        OpenMeteoApi.instancia,
        FutHubDatabase.obtener(app).climaDao()
    )

    private val _estado = MutableStateFlow(ClimaUiState())
    val estado: StateFlow<ClimaUiState> = _estado.asStateFlow()

    init {
        // Lo guardado aparece al instante (incluso sin internet)...
        viewModelScope.launch {
            repositorio.guardado.collect { guardado ->
                _estado.update { actual ->
                    // ...pero no pisa el dato fresco que acaba de bajar de la API.
                    if (actual.clima != null && !actual.clima.desdeCache) actual
                    else actual.copy(clima = guardado)
                }
            }
        }
        actualizar()
    }

    fun actualizar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repositorio.actualizar()
                .onSuccess { fresco ->
                    _estado.update { it.copy(clima = fresco, cargando = false) }
                }
                .onFailure {
                    _estado.update {
                        it.copy(cargando = false, error = "Sin conexión. Mostrando la última lectura guardada.")
                    }
                }
        }
    }
}
