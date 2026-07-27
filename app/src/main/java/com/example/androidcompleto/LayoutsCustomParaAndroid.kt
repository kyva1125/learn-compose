package com.example.androidcompleto

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE LAYOUTS CUSTOM Y CANVAS (Lección 15)
 *
 * Column, Row y Box resuelven el 90% de las pantallas. El otro 10%
 * (chips que fluyen, gráficas, componentes de diseño exacto) separa
 * a un mid de un senior. Hay 3 niveles de poder:
 *
 *   1. Modifier.layout { }  → ajustar CÓMO se mide/posiciona UN elemento
 *   2. Layout(...)          → inventar tu PROPIO Column/Row (mides y
 *                             colocas a todos los hijos tú mismo)
 *   3. Canvas               → dibujar píxeles directamente (gráficas,
 *                             indicadores, firmas, juegos)
 *
 * CÓMO FUNCIONA EL LAYOUT EN COMPOSE (una sola pasada):
 *   - El padre pasa CONSTRAINTS (ancho/alto mín y máx) a cada hijo.
 *   - Cada hijo se mide UNA vez y devuelve su tamaño (Placeable).
 *   - El padre decide dónde colocar (place) a cada Placeable.
 * Por eso Compose es rápido: nunca hay doble medición como en Views.
 */

// ============================================================
// --- 1. Modifier.layout: paddig desde la LÍNEA BASE del texto ---
// ============================================================
// El padding normal mide desde el borde superior del Text. Los diseñadores
// piden distancias desde la LÍNEA BASE (donde se apoya la letra).
// Este es el ejemplo clásico de la documentación oficial, en español:
fun Modifier.lineaBaseArriba(distancia: Dp) = this.layout { measurable, constraints ->
    // 1. Medimos el elemento con las constraints que nos dieron
    val placeable = measurable.measure(constraints)

    // 2. Leemos dónde quedó su primera línea base
    val lineaBase = placeable[FirstBaseline]

    // 3. Lo empujamos hacia abajo para que la distancia borde→línea base
    //    sea exactamente la pedida
    val y = distancia.roundToPx() - lineaBase
    layout(placeable.width, placeable.height + y) {
        placeable.placeRelative(0, y)
    }
}

@Composable
fun DemoLineaBase() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Modifier.layout: línea base", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ambos textos están 'a 32.dp'... pero uno desde el borde y otro desde " +
                        "la línea base. Así se cumplen los specs de diseño al píxel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("padding 32.dp", Modifier.padding(top = 32.dp))
                }
                Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("línea base 32.dp", Modifier.lineaBaseArriba(32.dp))
                }
            }
        }
    }
}

// ============================================================
// --- 2. Layout(): nuestra propia "FilaFluida" (flow layout) ---
// ============================================================
// Una fila de chips que SALTA de línea cuando no cabe (como el texto).
// Ni Row ni Column saben hacer esto: hay que medir y colocar a mano.
@Composable
fun FilaFluida(
    modifier: Modifier = Modifier,
    espacio: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val espacioPx = espacio.roundToPx()

        // 1. MEDIR: cada hijo decide su tamaño (sin mínimos forzados)
        val placeables = measurables.map { medible ->
            medible.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        // 2. CALCULAR posiciones: vamos llenando filas y saltando de línea
        var x = 0
        var y = 0
        var alturaFila = 0
        val posiciones = placeables.map { placeable ->
            if (x + placeable.width > constraints.maxWidth) {
                x = 0
                y += alturaFila + espacioPx
                alturaFila = 0
            }
            val posicion = IntOffset(x, y)
            x += placeable.width + espacioPx
            alturaFila = maxOf(alturaFila, placeable.height)
            posicion
        }

        // 3. COLOCAR: declaramos nuestro tamaño y ubicamos a cada hijo
        layout(width = constraints.maxWidth, height = y + alturaFila) {
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(posiciones[i].x, posiciones[i].y)
            }
        }
    }
}

