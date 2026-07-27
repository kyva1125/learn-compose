package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * ACCESIBILIDAD (A11Y) — PISTA COMPOSE (Lección C8)
 *
 * Dos razones para que te importe, una ética y otra egoísta:
 *   1. Hay millones de personas usando TalkBack. Tu app o funciona o no.
 *   2. El árbol de SEMANTICS que usa TalkBack es EXACTAMENTE el mismo que
 *      usan los tests de UI. App accesible = app testeable. Van juntas.
 *
 * QUÉ ES EL ÁRBOL DE SEMANTICS:
 * Compose mantiene, en paralelo al árbol de UI, un árbol que describe el
 * SIGNIFICADO de cada elemento: "esto es un botón", "dice Guardar", "está
 * deshabilitado". TalkBack lo lee en voz alta; los tests lo consultan.
 *
 * LAS 4 REGLAS QUE CUBREN EL 90%:
 *   1. Todo icono o imagen con información → contentDescription.
 *      Puramente decorativo → contentDescription = null (para que lo IGNORE).
 *   2. Objetivo táctil mínimo de 48x48 dp (Material lo exige).
 *   3. Contraste de texto mínimo 4.5:1 (3:1 para texto grande).
 *   4. Una fila que es UNA cosa → mergeDescendants: que se lea de un tirón,
 *      no campo por campo.
 *
 * PRUÉBALO DE VERDAD: Ajustes → Accesibilidad → TalkBack, y navega esta
 * pantalla deslizando a derecha e izquierda.
 */

