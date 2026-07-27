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

/**
 * EL TEMARIO COMPLETO EN TRES PISTAS.
 * Cada pista se puede recorrer por separado, pero el orden recomendado es
 * Kotlin → Compose → KMP: la última reutiliza todo lo anterior.
 */
val rutaCompleta = listOf(
    // ══════════════ 🟣 PISTA KOTLIN ══════════════
    NivelAprendizaje("🟣 Kotlin · Fundamentos", "🌱", listOf(
        HitoAprendizaje("k_sintaxis", "Sintaxis Kotlin", "val/var, null safety, when, data classes", "K1"),
        HitoAprendizaje("k_lambdas", "Colecciones y lambdas", "filter, map, groupBy — úsalos sin pensar", "K1"),
        HitoAprendizaje("k_clases", "Clases y sealed", "Herencia, interfaces, sealed para estados", "K1"),
    )),
    NivelAprendizaje("🟣 Kotlin · Avanzado", "🧬", listOf(
        HitoAprendizaje("ka_varianza", "Genéricos y varianza", "in / out, reified, type erasure", "K2"),
        HitoAprendizaje("ka_delegados", "Delegados", "by lazy, observable y delegados propios", "K2"),
        HitoAprendizaje("ka_inline", "inline / crossinline", "Por qué forEach no crea objetos", "K2"),
        HitoAprendizaje("ka_dsl", "DSLs y value class", "Lambdas con receptor, @JvmInline", "K2"),
        HitoAprendizaje("ki_seq", "Sequences", "Evaluación perezosa: cuándo compensa y cuándo no", "K3"),
        HitoAprendizaje("ki_chan", "Channels", "Cola caliente entre corrutinas · Flow frío vs Channel", "K3"),
        HitoAprendizaje("ki_ops", "Colecciones avanzadas", "fold, zip, windowed, partition, groupingBy", "K3"),
        HitoAprendizaje("ki_azucar", "Azúcar idiomático", "infix, destructuring, companion object, Result", "K3"),
    )),
    NivelAprendizaje("🟣 Kotlin · Concurrencia", "⚙️", listOf(
        HitoAprendizaje("co_suspend", "Corrutinas", "suspend, launch, async/await, Dispatchers", "K3"),
        HitoAprendizaje("co_flow", "Flows", "flow, operadores, collect, StateFlow vs SharedFlow", "K3"),
        HitoAprendizaje("co_estructurada", "Concurrencia estructurada", "Padres e hijos, cancelación cooperativa", "K4"),
        HitoAprendizaje("co_errores", "Errores en corrutinas", "coroutineScope vs supervisorScope, handlers", "K4"),
        HitoAprendizaje("co_operadores", "Operadores de Flow", "flatMapLatest, debounce, combine, retry, buffer", "K4"),
    )),
    NivelAprendizaje("🟣 Kotlin · Calidad", "🧪", listOf(
        HitoAprendizaje("t_runtest", "runTest y tiempo virtual", "advanceTimeBy, advanceUntilIdle, TestDispatcher", "K5"),
        HitoAprendizaje("t_turbine", "Turbine", "awaitItem() sobre Flows, sin tests inestables", "K5"),
        HitoAprendizaje("t_fakes", "Fakes sobre mocks", "Dobles de prueba escritos a mano", "K5"),
        HitoAprendizaje("t_arch", "Clean + MVI", "UseCases, reducer puro, estado único inmutable", "K6"),
        HitoAprendizaje("t_dominio", "Dominio testeado", "Reducer y UseCase probados sin Android", "ArquitecturaSeniorTest.kt"),
    )),

    // ══════════════ 🎨 PISTA COMPOSE ══════════════
    NivelAprendizaje("🎨 Compose · Base", "🧱", listOf(
        HitoAprendizaje("c_layouts", "Layouts básicos", "Column, Row, Box, Modifier y su ORDEN", "C1"),
        HitoAprendizaje("c_listas", "Listas Lazy", "LazyColumn/Row/Grid con keys", "C1"),
        HitoAprendizaje("e_remember", "Estado local", "remember, mutableStateOf, rememberSaveable", "C2"),
        HitoAprendizaje("e_vm", "ViewModel + StateFlow", "Estado que sobrevive rotaciones", "C2"),
        HitoAprendizaje("ef_efectos", "Efectos secundarios", "LaunchedEffect, DisposableEffect, derivedStateOf", "C3"),
    )),
    NivelAprendizaje("🎨 Compose · App real", "🏗", listOf(
        HitoAprendizaje("n_nav", "Navegación", "NavHost, rutas con argumentos, back stack", "C4"),
        HitoAprendizaje("n_navts", "Navegación type-safe", "Rutas @Serializable, toRoute(), deep links", "C5"),
        HitoAprendizaje("a_anim", "Animaciones", "animateAsState, AnimatedVisibility, transiciones", "C6"),
        HitoAprendizaje("f_forms", "Formularios", "State hoisting + validación derivada", "C7"),
    )),
    NivelAprendizaje("🎨 Compose · Profesional", "💎", listOf(
        HitoAprendizaje("g_gestos", "Gestos táctiles", "pointerInput, draggable, swipe-to-dismiss, zoom", "C8"),
        HitoAprendizaje("g_a11y", "Accesibilidad", "Semantics, TalkBack, mergeDescendants, 48dp", "C9"),
        HitoAprendizaje("g_adaptive", "Layouts adaptativos", "WindowSizeClass, list-detail, plegables", "C10"),
        HitoAprendizaje("g_interop", "Interop con Views", "AndroidView, ComposeView, migración legacy", "C11"),
        HitoAprendizaje("g_uitest", "Tests de UI", "composeTestRule, finders, acciones, aserciones", "BuscadorUiTest.kt"),
    )),
    NivelAprendizaje("🎨 Compose · Sistema de diseño", "🎭", listOf(
        HitoAprendizaje("m3_color", "Roles de color M3", "primary/onPrimary, dynamic color, modo oscuro", "C12"),
        HitoAprendizaje("m3_comp", "Componentes M3", "Diálogos, ModalBottomSheet, snackbars, chips, badges", "C12"),
        HitoAprendizaje("m3_insets", "Insets y edge-to-edge", "WindowInsets, imePadding, safeDrawing", "C12"),
        HitoAprendizaje("tx_annot", "AnnotatedString", "Estilos, colores y resaltado dentro de un Text", "C13"),
        HitoAprendizaje("tx_mask", "Máscaras de entrada", "VisualTransformation + OffsetMapping", "C13"),
        HitoAprendizaje("tx_foco", "Teclado y foco", "ImeAction, moveFocus, cerrar el teclado", "C13"),
        HitoAprendizaje("li_lazy", "Listas Lazy a fondo", "key, contentType, stickyHeader, animateItem", "C14"),
        HitoAprendizaje("li_pag", "Scroll infinito", "Paginación manual y qué automatiza Paging 3", "C14"),
        HitoAprendizaje("li_saver", "Savers y process death", "rememberSaveable con Saver propio, produceState", "C14"),
    )),
    NivelAprendizaje("🎨 Compose · Interno", "🔬", listOf(
        HitoAprendizaje("x_perf", "Rendimiento", "Skipping, estabilidad, derivedStateOf, lecturas diferidas", "C12"),
        HitoAprendizaje("x_layout", "Layouts custom y Canvas", "Layout(), Modifier.layout y dibujo directo", "C13"),
        HitoAprendizaje("x_cl", "CompositionLocal", "Design tokens y theming como MaterialTheme", "C14"),
    )),

    // ══════════════ 🌍 PISTA KMP Y DATOS ══════════════
    NivelAprendizaje("🌍 Datos · Android", "💾", listOf(
        HitoAprendizaje("r_retrofit", "Retrofit + MVVM", "API → Repository → ViewModel → sealed UiState", "M4"),
        HitoAprendizaje("p_datastore", "DataStore", "Preferencias persistentes como Flow", "M5"),
        HitoAprendizaje("s_room", "Room", "Entity, DAO con Flow, CRUD offline", "M6"),
        HitoAprendizaje("s_red2", "Retrofit avanzado", "Interceptores, POST, Response<T>, errores HTTP", "M7"),
        HitoAprendizaje("s_offline", "Offline-First", "Single Source of Truth: Room + Retrofit", "M8"),
    )),
    NivelAprendizaje("🌍 KMP · Multiplataforma", "🚀", listOf(
        HitoAprendizaje("m_expect", "expect / actual", "commonMain, androidMain, iosMain", "M1"),
        HitoAprendizaje("m_cmp", "Compose Multiplatform", "Una UI para Android, iOS, Desktop y Web", "M1"),
        HitoAprendizaje("m_ktor", "Ktor Client", "HTTP multiplataforma con motores por plataforma", "M2"),
        HitoAprendizaje("m_serial", "kotlinx.serialization", "JSON sin reflexión, generado en compilación", "M2"),
        HitoAprendizaje("m_koin", "Koin (DI en KMP)", "single/factory, por qué Hilt no vale en iOS", "M3"),
        HitoAprendizaje("m_reto", "Reto final", "Migra tu dominio a un proyecto KMP y compílalo para iOS", "Tu proyecto"),
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
