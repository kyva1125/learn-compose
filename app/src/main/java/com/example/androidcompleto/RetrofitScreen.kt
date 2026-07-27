package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * GUÍA MAESTRA DE RED CON RETROFIT (Arquitectura MVVM completa)
 *
 * Esta lección junta TODO lo aprendido en una arquitectura profesional:
 *
 *   UI (Composable) → ViewModel → Repository → ApiService (Retrofit) → Internet
 *
 * - MODELO: data class que mapea el JSON de la API.
 * - API SERVICE: interfaz donde declaras los endpoints (Retrofit la implementa).
 * - REPOSITORY: única puerta de acceso a los datos (mañana puedes añadir caché/Room
 *   sin tocar el ViewModel).
 * - VIEWMODEL: pide los datos y expone un StateFlow con el estado de la pantalla.
 * - SEALED INTERFACE: los 3 estados posibles (Cargando/Éxito/Error) — imposible
 *   olvidar uno, el 'when' te obliga a cubrirlos todos.
 *
 * API usada: jsonplaceholder.typicode.com (API pública gratuita de prueba)
 */

// --- 1. EL MODELO (mapea el JSON) ---
// @SerializedName conecta el nombre del campo JSON con tu propiedad Kotlin.
data class Usuario(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val telefono: String,
    @SerializedName("company") val empresa: Empresa
)

data class Empresa(
    @SerializedName("name") val nombre: String
)

// --- 2. EL API SERVICE (los endpoints) ---
// Retrofit GENERA la implementación de esta interfaz por ti.
// "suspend" integra Retrofit con corrutinas de forma nativa.
interface JsonPlaceholderApi {
    @GET("users")
    suspend fun obtenerUsuarios(): List<Usuario>
}

// --- 3. EL CLIENTE RETROFIT (singleton) ---
// "object" = singleton en Kotlin. "by lazy" = se crea solo la primera vez que se usa.
object RetrofitClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    val api: JsonPlaceholderApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON → data class
            .build()
            .create(JsonPlaceholderApi::class.java)
    }
}

// --- 4. EL REPOSITORY (capa de datos) ---
class UsuariosRepository(
    private val api: JsonPlaceholderApi = RetrofitClient.api
) {
    suspend fun obtenerUsuarios(): List<Usuario> = api.obtenerUsuarios()
}

// --- 5. EL ESTADO DE LA UI (sealed = estados cerrados y exhaustivos) ---
sealed interface RedUiState {
    object Cargando : RedUiState
    data class Exito(val usuarios: List<Usuario>) : RedUiState
    data class Error(val mensaje: String) : RedUiState
}

// --- 6. EL VIEWMODEL (lógica de presentación) ---
class UsuariosViewModel(
    private val repository: UsuariosRepository = UsuariosRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RedUiState>(RedUiState.Cargando)
    val uiState: StateFlow<RedUiState> = _uiState.asStateFlow()

    init {
        // Al crearse el ViewModel, carga los datos automáticamente
        cargarUsuarios()
    }

    fun cargarUsuarios() {
        viewModelScope.launch {
            _uiState.value = RedUiState.Cargando
            try {
                val usuarios = repository.obtenerUsuarios()
                _uiState.value = RedUiState.Exito(usuarios)
            } catch (e: Exception) {
                // Sin internet, timeout, JSON malformado... todo cae aquí
                _uiState.value = RedUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

// --- 7. LA PANTALLA (solo pinta el estado, cero lógica) ---
@Composable
fun PantallaRed(
    // viewModel() (no remember{}) crea el ViewModel atado al ciclo de vida:
    // sobrevive rotaciones de pantalla sin repetir la llamada de red.
    viewModel: UsuariosViewModel = viewModel()
) {
    // collectAsStateWithLifecycle: deja de observar cuando la app va a background
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // El when es EXHAUSTIVO: el compilador te obliga a manejar los 3 estados
    when (val s = estado) {
        is RedUiState.Cargando -> PantallaCargando()
        is RedUiState.Error -> PantallaError(mensaje = s.mensaje, onReintentar = { viewModel.cargarUsuarios() })
        is RedUiState.Exito -> ListaUsuarios(usuarios = s.usuarios)
    }
}

@Composable
fun PantallaCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Descargando usuarios de la API...")
        }
    }
}

@Composable
fun PantallaError(mensaje: String, onReintentar: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("😕 Algo salió mal", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReintentar) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun ListaUsuarios(usuarios: List<Usuario>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(usuarios, key = { it.id }) { usuario ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coil descarga y cachea la imagen automáticamente
                    AsyncImage(
                        model = "https://i.pravatar.cc/150?u=${usuario.id}",
                        contentDescription = "Avatar de ${usuario.nombre}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(usuario.email, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "🏢 ${usuario.empresa.nombre}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
