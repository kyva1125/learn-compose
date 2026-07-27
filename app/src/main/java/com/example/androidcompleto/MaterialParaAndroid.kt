package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * MATERIAL 3 A FONDO — PISTA COMPOSE (Lección C15)
 *
 * Hasta ahora usaste Card, Button y Text sin mirar debajo. Esta lección
 * cubre el sistema de diseño completo: cómo funciona el color, qué
 * componentes existen, y cómo se dibuja bajo las barras del sistema.
 *
 * EL SISTEMA DE COLOR DE M3 (lo que hace que todo combine solo):
 *   No eliges "azul". Eliges ROLES. Cada rol tiene su pareja "on-":
 *     primary   / onPrimary      → acción principal
 *     secondary / onSecondary    → acciones de apoyo
 *     tertiary  / onTertiary     → acentos, contraste
 *     error     / onError        → estados de error
 *     surface   / onSurface      → fondos de tarjetas y hojas
 *   REGLA: si pintas un fondo con X, el texto encima va con onX. Eso te
 *   garantiza el contraste en claro Y en oscuro, sin pensarlo.
 *
 * DYNAMIC COLOR (Android 12+): el sistema genera la paleta desde el
 * fondo de pantalla del usuario. Tu app se integra con SU teléfono.
 * Ya está activado en el Theme.kt de este proyecto.
 *
 * EDGE-TO-EDGE: desde Android 15 es OBLIGATORIO — tu app dibuja bajo la
 * barra de estado y la de navegación. Si no gestionas los INSETS, el
 * contenido queda tapado. Scaffold te da el padding necesario.
 */

