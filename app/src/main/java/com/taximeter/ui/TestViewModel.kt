package com.taximeter.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taximeter.domain.repository.TaximeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val repository: TaximeterRepository
) : ViewModel() {

    init {
        Log.d("TestViewModel", "ViewModel inicializado. Realizando llamada de prueba...")
        testApiCall()
    }

    private fun testApiCall() {
        viewModelScope.launch {
            val config = repository.getPriceConfig().firstOrNull()
            if (config != null) {
                Log.d("TestViewModel", "¡ÉXITO! Configuración recibida: $config")
            } else {
                Log.e("TestViewModel", "FALLO. No se recibió configuración.")
            }
        }
    }
}