// ============================================================
// --- 1. contentDescription: cuándo poner texto y cuándo null ---
// ============================================================
@Composable
fun DemoContentDescription() {
    var favorito by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1. contentDescription", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // ✖ MAL: sin descripción, TalkBack dice solo "botón"
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
                Text("✖ Icono sin descripción → \"botón\" a secas",
                    style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // ✔ BIEN: describe la ACCIÓN, no el dibujo
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
                }
                Text("✔ \"Eliminar tarea\" → describe la ACCIÓN",
                    style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // ✔ El estado cambia → la descripción también
                IconButton(onClick = { favorito = !favorito }) {
                    Icon(
                        if (favorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (favorito) "Quitar de favoritos"
                        else "Añadir a favoritos"
                    )
                }
                Text("✔ Descripción que sigue al estado",
                    style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "Regla: si el icono acompaña a un texto que ya lo explica, pon null " +
                        "(decorativo). Si no, describe qué PASA al pulsarlo — nunca digas " +
                        "\"icono de papelera\", di \"Eliminar tarea\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 2. mergeDescendants: leer la fila como una sola cosa ---
// ============================================================
@Composable
fun DemoMerge() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. Agrupar con mergeDescendants",
                style = MaterialTheme.typography.titleMedium)

            // ✖ SIN merge: TalkBack para 3 veces (nombre, correo, rol)
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("✖ Sin merge (3 paradas)", style = MaterialTheme.typography.labelSmall)
                    Text("José Rodríguez", style = MaterialTheme.typography.titleSmall)
                    Text("jose@ejemplo.com", style = MaterialTheme.typography.bodySmall)
                    Text("Administrador", style = MaterialTheme.typography.bodySmall)
                }
            }

            // ✔ CON merge: una sola parada, se lee todo seguido
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("✔ Con merge (1 parada)", style = MaterialTheme.typography.labelSmall)
                    Text("Ana Quispe", style = MaterialTheme.typography.titleSmall)
                    Text("ana@ejemplo.com", style = MaterialTheme.typography.bodySmall)
                    Text("Editora", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "Modifier.clickable YA hace merge automático. Solo lo necesitas a mano " +
                        "en tarjetas informativas que no son pulsables.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 3. Objetivo táctil mínimo de 48dp ---
// ============================================================
@Composable
fun DemoTamanoTactil() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. Objetivo táctil de 48 dp", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(24.dp)   // ✖ demasiado pequeño para un dedo
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .clickable { }
                    )
                    Text("✖ 24dp", style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            // El icono se ve de 24dp pero el área táctil es de 48dp
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                    Text("✔ 48dp", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                "El área táctil puede ser mayor que el dibujo. IconButton ya mide 48dp " +
                        "por dentro — otra razón para usar los componentes de Material.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 4. Semantics para componentes propios ---
// ============================================================
/**
 * Un control deslizante casero. Sin semantics, TalkBack no sabría
 * ni qué es ni cómo cambiarlo. Con ellas, se anuncia y es operable.
 */
@Composable
fun ValoracionAccesible(
    valor: Int,
    onValorCambia: (Int) -> Unit,
    maximo: Int = 5
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                // Le decimos a TalkBack QUÉ es y CUÁNTO vale
                contentDescription = "Valoración: $valor de $maximo estrellas"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = valor.toFloat(),
                    range = 0f..maximo.toFloat(),
                    steps = maximo - 1
                )
                // Y le damos acciones alternativas al gesto táctil
                setProgress { objetivo ->
                    onValorCambia(objetivo.roundToInt().coerceIn(0, maximo))
                    true
                }
            }
    ) {
        (1..maximo).forEach { i ->
            Box(
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(
                        // Los hijos ya no necesitan foco propio: el padre lo maneja
                        onClickLabel = "Puntuar con $i"
                    ) { onValorCambia(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(if (i <= valor) "★" else "☆",
                    style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
fun DemoSemanticsPropias() {
    var valoracion by remember { mutableIntStateOf(3) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("4. Semantics en componentes propios",
                style = MaterialTheme.typography.titleMedium)
            ValoracionAccesible(valor = valoracion, onValorCambia = { valoracion = it })
            Text(
                "Con TalkBack esto se anuncia como \"Valoración: $valoracion de 5 estrellas\" " +
                        "y se puede cambiar con los controles del lector, sin tocar la estrella exacta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 5. Estados: disabled, selected, heading ---
// ============================================================
@Composable
fun DemoEstadosSemanticos() {
    var seleccionado by remember { mutableStateOf("Uno") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "5. Estados y encabezados",
                style = MaterialTheme.typography.titleMedium,
                // heading() permite a TalkBack saltar de sección en sección
                modifier = Modifier.semantics { heading() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Uno", "Dos").forEach { opcion ->
                    FilterChip(
                        selected = seleccionado == opcion,
                        onClick = { seleccionado = opcion },
                        label = { Text(opcion) }
                        // FilterChip ya anuncia "seleccionado" solo ✔
                    )
                }
            }

            Button(onClick = { }, enabled = false) { Text("Deshabilitado") }
            Text(
                "Los componentes de Material ya traen sus estados semánticos. Si haces " +
                        "el tuyo, añade selectable(), toggleable() o disabled() a mano.",
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
fun PantallaAccesibilidad() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Accesibilidad y semantics", style = MaterialTheme.typography.headlineSmall)
            Text(
                "El mismo árbol que lee TalkBack es el que consultan tus tests de UI. " +
                        "Hacer la app accesible la hace testeable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoContentDescription() }
        item { DemoMerge() }
        item { DemoTamanoTactil() }
        item { DemoSemanticsPropias() }
        item { DemoEstadosSemanticos() }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de accesibilidad", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("contentDescription describe la ACCIÓN, no el dibujo")
                    BulletPoint("Decorativo → contentDescription = null (para ignorarlo)")
                    BulletPoint("Objetivo táctil ≥ 48x48dp (usa sizeIn, no agrandes el icono)")
                    BulletPoint("Tarjeta informativa → semantics(mergeDescendants = true)")
                    BulletPoint("Títulos de sección → Modifier.semantics { heading() }")
                    BulletPoint("Gesto personalizado → añade siempre onClick/setProgress semántico")
                    BulletPoint("Nunca uses SOLO el color para informar (daltonismo)")
                    BulletPoint("Respeta el tamaño de fuente del sistema: usa sp, no dp, en texto")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Herramientas: Accessibility Scanner (Play Store), Layout Inspector " +
                                "para ver el árbol de semantics, y TalkBack en un dispositivo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
