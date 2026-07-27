package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * GUÍA MAESTRA DE RENDIMIENTO EN COMPOSE (Lección 14)
 *
 * Un senior no escribe UI "que funciona": escribe UI que NO recompone de más.
 *
 * LAS 3 FASES DE UN FRAME:
 *   1. COMPOSICIÓN: qué UI mostrar (ejecutar tus @Composable)
 *   2. LAYOUT: dónde va cada cosa (medir y posicionar)
 *   3. DIBUJO: pintar píxeles en pantalla
 * La optimización #1 es SALTARSE fases: si algo solo se mueve,
 * no hace falta recomponerlo (fase 1), solo re-posicionarlo (fase 2).
 *
 * SMART SKIPPING: Compose se salta un composable si sus parámetros
 * no cambiaron. Para eso los parámetros deben ser ESTABLES:
 *   - Primitivos, String y lambdas → estables ✔
 *   - data class con vals de tipos estables → estable ✔
 *   - List/Map/Set (interfaces) → INESTABLES ✖ (usa la misma instancia,
 *     kotlinx.collections.immutable, o anota tu clase con @Immutable)
 *
 * Desde Kotlin 2.0 el compilador activa STRONG SKIPPING: incluso los
 * parámetros inestables se comparan por INSTANCIA (===). Traducción:
 * si creas una lista nueva en cada recomposición, el hijo recompone;
 * si la recuerdas con remember, se la salta.
 *
 * CÓMO MEDIR (nunca optimices a ciegas):
 *   - Layout Inspector → "Recomposition counts"
 *   - Siempre en build RELEASE con R8: debug es 10x más lento
 *   - Macrobenchmark + Baseline Profiles para el arranque
 */

// Truco de demo: contador que NO es estado (no dispara recomposiciones),
// solo cuenta cuántas veces se ejecutó el cuerpo del composable.
class ContadorRef { var valor = 0 }

