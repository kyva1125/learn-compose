package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * LAYOUTS ADAPTATIVOS — PISTA COMPOSE (Lección C9)
 *
 * Tu app ya no corre solo en un móvil: hay tablets, plegables, Chromebooks,
 * ventanas redimensionables en escritorio y modo multiventana. Google
 * REQUIERE soporte de pantallas grandes para destacar en Play Store.
 *
 * LA REGLA DE ORO: NUNCA decidas por "¿es una tablet?" ni por píxeles
 * exactos. Decide por WINDOW SIZE CLASS, que mide la VENTANA que te toca
 * (en multiventana, un móvil y una tablet pueden dar el mismo tamaño).
 *
 * LAS TRES CLASES DE ANCHO:
 *   COMPACT  (< 600dp)  → móvil en vertical      → 1 panel, barra inferior
 *   MEDIUM   (600-840)  → tablet vertical, plegable → 1-2 paneles, rail lateral
 *   EXPANDED (> 840dp)  → tablet horizontal, escritorio → 2 paneles, drawer fijo
 *
 * PATRONES CANÓNICOS:
 *   - List-Detail: lista sola en compact; lista + detalle lado a lado en expanded
 *   - Navegación que muta: BottomBar → NavigationRail → NavigationDrawer
 *   - Contenido con ancho máximo: el texto a pantalla completa en 1200dp es ilegible
 *
 * PRUÉBALO: gira el dispositivo o abre el modo multiventana y mira cómo
 * cambia todo en vivo.
 */

data class MensajeDemo(val id: Int, val autor: String, val asunto: String, val cuerpo: String)

val bandejaDemo = listOf(
    MensajeDemo(1, "Ana Quispe", "Revisión del PR #42",
        "Dejé tres comentarios sobre el reducer. El resto se ve muy bien, " +
                "sobre todo la separación del dominio."),
    MensajeDemo(2, "Carlos Mendoza", "Diseño de la pantalla nueva",
        "Adjunto el Figma. Ojo con los espaciados: usamos tokens de 4dp, " +
                "no valores sueltos."),
    MensajeDemo(3, "Lucía Fernández", "Métricas de la semana",
        "El arranque en frío bajó a 780ms tras aplicar el Baseline Profile. " +
                "Bien por el equipo."),
    MensajeDemo(4, "Diego Ramos", "Migración a KMP",
        "Propongo empezar por la capa de datos: Ktor y kotlinx.serialization " +
                "primero, la UI al final.")
)

// ============================================================
// --- EL PATRÓN LIST-DETAIL ADAPTATIVO ---
// ============================================================
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PantallaAdaptive() {
    // calculateWindowSizeClass necesita la Activity. Como esta pantalla vive
    // dentro del menú, lo derivamos de la configuración actual: mismo criterio
    // (dp de ancho de ventana) y funciona igual al girar o redimensionar.
    val anchoDp = LocalConfiguration.current.screenWidthDp
    val clase = when {
        anchoDp < 600 -> WindowWidthSizeClass.Compact
        anchoDp < 840 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }

    var seleccionado by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        // --- Cabecera explicativa ---
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Layouts adaptativos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ventana: ${anchoDp}dp → ${
                        when (clase) {
                            WindowWidthSizeClass.Compact -> "COMPACT (1 panel)"
                            WindowWidthSizeClass.Medium -> "MEDIUM (1 panel + rail)"
                            else -> "EXPANDED (2 paneles)"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Gira el dispositivo o abre multiventana: el layout cambia solo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (clase) {
            // --- COMPACT: un panel cada vez (lista O detalle) ---
            WindowWidthSizeClass.Compact -> {
                if (seleccionado == null) {
                    ListaMensajes(
                        seleccionado = null,
                        onSeleccionar = { seleccionado = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DetalleMensaje(
                        mensaje = bandejaDemo.first { it.id == seleccionado },
                        onVolver = { seleccionado = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // --- MEDIUM y EXPANDED: los dos paneles a la vez ---
            else -> {
                Row(Modifier.fillMaxSize()) {
                    ListaMensajes(
                        seleccionado = seleccionado,
                        onSeleccionar = { seleccionado = it },
                        // En expanded la lista ocupa menos proporción
                        modifier = Modifier.weight(if (clase == WindowWidthSizeClass.Expanded) 0.4f else 0.5f)
                    )
                    VerticalDivider()
                    Box(Modifier.weight(if (clase == WindowWidthSizeClass.Expanded) 0.6f else 0.5f)) {
                        val m = bandejaDemo.firstOrNull { it.id == seleccionado }
                        if (m == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Elige un mensaje 👈",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            DetalleMensaje(m, onVolver = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListaMensajes(
    seleccionado: Int?,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bandejaDemo, key = { it.id }) { m ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSeleccionar(m.id) },
                colors = if (m.id == seleccionado) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                } else CardDefaults.cardColors()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.autor, style = MaterialTheme.typography.titleSmall)
                    Text(m.asunto, style = MaterialTheme.typography.bodyMedium)
                    Text(m.cuerpo, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
        item { TarjetaTeoriaAdaptive() }
    }
}

@Composable
fun DetalleMensaje(
    mensaje: MensajeDemo,
    onVolver: (() -> Unit)?,   // null cuando hay dos paneles: no hace falta "atrás"
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .padding(16.dp)
            // Ancho máximo legible: en pantallas anchas el texto no debe estirarse
            .widthIn(max = 700.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onVolver != null) {
            TextButton(onClick = onVolver) { Text("← Volver a la lista") }
        }
        Text(mensaje.asunto, style = MaterialTheme.typography.headlineSmall)
        Text("De: ${mensaje.autor}", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)
        HorizontalDivider()
        Text(mensaje.cuerpo, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(12.dp)) {
                Text("📌 Lo que acabas de ver", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                BulletPoint("En compact: lista y detalle son PANTALLAS distintas")
                BulletPoint("En expanded: son PANELES a la vez, sin botón 'volver'")
                BulletPoint("El estado 'seleccionado' sobrevive al giro (rememberSaveable)")
                BulletPoint("widthIn(max = 700.dp) mantiene el texto legible")
            }
        }
    }
}

// ============================================================
// --- TEORÍA COMPLEMENTARIA ---
// ============================================================
@Composable
fun TarjetaTeoriaAdaptive() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("📌 Chuleta de adaptive", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BulletPoint("Decide por WindowSizeClass, jamás por 'esTablet' ni por píxeles")
            BulletPoint("Compact < 600dp · Medium 600-840dp · Expanded > 840dp")
            BulletPoint("Navegación: BottomBar → NavigationRail → NavigationDrawer")
            BulletPoint("NavigationSuiteScaffold hace ese cambio automáticamente")
            BulletPoint("Texto con widthIn(max) — nunca líneas de 1200dp de ancho")
            BulletPoint("Guarda el estado con rememberSaveable: girar destruye la Activity")
            BulletPoint("Librería oficial: material3-adaptive con ListDetailPaneScaffold")
            Spacer(Modifier.height(8.dp))
            BloqueCodigo(
                "En una Activity, lo canónico es calcularlo así",
                """
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            val sizeClass = calculateWindowSizeClass(this)
                            MiApp(windowSizeClass = sizeClass)
                        }
                    }
                }
                """.trimIndent()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No olvides el manifest: android:resizeableActivity=\"true\" y NO bloquear " +
                        "la orientación si quieres puntuar bien en pantallas grandes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
