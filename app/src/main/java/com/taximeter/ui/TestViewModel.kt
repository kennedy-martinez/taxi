package com.taximeter.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taximeter.domain.repository.TaximeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val repository: TaximeterRepository
) : ViewModel() {

    init {
        Log.d("TestViewModel", "ViewModel inicializado.")

        viewModelScope.launch {
            repository.fetchPriceConfigIfNeeded()
        }

        viewModelScope.launch {
            repository.getPriceConfig()
                .catch { e -> Log.e("TestViewModel", "Error en el Flow de config", e) }
                .collect { config ->
                    if (config != null) {
                        Log.d("TestViewModel", "¡ÉXITO DESDE BBDD! Config: $config")
                    } else {
                        Log.d("TestViewModel", "BBDD vacía, esperando carga...")
                    }
                }
        }
    }
}