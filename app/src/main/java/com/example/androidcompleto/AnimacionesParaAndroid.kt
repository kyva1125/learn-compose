package com.example.androidcompleto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE ANIMACIONES EN COMPOSE
 * Compose anima de forma DECLARATIVA: tú no dices "muévete de A a B",
 * dices "el estado cambió" y Compose interpola los valores por ti.
 *
 * LAS 4 HERRAMIENTAS ESENCIALES:
 * 1. animate*AsState  -> anima UN valor cuando cambia el estado (color, tamaño, rotación)
 * 2. AnimatedVisibility -> anima la ENTRADA/SALIDA de un elemento
 * 3. animateContentSize -> anima el cambio de TAMAÑO de un contenedor
 * 4. rememberInfiniteTransition -> animaciones que se repiten para siempre (loading)
 */

@Composable
fun PantallaAnimaciones() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Animaciones", style = MaterialTheme.typography.headlineSmall)

        DemoAnimateAsState()
        DemoAnimatedVisibility()
        DemoAnimateContentSize()
        DemoInfinita()
    }
}

// --- 1. animate*AsState: color, tamaño y rotación en un solo click ---
@Composable
fun DemoAnimateAsState() {
    var activado by remember { mutableStateOf(false) }

    // Cuando "activado" cambia, estos 3 valores se interpolan SOLOS:
    val color by animateColorAsState(
        targetValue = if (activado) Color(0xFF4CAF50) else Color(0xFF9C27B0),
        animationSpec = tween(durationMillis = 600), // duración y curva
        label = "color"
    )
    val tamano by animateDpAsState(
        targetValue = if (activado) 120.dp else 60.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), // física de resorte
        label = "tamano"
    )
    val rotacion by animateFloatAsState(
        targetValue = if (activado) 360f else 0f,
        animationSpec = tween(800),
        label = "rotacion"
    )

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("1. animate*AsState", style = MaterialTheme.typography.titleMedium)
            Text("Color + tamaño (spring) + rotación a la vez", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(tamano)
                        .rotate(rotacion)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                )
                Spacer(Modifier.width(16.dp))
                Button(onClick = { activado = !activado }) {
                    Text(if (activado) "Revertir" else "Animar")
                }
            }
        }
    }
}

// --- 2. AnimatedVisibility: aparecer/desaparecer con estilo ---
@Composable
fun DemoAnimatedVisibility() {
    var visible by remember { mutableStateOf(true) }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("2. AnimatedVisibility", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "Ocultar" else "Mostrar")
            }
            // enter y exit combinan efectos con el operador +
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                Text(
                    "¡Aparezco con fade + slide y me voy igual!",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
    }
}

// --- 3. animateContentSize: el contenedor crece/encoge suavemente ---
@Composable
fun DemoAnimateContentSize() {
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Este modifier es MAGIA: cualquier cambio de tamaño se anima solo
            .animateContentSize(animationSpec = tween(400))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("3. animateContentSize", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { expandido = !expandido }) {
                Text(if (expandido) "Ver menos ▲" else "Ver más ▼")
            }
            if (expandido) {
                Text(
                    "Este es el patrón del 'card expandible' que ves en todas las apps. " +
                            "No animamos nada a mano: el modifier animateContentSize detecta " +
                            "que el contenido creció y anima la transición. Úsalo en FAQs, " +
                            "descripciones de productos, comentarios largos, etc."
                )
            }
        }
    }
}

// --- 4. rememberInfiniteTransition: loops infinitos (loaders, pulsos) ---
@Composable
fun DemoInfinita() {
    val transicion = rememberInfiniteTransition(label = "pulso")
    // Anima de 0.6 a 1.0 y vuelve, para siempre (RepeatMode.Reverse)
    val escala by transicion.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("4. InfiniteTransition (pulso)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp * escala)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
