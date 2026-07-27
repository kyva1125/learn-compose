package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GUÍA MAESTRA DE OFFLINE-FIRST (el patrón MÁS senior)
 *
 * Las mejores apps (WhatsApp, Spotify, Gmail) funcionan SIN internet.
 * El truco: la UI NUNCA lee de la red. Lee SIEMPRE de Room.
 *
 *        UI  ←(Flow)←  ROOM  ←(refrescar)←  RETROFIT  ←  Internet
 *
 * "SINGLE SOURCE OF TRUTH" (única fuente de verdad):
 * 1. La pantalla observa Room con un Flow (reactivo, instantáneo, offline).
 * 2. "Refrescar" pide datos a la API y los GUARDA en Room.
 * 3. Room emite el cambio → la UI se actualiza sola.
 * 4. ¿Sin internet? La UI sigue mostrando lo último cacheado. Cero crashes.
 *
 * PRUÉBALO: refresca con internet, luego pon modo avión, cierra y abre
 * la app. Los posts siguen ahí.
 */

// ============================================================
// --- 1. EL REPOSITORY (aquí vive el patrón) ---
// ============================================================
class PostsRepository(
    private val dao: PostCacheDao,           // Room  (RoomParaAndroid.kt)
    private val api: ApiAvanzada = ClienteAvanzado.api  // Retrofit (RedAvanzadaParaAndroid.kt)
) {
    // LA UI SOLO LEE ESTO: el Flow de Room. Nunca la red directamente.
    fun observarPosts(): Flow<List<PostCacheado>> = dao.observarTodos()

    // Refrescar = red → mapear → guardar en Room. La UI ni se entera:
    // reacciona sola cuando Room emite la lista nueva.
    suspend fun refrescar() {
        val postsDeRed = api.obtenerPosts().take(20)
        dao.guardarTodos(postsDeRed.map {
            PostCacheado(id = it.id, titulo = it.titulo, cuerpo = it.cuerpo)
        })
    }

    suspend fun limpiarCache() = dao.limpiar()
}

// ============================================================
// --- 2. EL VIEWMODEL ---
// ============================================================
class OfflineFirstViewModel(private val repo: PostsRepository) : ViewModel() {

    val posts: Flow<List<PostCacheado>> = repo.observarPosts()

    // Estado SOLO del refresco (la lista va aparte, por Room)
    private val _refrescando = MutableStateFlow(false)
    val refrescando = _refrescando.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje = _mensaje.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _refrescando.value = true
            try {
                repo.refrescar()
                _mensaje.value = "✔ Sincronizado con la API y guardado en Room"
            } catch (e: Exception) {
                // FALLÓ LA RED... ¿y qué? La UI sigue mostrando el caché.
                _mensaje.value = "✖ Sin conexión: mostrando datos del caché local"
            }
            _refrescando.value = false
        }
    }

    fun limpiar() {
        viewModelScope.launch {
            repo.limpiarCache()
            _mensaje.value = "🗑 Caché borrado"
        }
    }
}

// ============================================================
// --- 3. LA PANTALLA ---
// ============================================================
@Composable
fun PantallaOfflineFirst() {
    val context = LocalContext.current
    val vm: OfflineFirstViewModel = viewModel {
        OfflineFirstViewModel(
            PostsRepository(AppDatabase.obtener(context).postCacheDao())
        )
    }

    val posts by vm.posts.collectAsState(initial = emptyList())
    val refrescando by vm.refrescando.collectAsState()
    val mensaje by vm.mensaje.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Offline-First", style = MaterialTheme.typography.headlineSmall)
        Text(
            "La lista viene SIEMPRE de Room (caché local). 'Sincronizar' trae " +
                    "datos de la API y los guarda. Prueba en modo avión: sigue funcionando.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { vm.refrescar() }, enabled = !refrescando) {
                Text(if (refrescando) "Sincronizando..." else "⟳ Sincronizar")
            }
            OutlinedButton(onClick = { vm.limpiar() }) { Text("Borrar caché") }
            if (refrescando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        mensaje?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
        }

        Spacer(Modifier.height(12.dp))

        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Caché vacío.\nPulsa 'Sincronizar' (con internet) la primera vez.")
            }
        } else {
            Text("${posts.size} posts en el caché local (Room):",
                style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(posts, key = { it.id }) { post ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(post.titulo, style = MaterialTheme.typography.titleSmall)
                            Text(post.cuerpo, style = MaterialTheme.typography.bodySmall,
                                maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
