package com.example.androidcompleto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * GUÍA MAESTRA DE GESTIÓN DE ESTADO EN ANDROID
 * Aquí aprenderás cómo fluye la información entre tu lógica y tu interfaz.
 */

// --- 1. ESTADO LOCAL (El "setState" de Compose) ---
@Composable
fun ContadorLocal() {
    // remember: hace que el valor sobreviva a la recomposición.
    // mutableIntStateOf: es la caja reactiva optimizada para enteros. Si cambia, la UI se refresca.
    var count by remember { mutableIntStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Clics: $count")
    }
}

// --- 2. VIEWMODEL (Tu BLoC / StateNotifier) ---
// El ViewModel sobrevive a cambios de rotación y separa la lógica de la UI.
class MiViewModel : ViewModel() {

    // 3. REACTIVIDAD CON STATEFLOW (Tus Streams de datos)
    // MutableStateFlow: Solo se modifica dentro del ViewModel (privado).
    private val _uiState = MutableStateFlow("Estado Inicial")
    
    // StateFlow: La versión de solo lectura que la UI observa.
    val uiState: StateFlow<String> = _uiState.asStateFlow()

    // SharedFlow: Para eventos que no se repiten (como mostrar un Toast o navegar).
    private val _eventos = MutableSharedFlow<String>()
    val eventos: SharedFlow<String> = _eventos

    fun actualizarTexto(nuevoTexto: String) {
        // Ejecutamos en un scope de corrutina para no bloquear la app
        viewModelScope.launch {
            _uiState.value = nuevoTexto
        }
    }
    
    fun lanzarEvento() {
        viewModelScope.launch {
            _eventos.emit("¡Evento disparado!")
        }
    }
}

// --- CÓMO CONECTAR TODO EN LA UI ---
@Composable
fun PantallaPrincipal(viewModel: MiViewModel) {
    // collectAsState: Transforma el Flow en un Estado de Compose que la UI entiende.
    val textoActual by viewModel.uiState.collectAsState()

    Column {
        Text(text = "Dato del ViewModel: $textoActual")
        
        Button(onClick = { 
            viewModel.actualizarTexto("¡Dato Cambiado!") 
        }) {
            Text("Cambiar Estado Global")
        }
    }
}

@Composable
fun PantallaEstadoApp() {
    // viewModel() ata la instancia al ciclo de vida: sobrevive rotaciones
    val viewModel: MiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("Lección 3: Gestión de Estado", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado Local (remember):", style = MaterialTheme.typography.titleMedium)
                ContadorLocal()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado Global (ViewModel + Flow):", style = MaterialTheme.typography.titleMedium)
                PantallaPrincipal(viewModel = viewModel)
            }
        }
    }
}
