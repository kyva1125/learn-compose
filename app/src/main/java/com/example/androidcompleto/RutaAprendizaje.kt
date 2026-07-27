package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * RUTA DE APRENDIZAJE CON PROGRESO PERSISTENTE
 * Esta pantalla ES una lección encubierta: guarda tu progreso en DataStore
 * (lección 10), lo lee como Flow reactivo y calcula el % con derivedStateOf
 * (lección 5). Marca cada hito cuando lo domines: sobrevive al cierre de la app.
 */

data class HitoAprendizaje(
    val clave: String,      // clave única para DataStore
    val titulo: String,
    val detalle: String,
    val leccion: String     // dónde practicarlo en esta app
)

data class NivelAprendizaje(
    val nombre: String,
    val emoji: String,
    val hitos: List<HitoAprendizaje>
)

val rutaCompleta = listOf(
    NivelAprendizaje("Nivel 1: Fundamentos", "🌱", listOf(
        HitoAprendizaje("k_sintaxis", "Sintaxis Kotlin", "val/var, null safety, when, data classes", "Lección 1"),
        HitoAprendizaje("k_lambdas", "Colecciones y lambdas", "filter, map, groupBy — úsalos sin pensar", "Lección 1"),
        HitoAprendizaje("c_layouts", "Layouts básicos", "Column, Row, Box, Modifier y su ORDEN", "Lección 2"),
        HitoAprendizaje("c_listas", "Listas Lazy", "LazyColumn/Row/Grid con keys", "Lección 2"),
    )),
    NivelAprendizaje("Nivel 2: Reactividad", "⚡", listOf(
        HitoAprendizaje("e_remember", "Estado local", "remember, mutableStateOf, rememberSaveable", "Lección 3"),
        HitoAprendizaje("e_vm", "ViewModel + StateFlow", "Estado que sobrevive rotaciones", "Lección 3"),
        HitoAprendizaje("co_suspend", "Corrutinas", "suspend, launch, async/await, Dispatchers", "Lección 4"),
        HitoAprendizaje("co_flow", "Flows", "flow, operadores, collect, StateFlow vs SharedFlow", "Lección 4"),
        HitoAprendizaje("ef_efectos", "Efectos secundarios", "LaunchedEffect, DisposableEffect, derivedStateOf", "Lección 5"),
    )),
    NivelAprendizaje("Nivel 3: App real", "🏗", listOf(
        HitoAprendizaje("n_nav", "Navegación", "NavHost, rutas con argumentos, back stack", "Lección 6"),
        HitoAprendizaje("a_anim", "Animaciones", "animateAsState, AnimatedVisibility, transiciones", "Lección 7"),
        HitoAprendizaje("f_forms", "Formularios", "State hoisting + validación derivada", "Lección 8"),
        HitoAprendizaje("r_retrofit", "Retrofit + MVVM", "API → Repository → ViewModel → sealed UiState", "Lección 9"),
        HitoAprendizaje("p_datastore", "DataStore", "Preferencias persistentes como Flow", "Lección 10"),
    )),
    NivelAprendizaje("Nivel 4: Senior", "🚀", listOf(
        HitoAprendizaje("s_room", "Room", "Entity, DAO con Flow, CRUD offline", "Lección 11"),
        HitoAprendizaje("s_red2", "Retrofit avanzado", "Interceptores, POST, Response<T>, errores HTTP", "Lección 12"),
        HitoAprendizaje("s_offline", "Offline-First", "Single Source of Truth: Room + Retrofit", "Lección 13"),
        HitoAprendizaje("s_test", "Testing", "JUnit + patrón AAA sobre lógica pura", "LogicaKotlinTest.kt"),
        HitoAprendizaje("s_arch", "Reto final", "Crea TU app: API + caché Room + navegación, sin mirar", "Tu proyecto"),
    )),
)

@Composable
fun PantallaRuta() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Leemos TODO el progreso como un Set de claves completadas (reactivo)
    val completados by context.dataStore.data
        .map { prefs ->
            rutaCompleta.flatMap { it.hitos }
                .filter { hito -> prefs[booleanPreferencesKey("ruta_${hito.clave}")] == true }
                .map { it.clave }
                .toSet()
        }
        .collectAsState(initial = emptySet())

    val totalHitos = rutaCompleta.sumOf { it.hitos.size }
    val progreso by remember {
        derivedStateOf { completados.size / totalHitos.toFloat() }
    }

    fun alternarHito(clave: String, valor: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[booleanPreferencesKey("ruta_$clave")] = valor
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Tu camino a Senior 🎯", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${completados.size} de $totalHitos hitos — " +
                                when {
                                    progreso == 0f -> "¡empieza por el Nivel 1!"
                                    progreso < 0.5f -> "buen ritmo, sigue así"
                                    progreso < 1f -> "ya piensas en Compose 💪"
                                    else -> "🏆 ¡NIVEL SENIOR DESBLOQUEADO!"
                                },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "El progreso se guarda con DataStore: cierra la app y aquí seguirá.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        rutaCompleta.forEach { nivel ->
            item {
                val hechosNivel = nivel.hitos.count { it.clave in completados }
                Text(
                    "${nivel.emoji} ${nivel.nombre}  ($hechosNivel/${nivel.hitos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(nivel.hitos.size) { i ->
                val hito = nivel.hitos[i]
                val hecho = hito.clave in completados
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hecho,
                            onCheckedChange = { alternarHito(hito.clave, it) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                hito.titulo,
                                style = MaterialTheme.typography.titleSmall,
                                textDecoration = if (hecho) TextDecoration.LineThrough
                                else TextDecoration.None
                            )
                            Text(hito.detalle, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "📍 ${hito.leccion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
