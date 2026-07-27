package com.example.androidcompleto

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LISTAS LAZY AVANZADAS Y ESTADO — PISTA COMPOSE (Lección C17)
 *
 * Una LazyColumn con items() cubre lo básico. Esta lección cubre lo que
 * hace falta cuando la lista es real: encabezados fijos, tipos mezclados,
 * animaciones al insertar/borrar, scroll controlado, carga paginada y
 * estado que sobrevive a la muerte del proceso.
 *
 * POR QUÉ IMPORTA `key`:
 *   Sin key, Compose identifica los items por POSICIÓN. Si insertas uno al
 *   principio, cree que TODOS cambiaron: pierde el estado de scroll, rompe
 *   las animaciones y recompone de más.
 *   Con key = { it.id }, sabe exactamente qué se movió.
 *
 * POR QUÉ IMPORTA `contentType`:
 *   Compose reutiliza la estructura de composición entre items del mismo
 *   tipo. Si mezclas cabeceras y filas sin declarar contentType, no puede
 *   reutilizar nada y va más lento.
 */

data class ContactoDemo(val id: Int, val nombre: String) {
    val inicial: Char get() = nombre.first().uppercaseChar()
}

private val contactosBase = listOf(
    "Ana Quispe", "Alberto Ríos", "Beatriz Luna", "Carlos Mendoza",
    "Carmen Díaz", "Diego Ramos", "Elena Soto", "Fernando Cruz"
).mapIndexed { i, n -> ContactoDemo(i + 1, n) }