@Composable
fun DemoFilaFluida() {
    val tecnologias = remember {
        listOf(
            "Compose", "Kotlin", "Coroutines", "Flow", "Room", "Retrofit",
            "KMP", "Ktor", "SQLDelight", "Koin", "Coil", "DataStore"
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("2. Layout(): FilaFluida propia", style = MaterialTheme.typography.titleMedium)
            Text(
                "12 chips que saltan de línea solos. Medimos y colocamos cada hijo " +
                        "a mano: esto es literalmente escribir tu propio Row.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            FilaFluida(Modifier.fillMaxWidth()) {
                tecnologias.forEach { nombre ->
                    Text(
                        nombre,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// --- 3. Canvas: gráfica de barras + anillo de progreso ---
// ============================================================
@Composable
fun DemoCanvas() {
    val datos = remember { listOf(30f, 80f, 45f, 100f, 60f, 75f) }
    var progreso by remember { mutableFloatStateOf(0.72f) }
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 600),
        label = "anillo"
    )

    // Los colores del tema se leen AQUÍ (contexto composable);
    // dentro de Canvas ya solo hay un DrawScope, no composición.
    val colorBarra = MaterialTheme.colorScheme.primary
    val colorPista = MaterialTheme.colorScheme.surfaceVariant
    val colorAnillo = MaterialTheme.colorScheme.tertiary

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. Canvas: dibujo directo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sin librerías de gráficas: una barra por dato y un anillo con drawArc. " +
                        "En Canvas tú eres el motor de render.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            // --- Gráfica de barras ---
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val maxDato = datos.max()
                val anchoSlot = size.width / datos.size
                val anchoBarra = anchoSlot * 0.6f
                datos.forEachIndexed { i, valor ->
                    val alto = (valor / maxDato) * size.height
                    drawRoundRect(
                        color = colorBarra,
                        topLeft = Offset(
                            x = i * anchoSlot + (anchoSlot - anchoBarra) / 2,
                            y = size.height - alto
                        ),
                        size = Size(anchoBarra, alto),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }

            // --- Anillo de progreso ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(110.dp).padding(8.dp)) {
                        val grosor = 18f
                        drawArc(
                            color = colorPista,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = grosor, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = colorAnillo,
                            startAngle = -90f,
                            sweepAngle = 360f * progresoAnimado,
                            useCenter = false,
                            style = Stroke(width = grosor, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "${(progresoAnimado * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Progreso animado", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = progreso,
                        onValueChange = { progreso = it },
                        valueRange = 0f..1f
                    )
                }
            }
        }
    }
}

// ============================================================
// --- TEORÍA EXTRA PARA ENTREVISTAS ---
// ============================================================
@Composable
fun TarjetaTeoriaLayouts() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("📌 Para la entrevista senior", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BulletPoint("Compose mide en UNA pasada: cada hijo se mide 1 sola vez")
            BulletPoint("Constraints bajan (padre→hijo), tamaños suben (hijo→padre)")
            BulletPoint("Modifier.layout = 1 elemento; Layout() = varios hijos")
            BulletPoint("SubcomposeLayout: componer DURANTE la medición (así funciona LazyColumn)")
            BulletPoint("Intrinsics (Modifier.height(IntrinsicSize.Min)): 'pre-preguntar' tamaños")
            BulletPoint("Canvas = Modifier.drawBehind con tamaño propio; también existe drawWithCache")
            Spacer(Modifier.height(4.dp))
            Text(
                "Si en la entrevista te piden 'un Row que salte de línea', ya lo escribiste " +
                        "aquí desde cero: FilaFluida.",
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
fun PantallaLayoutsCustom() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Layouts custom y Canvas", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Los 3 niveles de poder visual: ajustar un elemento, inventar un layout " +
                        "y dibujar píxeles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoLineaBase() }
        item { DemoFilaFluida() }
        item { DemoCanvas() }
        item { TarjetaTeoriaLayouts() }
    }
}