@Composable
fun EtiquetaRecomposicion(nombre: String, colorFondo: androidx.compose.ui.graphics.Color) {
    val ref = remember { ContadorRef() }
    ref.valor++ // se ejecuta en cada recomposición de QUIEN LO CONTIENE
    Text(
        "$nombre → recompuesto ${ref.valor} ${if (ref.valor == 1) "vez" else "veces"}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .background(colorFondo, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ============================================================
// --- DEMO 1: SKIPPING — ¿qué hijos recomponen? ---
// ============================================================
@Composable
fun HijoParametroEstable(texto: String) {
    // String es estable y "texto" nunca cambia → Compose SE LO SALTA
    // aunque el padre recomponga mil veces.
    val ref = remember { ContadorRef() }
    ref.valor++
    Text(
        "Hijo estable (\"$texto\") → ${ref.valor}x",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun HijoConLista(etiqueta: String, lista: List<String>) {
    // List<String> es INESTABLE. Con strong skipping se compara por
    // instancia: misma instancia → skip; instancia nueva → recompone.
    val ref = remember { ContadorRef() }
    ref.valor++
    Text(
        "$etiqueta (${lista.size} items) → ${ref.valor}x",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun DemoSkipping() {
    var clicks by remember { mutableIntStateOf(0) }

    // ✖ MAL: instancia NUEVA en cada recomposición del padre
    val listaInline = listOf("A", "B", "C")

    // ✔ BIEN: la MISMA instancia sobrevive entre recomposiciones
    val listaRecordada = remember { listOf("A", "B", "C") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Smart skipping en vivo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pulsa el botón: el padre recompone, pero mira QUIÉN lo acompaña y quién se lo salta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Button(onClick = { clicks++ }) { Text("Recomponer padre (clicks: $clicks)") }

            EtiquetaRecomposicion("PADRE", MaterialTheme.colorScheme.surfaceVariant)
            HijoParametroEstable("hola")
            HijoConLista("✖ Hijo con lista creada inline", listaInline)
            HijoConLista("✔ Hijo con lista en remember", listaRecordada)

            Text(
                "Lección: los objetos que pasas como parámetro deben venir de remember, " +
                        "de un ViewModel o ser constantes. Crearlos en el cuerpo = recomposición segura.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ============================================================
// --- DEMO 2: derivedStateOf — recomponer solo cuando IMPORTA ---
// ============================================================
@Composable
fun DemoDerivadoScroll() {
    val estadoLista = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // firstVisibleItemIndex cambia en CADA píxel de scroll.
    // Sin derivedStateOf recompondríamos decenas de veces por segundo.
    // Con él, solo recomponemos cuando el Boolean cruza el umbral.
    val mostrarBoton by remember {
        derivedStateOf { estadoLista.firstVisibleItemIndex > 3 }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("2. derivedStateOf con scroll", style = MaterialTheme.typography.titleMedium)
            Text(
                "El botón 'Subir' solo aparece tras pasar el elemento 3. La condición se " +
                        "evalúa en cada píxel, pero SOLO recompone cuando el resultado cambia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            EtiquetaRecomposicion("Tarjeta del scroll", MaterialTheme.colorScheme.surfaceVariant)

            LazyColumn(
                state = estadoLista,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            ) {
                items(count = 30) { i ->
                    Text("Elemento $i", Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            if (mostrarBoton) {
                OutlinedButton(onClick = {
                    scope.launch { estadoLista.animateScrollToItem(0) }
                }) { Text("⬆ Volver arriba") }
            }
        }
    }
}

// ============================================================
// --- DEMO 3: DIFERIR LECTURAS — saltarse la fase de composición ---
// ============================================================
@Composable
fun CajaQueRecompone(desplazamiento: Float) {
    // Recibe el VALOR: cada cambio del slider = parámetro nuevo = recomposición.
    Column {
        EtiquetaRecomposicion("✖ Caja que lee el valor", MaterialTheme.colorScheme.errorContainer)
        Box(
            Modifier
                .offset(x = desplazamiento.dp) // lectura en fase de COMPOSICIÓN
                .size(28.dp)
                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(6.dp))
        )
    }
}

@Composable
fun CajaDiferida(desplazamiento: () -> Float) {
    // Recibe una LAMBDA: la composición no lee el estado.
    // El offset {} lo lee en fase de LAYOUT → cero recomposiciones al mover.
    Column {
        EtiquetaRecomposicion("✔ Caja con lectura diferida", MaterialTheme.colorScheme.primaryContainer)
        Box(
            Modifier
                .offset { IntOffset(desplazamiento().dp.roundToPx(), 0) }
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
        )
    }
}

@Composable
fun DemoLecturaDiferida() {
    var despl by remember { mutableFloatStateOf(0f) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("3. Diferir lecturas de estado", style = MaterialTheme.typography.titleMedium)
            Text(
                "Mueve el slider: ambas cajas se desplazan igual, pero la roja recompone " +
                        "en cada cambio y la verde/primaria NUNCA (solo re-layout).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Slider(
                value = despl,
                onValueChange = { despl = it },
                valueRange = 0f..200f
            )
            CajaQueRecompone(despl)
            Spacer(Modifier.height(4.dp))
            CajaDiferida { despl }
            Text(
                "Mismo truco con Modifier.graphicsLayer { } para alpha, scale y rotation: " +
                        "las animaciones nunca deberían recomponer.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ============================================================
// --- CHULETA FINAL ---
// ============================================================
@Composable
fun TarjetaChuletaRendimiento() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("📌 Chuleta de rendimiento", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BulletPoint("Estado lo más ABAJO posible (state hoisting con cabeza)")
            BulletPoint("Listas Lazy: siempre key = { it.id } y contentType si mezclas tipos")
            BulletPoint("Objetos caros → remember; formatos/regex → fuera del composable")
            BulletPoint("Cálculo que cambia menos que su fuente → derivedStateOf")
            BulletPoint("Animaciones → lambdas: offset { }, graphicsLayer { }")
            BulletPoint("Clases de dominio en el UI state → @Immutable o ImmutableList")
            BulletPoint("Mide con Layout Inspector y SIEMPRE en release + R8")
            Spacer(Modifier.height(4.dp))
            Text(
                "Regla de oro: primero código claro, después medir, y solo al final " +
                        "optimizar lo que el perfilador señale.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- PANTALLA PRINCIPAL DE LA LECCIÓN ---
// ============================================================
@Composable
fun PantallaRendimiento() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Rendimiento y recomposición", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cada tarjeta muestra contadores de recomposición EN VIVO. " +
                        "Interactúa y observa quién recompone y quién no.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoSkipping() }
        item { DemoDerivadoScroll() }
        item { DemoLecturaDiferida() }
        item { TarjetaChuletaRendimiento() }
    }
}
