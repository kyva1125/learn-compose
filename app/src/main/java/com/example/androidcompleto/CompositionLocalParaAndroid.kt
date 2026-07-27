package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE COMPOSITIONLOCAL (Lección 16)
 *
 * ¿Cómo sabe un Text() de qué color pintarse sin que NADIE le pase el
 * tema por parámetro? Porque MaterialTheme.colorScheme no es magia:
 * es un CompositionLocal — un valor que "fluye" invisible por el árbol
 * y cualquier descendiente puede leer con .current.
 *
 *   CompositionLocalProvider(LocalX provides valor) {
 *       // ...todo este subárbol lee LocalX.current y ve "valor"
 *   }
 *
 * ¿CUÁNDO USARLO? Solo para valores TRANSVERSALES de diseño o contexto
 * que casi cualquier composable podría necesitar:
 *   ✔ tema, espaciados, colores semánticos, formato de moneda/idioma
 *   ✖ el estado de tu pantalla, callbacks, ViewModels → eso va por
 *     PARÁMETROS (explícito > implícito; abusar crea dependencias ocultas)
 *
 * DOS SABORES:
 *   - compositionLocalOf: si cambia el valor, recompone SOLO a quien
 *     hizo .current (lecturas rastreadas). Para valores que cambian.
 *   - staticCompositionLocalOf: si cambia, recompone TODO el bloque
 *     del provider. Más barato de leer; para valores que casi nunca
 *     cambian (el tema, la densidad...).
 *
 * Ya los usas sin saberlo: LocalContext, LocalDensity, LocalContentColor,
 * LocalLifecycleOwner... y esta app los usa en varias lecciones.
 */

// ============================================================
// --- 1. NUESTROS DESIGN TOKENS: espaciado ---
// ============================================================
data class TokensEspaciado(
    val chico: Dp,
    val medio: Dp,
    val grande: Dp
)

val EspaciadoComodo = TokensEspaciado(chico = 6.dp, medio = 14.dp, grande = 24.dp)
val EspaciadoCompacto = TokensEspaciado(chico = 2.dp, medio = 6.dp, grande = 10.dp)

// Cambia en caliente (con un Switch) → compositionLocalOf
val LocalEspaciado = compositionLocalOf { EspaciadoComodo }

// ============================================================
// --- 2. COLORES SEMÁNTICOS (extender MaterialTheme) ---
// ============================================================
// Material3 no trae "verde éxito" ni "ámbar advertencia". El patrón
// senior: definir colores propios y proveerlos JUNTO al tema.
data class ColoresSemanticos(
    val exito: Color,
    val advertencia: Color,
    val informacion: Color
)

// Casi nunca cambia (solo con el tema) → staticCompositionLocalOf
val LocalColoresSemanticos = staticCompositionLocalOf {
    ColoresSemanticos(
        exito = Color(0xFF2E7D32),
        advertencia = Color(0xFFF9A825),
        informacion = Color(0xFF1565C0)
    )
}

@Composable
fun EtiquetaEstado(texto: String, color: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// ============================================================
// --- 3. UN COMPONENTE QUE CONSUME LOS TOKENS ---
// ============================================================
// Fíjate: NO recibe espaciados ni colores por parámetro.
// Los lee del árbol, igual que Text lee el tema.
@Composable
fun TarjetaPedido(numero: Int, estado: String) {
    val espaciado = LocalEspaciado.current
    val colores = LocalColoresSemanticos.current

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(espaciado.medio),
            verticalArrangement = Arrangement.spacedBy(espaciado.chico)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pedido #$numero", style = MaterialTheme.typography.titleSmall)
                when (estado) {
                    "Entregado" -> EtiquetaEstado("✔ $estado", colores.exito)
                    "En camino" -> EtiquetaEstado("🚚 $estado", colores.informacion)
                    else -> EtiquetaEstado("⏳ $estado", colores.advertencia)
                }
            }
            Text(
                "Los paddings de esta tarjeta salen de LocalEspaciado.current",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 4. LA DEMO: proveer y cambiar los tokens en vivo ---
// ============================================================
@Composable
fun DemoTokensEnVivo() {
    var compacto by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Design tokens en vivo", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = compacto, onCheckedChange = { compacto = it })
                Spacer(Modifier.width(8.dp))
                Text(if (compacto) "Modo compacto" else "Modo cómodo")
            }

            // TODO el subárbol de abajo ve los tokens elegidos,
            // sin pasar ni un parámetro extra.
            CompositionLocalProvider(
                LocalEspaciado provides if (compacto) EspaciadoCompacto else EspaciadoComodo
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalEspaciado.current.chico)) {
                    TarjetaPedido(1042, "Entregado")
                    TarjetaPedido(1043, "En camino")
                    TarjetaPedido(1044, "Pendiente")
                }
            }
        }
    }
}

// ============================================================
// --- 5. CÓMO LO HACE MATERIALTHEME POR DENTRO ---
// ============================================================
@Composable
fun TarjetaInternasMaterial() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("🔬 Así funciona MaterialTheme por dentro", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BloqueCodigo(
                "MaterialTheme, simplificado (código real de la librería)",
                """
                @Composable
                fun MaterialTheme(colorScheme, typography, content) {
                    CompositionLocalProvider(
                        LocalColorScheme provides colorScheme,
                        LocalTypography provides typography,
                    ) { content() }
                }

                // Y el objeto que usas a diario solo hace .current:
                object MaterialTheme {
                    val colorScheme: ColorScheme
                        @Composable get() = LocalColorScheme.current
                }
                """.trimIndent()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tu design system propio se construye IGUAL: un objeto TuTema que exponga " +
                        "espaciados, colores y formas leyendo tus CompositionLocals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun TarjetaChuletaCompositionLocal() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("📌 Chuleta CompositionLocal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BulletPoint("Leer: LocalX.current (solo dentro de @Composable)")
            BulletPoint("Proveer: CompositionLocalProvider(LocalX provides valor) { ... }")
            BulletPoint("Cambia seguido → compositionLocalOf (recompone solo lectores)")
            BulletPoint("Casi constante → staticCompositionLocalOf (lectura más barata)")
            BulletPoint("Es para diseño/contexto transversal, NO para tu estado de pantalla")
            BulletPoint("Regla: si solo lo usan 2-3 composables, pásalo por parámetro")
        }
    }
}

// ============================================================
// --- PANTALLA PRINCIPAL DE LA LECCIÓN ---
// ============================================================
@Composable
fun PantallaCompositionLocal() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("CompositionLocal y theming", style = MaterialTheme.typography.headlineSmall)
            Text(
                "El mecanismo secreto detrás de MaterialTheme, y cómo usarlo para " +
                        "tu propio design system.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoTokensEnVivo() }
        item { TarjetaInternasMaterial() }
        item { TarjetaChuletaCompositionLocal() }
    }
}