// ============================================================
// --- 1. LOS ROLES DE COLOR ---
// ============================================================
@Composable
fun DemoRolesColor() {
    val esquema = MaterialTheme.colorScheme
    val roles = listOf(
        Triple("primary", esquema.primary, esquema.onPrimary),
        Triple("primaryContainer", esquema.primaryContainer, esquema.onPrimaryContainer),
        Triple("secondary", esquema.secondary, esquema.onSecondary),
        Triple("tertiary", esquema.tertiary, esquema.onTertiary),
        Triple("error", esquema.error, esquema.onError),
        Triple("surfaceVariant", esquema.surfaceVariant, esquema.onSurfaceVariant),
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Roles de color, no colores", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cada fondo tiene su color 'on-' de texto garantizado. Activa el modo " +
                        "oscuro del sistema: todos se recalculan solos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            roles.forEach { (nombre, fondo, texto) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(fondo, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "$nombre / on$nombre",
                        color = texto,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Text(
                "En Android 12+ estos colores salen del FONDO DE PANTALLA del usuario " +
                        "(dynamic color). Cámbialo y vuelve: la app se habrá repintado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 2. LA ESCALA TIPOGRÁFICA ---
// ============================================================
@Composable
fun DemoTipografia() {
    val tipos = listOf(
        "displaySmall" to MaterialTheme.typography.displaySmall,
        "headlineMedium" to MaterialTheme.typography.headlineMedium,
        "titleLarge" to MaterialTheme.typography.titleLarge,
        "bodyLarge" to MaterialTheme.typography.bodyLarge,
        "bodySmall" to MaterialTheme.typography.bodySmall,
        "labelSmall" to MaterialTheme.typography.labelSmall,
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("2. Escala tipográfica", style = MaterialTheme.typography.titleMedium)
            Text(
                "15 estilos con nombre. Usa el ROL, no un tamaño en sp suelto: así " +
                        "respetas el tamaño de fuente que el usuario eligió en el sistema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            tipos.forEach { (nombre, estilo) ->
                Text(nombre, style = estilo, maxLines = 1)
            }
        }
    }
}

// ============================================================
// --- 3. DIÁLOGOS, HOJAS Y SNACKBARS ---
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoComponentesM3() {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarHoja by remember { mutableStateOf(false) }
    val estadoHoja = rememberModalBottomSheetState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var resultado by remember { mutableStateOf("—") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("3. Diálogos, hojas y snackbars",
                style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { mostrarDialogo = true }) { Text("AlertDialog") }
                OutlinedButton(onClick = { mostrarHoja = true }) { Text("BottomSheet") }
            }

            OutlinedButton(onClick = {
                scope.launch {
                    // showSnackbar SUSPENDE hasta que se cierra: por eso devuelve resultado
                    val r = snackbarHost.showSnackbar(
                        message = "Elemento eliminado",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    resultado = when (r) {
                        SnackbarResult.ActionPerformed -> "Pulsaste Deshacer ↩"
                        SnackbarResult.Dismissed -> "Se cerró sin acción"
                    }
                }
            }) { Text("Snackbar con acción") }

            Text("Última interacción: $resultado", style = MaterialTheme.typography.labelLarge)

            // El host tiene que estar en el árbol para que se vea
            SnackbarHost(snackbarHost)
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },   // tocar fuera / botón atrás
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            title = { Text("¿Eliminar la tarea?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    resultado = "Confirmaste el diálogo"
                    mostrarDialogo = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarHoja) {
        ModalBottomSheet(
            onDismissRequest = { mostrarHoja = false },
            sheetState = estadoHoja
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hoja inferior modal", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Se arrastra, se cierra deslizando y respeta los insets del sistema " +
                            "sin que hagas nada.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    scope.launch { estadoHoja.hide() }.invokeOnCompletion {
                        if (!estadoHoja.isVisible) mostrarHoja = false
                    }
                }) { Text("Cerrar con animación") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================
// --- 4. CHIPS, BADGES E INDICADORES ---
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoChipsYBadges() {
    var filtros by remember { mutableStateOf(setOf("Kotlin")) }
    var notificaciones by remember { mutableIntStateOf(3) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("4. Chips, badges e indicadores",
                style = MaterialTheme.typography.titleMedium)

            Text("FilterChip (selección múltiple)",
                style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Kotlin", "Compose", "KMP").forEach { etiqueta ->
                    FilterChip(
                        selected = etiqueta in filtros,
                        onClick = {
                            filtros = if (etiqueta in filtros) filtros - etiqueta
                            else filtros + etiqueta
                        },
                        label = { Text(etiqueta) }
                    )
                }
            }

            Text("AssistChip y SuggestionChip",
                style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = { notificaciones++ },
                    label = { Text("Añadir") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
                SuggestionChip(onClick = { notificaciones = 0 }, label = { Text("Limpiar") })
            }

            Text("BadgedBox", style = MaterialTheme.typography.labelLarge)
            BadgedBox(
                badge = {
                    if (notificaciones > 0) {
                        Badge { Text("$notificaciones") }
                    }
                }
            ) {
                Icon(Icons.Default.Home, contentDescription = "Inicio")
            }

            Text("Indicadores de progreso", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                LinearProgressIndicator(Modifier.weight(1f))
            }
            Text(
                "Sin argumento 'progress' son INDETERMINADOS (giran sin fin). " +
                        "Con progress = { 0.7f } muestran un valor concreto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 5. INSETS Y EDGE-TO-EDGE ---
// ============================================================
@Composable
fun DemoInsets() {
    val barras = WindowInsets.systemBars.asPaddingValues()
    val ime = WindowInsets.ime.asPaddingValues()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("5. Insets y edge-to-edge", style = MaterialTheme.typography.titleMedium)
            Text(
                "Desde Android 15 tu app dibuja SIEMPRE bajo las barras del sistema. " +
                        "Los insets te dicen cuánto espacio ocupan para que nada quede tapado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Medidas reales de ESTE dispositivo:",
                        style = MaterialTheme.typography.labelLarge)
                    Text("Barra de estado (arriba): ${barras.calculateTopPadding()}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Barra de navegación (abajo): ${barras.calculateBottomPadding()}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Teclado (IME): ${ime.calculateBottomPadding()}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            BloqueCodigo(
                "Las 3 piezas del edge-to-edge",
                """
                // 1) En la Activity, antes de setContent:
                enableEdgeToEdge()

                // 2) Scaffold te da el padding ya calculado:
                Scaffold { padding ->
                    Column(Modifier.padding(padding)) { ... }
                }

                // 3) O a mano, con el modifier que necesites:
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                Modifier.imePadding()          // sube con el teclado
                Modifier.navigationBarsPadding()
                """.trimIndent()
            )

            Text(
                "Error clásico: una LazyColumn a pantalla completa NO debe llevar " +
                        "padding de insets en el Modifier (cortaría el scroll). Pásalos en " +
                        "contentPadding para que el contenido pase por debajo al hacer scroll.",
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
fun PantallaMaterial() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Material 3 y theming", style = MaterialTheme.typography.headlineSmall)
            Text(
                "El sistema de diseño completo: color por roles, tipografía, componentes " +
                        "y cómo convivir con las barras del sistema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoRolesColor() }
        item { DemoTipografia() }
        item { DemoComponentesM3() }
        item { DemoChipsYBadges() }
        item { DemoInsets() }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de Material 3", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Fondo X → texto onX. Nunca elijas colores a ojo")
                    BulletPoint("Usa roles de tipografía, no sp sueltos (respeta accesibilidad)")
                    BulletPoint("Dynamic color (Android 12+) toma la paleta del fondo de pantalla")
                    BulletPoint("Prueba SIEMPRE en claro y en oscuro antes de dar por hecha una pantalla")
                    BulletPoint("showSnackbar suspende y devuelve si se pulsó la acción")
                    BulletPoint("ModalBottomSheet: hide() es suspend, luego ocultas el estado")
                    BulletPoint("Edge-to-edge obligatorio en Android 15: enableEdgeToEdge + Scaffold")
                    BulletPoint("En listas, los insets van en contentPadding, no en Modifier.padding")
                }
            }
        }
    }
}
