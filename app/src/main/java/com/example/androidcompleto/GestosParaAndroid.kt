package com.example.androidcompleto

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * GESTOS Y ENTRADA TÁCTIL — PISTA COMPOSE (Lección C7)
 *
 * clickable{} resuelve el 80%. El otro 20% (arrastrar, deslizar para
 * borrar, pellizcar para hacer zoom, scroll anidado) se hace con
 * pointerInput, y es donde se atasca casi todo el mundo.
 *
 * LOS DOS NIVELES:
 *
 *   ALTO NIVEL (usa estos siempre que puedas):
 *     Modifier.clickable / combinedClickable (long press, doble tap)
 *     Modifier.draggable  → arrastre en UN eje
 *     Modifier.transformable → zoom + rotación + pan
 *     Modifier.scrollable / verticalScroll
 *
 *   BAJO NIVEL (pointerInput): cuando necesitas control total
 *     detectTapGestures, detectDragGestures, detectTransformGestures
 *     awaitPointerEventScope { awaitPointerEvent() }  ← el metal desnudo
 *
 * CLAVE: pointerInput(key) se reinicia cuando cambia la key, igual que
 * LaunchedEffect. Si capturas estado dentro, usa rememberUpdatedState
 * o pasa la key correcta, o te quedarás con valores viejos.
 */

// ============================================================
// --- 1. detectDragGestures: arrastrar libremente ---
// ============================================================
@Composable
fun DemoArrastreLibre() {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Arrastre libre (detectDragGestures)",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Arrastra el círculo por la caja. offset { } lee la posición en fase de " +
                        "LAYOUT: mover no recompone (lección de rendimiento).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            ) {
                Box(
                    Modifier
                        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { /* aquí guardarías la posición final */ }
                            ) { change, arrastre ->
                                change.consume()        // "yo me encargo de este evento"
                                offset += arrastre
                            }
                        }
                )
            }
            OutlinedButton(onClick = { offset = Offset.Zero }) { Text("Recentrar") }
        }
    }
}

// ============================================================
// --- 2. Swipe-to-dismiss hecho a mano (con Animatable) ---
// ============================================================
@Composable
fun DemoDeslizarParaBorrar() {
    var items by remember { mutableStateOf((1..4).map { "Tarea $it" }) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("2. Deslizar para borrar", style = MaterialTheme.typography.titleMedium)
            Text(
                "Desliza una fila a la izquierda. Si pasa el umbral se borra; si no, " +
                        "vuelve con animación (animateTo). Esto es lo que hace SwipeToDismissBox.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            if (items.isEmpty()) {
                Text("Todo borrado 🎉")
                OutlinedButton(onClick = { items = (1..4).map { "Tarea $it" } }) {
                    Text("Restaurar")
                }
            }

            items.forEach { item ->
                key(item) {
                    FilaDeslizable(
                        texto = item,
                        onBorrar = { items = items - item }
                    )
                }
            }
        }
    }
}

@Composable
fun FilaDeslizable(texto: String, onBorrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val umbralPx = with(LocalDensity.current) { 120.dp.toPx() }

    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
    ) {
        // Fondo que asoma al deslizar
        Row(
            Modifier.matchParentSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗑 Soltar para borrar", style = MaterialTheme.typography.labelMedium)
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            // Solo permitimos arrastrar hacia la izquierda
                            val nuevo = (offsetX.value + delta).coerceAtMost(0f)
                            offsetX.snapTo(nuevo)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (abs(offsetX.value) > umbralPx) {
                                offsetX.animateTo(-2_000f)   // sale de pantalla
                                onBorrar()
                            } else {
                                offsetX.animateTo(0f)        // vuelve a su sitio
                            }
                        }
                    }
                )
        ) {
            Text(texto, Modifier.padding(16.dp))
        }
    }
}

// ============================================================
// --- 3. transformable: pellizcar para zoom + rotar ---
// ============================================================
@Composable
fun DemoZoomYRotacion() {
    var escala by remember { mutableFloatStateOf(1f) }
    var rotacion by remember { mutableFloatStateOf(0f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    val estado = rememberTransformableState { zoomCambio, panCambio, rotacionCambio ->
        escala = (escala * zoomCambio).coerceIn(0.5f, 4f)
        rotacion += rotacionCambio
        desplazamiento += panCambio
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("3. Zoom, rotación y pan (transformable)",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Pellizca con dos dedos sobre el cuadro (en emulador: Ctrl + arrastrar). " +
                        "graphicsLayer aplica las transformaciones SIN recomponer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            rotationZ = rotacion
                            translationX = desplazamiento.x
                            translationY = desplazamiento.y
                        }
                        .transformable(state = estado)
                        .size(90.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔍", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Text(
                "escala %.2f · rotación %.0f°".format(escala, rotacion),
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedButton(onClick = {
                escala = 1f; rotacion = 0f; desplazamiento = Offset.Zero
            }) { Text("Reiniciar") }
        }
    }
}

// ============================================================
// --- 4. detectTapGestures: tap, doble tap, long press ---
// ============================================================
@Composable
fun DemoTaps() {
    var ultimo by remember { mutableStateOf("aún nada") }
    var contador by remember { mutableIntStateOf(0) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("4. Tap, doble tap y pulsación larga",
                style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { pos ->
                                contador++
                                ultimo = "tap en (%.0f, %.0f)".format(pos.x, pos.y)
                            },
                            onDoubleTap = { contador++; ultimo = "¡doble tap!" },
                            onLongPress = { contador++; ultimo = "pulsación larga 🕐" },
                            onPress = { /* se dispara al bajar el dedo */ }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Tócame de varias formas", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Último gesto ($contador): $ultimo",
                style = MaterialTheme.typography.labelLarge)
            Text(
                "Para un simple click usa Modifier.clickable: trae ripple, foco y " +
                        "accesibilidad gratis. pointerInput NO los trae — tendrías que añadirlos.",
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
fun PantallaGestos() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Gestos y entrada táctil", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Todo es interactivo: arrastra, desliza, pellizca y toca.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoArrastreLibre() }
        item { DemoDeslizarParaBorrar() }
        item { DemoZoomYRotacion() }
        item { DemoTaps() }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de gestos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Empieza siempre por lo alto: clickable, draggable, transformable")
                    BulletPoint("change.consume() = 'este evento es mío', evita que lo use el padre")
                    BulletPoint("pointerInput(key): cambia la key y el gesto se reinicia")
                    BulletPoint("Animatable + snapTo/animateTo = arrastre con retorno elástico")
                    BulletPoint("Mueve con offset{} o graphicsLayer{}, nunca recomponiendo")
                    BulletPoint("Conflictos de scroll → NestedScrollConnection")
                    BulletPoint("Material3 ya trae SwipeToDismissBox y AnchoredDraggable listos")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Accesibilidad: un gesto personalizado es invisible para TalkBack. " +
                                "Añade siempre una alternativa con semantics { onClick(...) } " +
                                "— lo ves en la lección de accesibilidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