// ============================================================
// --- 1. Sticky headers, contentType y animateItem ---
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DemoListaCompleta() {
    var contactos by remember { mutableStateOf(contactosBase) }
    var siguienteId by remember { mutableIntStateOf(100) }
    val estadoLista = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Agrupamos por inicial para las cabeceras
    val agrupados = remember(contactos) {
        contactos.sortedBy { it.nombre }.groupBy { it.inicial }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Cabeceras fijas y animaciones",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Haz scroll: la letra se queda pegada arriba. Añade o borra: los items " +
                        "se reordenan con animación gracias a la key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val nombres = listOf("Bruno Vega", "Ada Lovelace", "Zoe Marín", "Cesar Paz")
                    contactos = contactos + ContactoDemo(
                        siguienteId++, nombres.random()
                    )
                }) { Text("Añadir") }
                OutlinedButton(
                    onClick = { if (contactos.isNotEmpty()) contactos = contactos.drop(1) },
                    enabled = contactos.isNotEmpty()
                ) { Text("Borrar 1º") }
                OutlinedButton(onClick = {
                    scope.launch { estadoLista.animateScrollToItem(0) }
                }) { Text("⬆") }
            }

            LazyColumn(
                state = estadoLista,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                agrupados.forEach { (inicial, personas) ->
                    stickyHeader(key = "cab_$inicial", contentType = "cabecera") {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                inicial.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(
                        items = personas,
                        key = { it.id },                 // identidad estable
                        contentType = { "contacto" }     // permite reutilizar la estructura
                    ) { persona ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                // animateItem anima inserción, borrado y REORDENACIÓN.
                                // Solo funciona si hay key.
                                .animateItem(
                                    placementSpec = tween(400)
                                )
                        ) {
                            Text(
                                "${persona.nombre}  (id ${persona.id})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
            Text("${contactos.size} contactos", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ============================================================
// --- 2. Paginación: cargar más al llegar al final ---
// ============================================================
@Composable
fun DemoPaginacion() {
    var pagina by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var hayMas by remember { mutableStateOf(true) }
    val estado = rememberLazyListState()

    // La condición se calcula en cada scroll, pero derivedStateOf hace que
    // solo recomponga cuando el booleano CAMBIA (lección de rendimiento).
    val debeCargarMas by remember {
        derivedStateOf {
            val ultimoVisible = estado.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = estado.layoutInfo.totalItemsCount
            total > 0 && ultimoVisible >= total - 3
        }
    }

    // Dispara la carga cuando la condición se vuelve cierta
    LaunchedEffect(debeCargarMas, hayMas) {
        if (debeCargarMas && hayMas && !cargando) {
            cargando = true
            delay(700)                       // simula la petición de red
            val nuevos = (1..10).map { "Elemento ${pagina * 10 + it}" }
            items = items + nuevos
            pagina++
            hayMas = pagina < 4              // 4 páginas y se acabó
            cargando = false
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("2. Scroll infinito (paginación)",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Baja hasta el final: se cargan más solos. El patrón manual detrás de " +
                        "lo que automatiza la librería Paging 3.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            LazyColumn(
                state = estado,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item }) { _, item ->
                    Text(item, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
                if (cargando) {
                    item(contentType = "cargando") {
                        Box(Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                if (!hayMas && items.isNotEmpty()) {
                    item(contentType = "fin") {
                        Text("— no hay más resultados —",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                }
            }
            Text("Página $pagina · ${items.size} elementos",
                style = MaterialTheme.typography.labelSmall)

            Text(
                "En producción usarías Paging 3 (androidx.paging): gestiona páginas, " +
                        "reintentos, estados de carga y caché con Room. El patrón conceptual " +
                        "es este mismo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 3. rememberSaveable con Savers propios ---
// ============================================================
/**
 * rememberSaveable guarda en el Bundle → sobrevive a la ROTACIÓN y a la
 * MUERTE DEL PROCESO (Android mata tu app en segundo plano por memoria).
 *
 * De serie solo sabe guardar tipos primitivos y Parcelables. Para una clase
 * propia hay que decirle CÓMO convertirla a algo guardable: eso es un Saver.
 */
data class FiltroBusqueda(val texto: String, val soloFavoritos: Boolean, val minPrecio: Int)

/** listSaver: convierte el objeto en una lista y de vuelta. El más simple. */
val FiltroSaver: Saver<FiltroBusqueda, Any> = listSaver(
    save = { listOf(it.texto, it.soloFavoritos, it.minPrecio) },
    restore = {
        FiltroBusqueda(
            texto = it[0] as String,
            soloFavoritos = it[1] as Boolean,
            minPrecio = it[2] as Int
        )
    }
)

@Composable
fun DemoSaver() {
    // Con el Saver propio: la data class entera sobrevive a rotación y a
    // muerte de proceso. `stateSaver` es la variante para un MutableState.
    var filtroGuardado by rememberSaveable(stateSaver = FiltroSaver) {
        mutableStateOf(FiltroBusqueda("compose", true, 50))
    }

    // Solo sobrevive a la recomposición: se pierde al rotar
    var soloRemember by remember { mutableStateOf(0) }
    // Sobrevive a rotación y muerte de proceso (tipo primitivo)
    var conSaveable by rememberSaveable { mutableStateOf(0) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. Estado que sobrevive de verdad",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Pulsa los dos y GIRA el dispositivo: uno se reinicia a 0 y el otro no.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { soloRemember++ }) { Text("remember: $soloRemember") }
                Button(onClick = { conSaveable++ }) { Text("Saveable: $conSaveable") }
            }

            HorizontalDivider()
            Text("Saver propio para una data class",
                style = MaterialTheme.typography.labelLarge)
            Text("Filtro actual: \"${filtroGuardado.texto}\" · " +
                    "favoritos=${filtroGuardado.soloFavoritos} · " +
                    "min=${filtroGuardado.minPrecio}",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    filtroGuardado = filtroGuardado.copy(
                        soloFavoritos = !filtroGuardado.soloFavoritos
                    )
                }) { Text("Cambiar favoritos") }
                OutlinedButton(onClick = {
                    filtroGuardado = filtroGuardado.copy(
                        minPrecio = filtroGuardado.minPrecio + 10
                    )
                }) { Text("+10 precio") }
            }
            Text("Cámbialo, gira el dispositivo y comprueba que sigue igual.",
                style = MaterialTheme.typography.labelSmall)

            BloqueCodigo(
                "Un Saver en 5 líneas",
                """
                val FiltroSaver = listSaver<FiltroBusqueda, Any>(
                    save    = { listOf(it.texto, it.soloFavoritos, it.minPrecio) },
                    restore = { FiltroBusqueda(it[0] as String, it[1] as Boolean, it[2] as Int) }
                )

                // stateSaver → para un MutableState<T>
                var filtro by rememberSaveable(stateSaver = FiltroSaver) {
                    mutableStateOf(FiltroBusqueda("", false, 0))
                }

                // saver → para un valor T normal (sin MutableState)
                val filtroFijo = rememberSaveable(saver = FiltroSaver) {
                    FiltroBusqueda("", false, 0)
                }

                // Alternativa sin Saver: marca la clase como @Parcelize
                @Parcelize
                data class FiltroBusqueda(...) : Parcelable
                """.trimIndent()
            )

            Text(
                "⚠ El Bundle tiene un límite (~500 KB). rememberSaveable es para estado " +
                        "de UI pequeño (texto de un campo, posición, filtros), NUNCA para " +
                        "listas de datos: esas van en Room o se recargan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 4. produceState: convertir algo asíncrono en estado ---
// ============================================================
@Composable
fun DemoProduceState() {
    var recargar by remember { mutableIntStateOf(0) }

    // produceState = LaunchedEffect + mutableStateOf en una sola pieza.
    // Ideal para exponer una fuente asíncrona como State<T>.
    val estado by produceState(initialValue = "cargando…", recargar) {
        value = "cargando…"
        delay(800)
        value = "✔ dato recibido (carga nº ${recargar + 1})"
        // awaitDispose { } si tuvieras que liberar algo al salir
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("4. produceState", style = MaterialTheme.typography.titleMedium)
            Text(estado, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { recargar++ }) { Text("Recargar") }
            Text(
                "produceState es azúcar de LaunchedEffect + estado. Se cancela solo al " +
                        "salir de la composición y se reinicia al cambiar sus claves.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaListasAvanzadas() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Listas avanzadas y estado", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cabeceras fijas, animaciones, scroll infinito y estado que sobrevive " +
                        "a la muerte del proceso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoListaCompleta() }
        item { DemoPaginacion() }
        item { DemoSaver() }
        item { DemoProduceState() }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de listas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("key = { it.id } SIEMPRE: sin ella no hay animación ni reutilización")
                    BulletPoint("contentType cuando mezclas cabeceras y filas")
                    BulletPoint("animateItem() para inserción, borrado y reordenación")
                    BulletPoint("stickyHeader para índices alfabéticos y secciones")
                    BulletPoint("Scroll infinito: derivedStateOf sobre layoutInfo + LaunchedEffect")
                    BulletPoint("remember muere al rotar · rememberSaveable sobrevive")
                    BulletPoint("Saver propio con listSaver, o @Parcelize en la data class")
                    BulletPoint("produceState = LaunchedEffect + estado, para fuentes asíncronas")
                    BulletPoint("Nunca guardes listas grandes en el Bundle: usa Room")
                }
            }
        }
    }
}
