package com.example.androidcompleto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * NAVEGACIÓN TYPE-SAFE — PISTA COMPOSE (Lección C4b)
 *
 * La Lección 6 te enseñó navegación con rutas de texto. Funciona, pero
 * tiene un problema grave que solo descubres en producción:
 *
 *   ✖ ANTES (rutas como String):
 *       navController.navigate("detalle/${producto.id}")
 *       composable("detalle/{id}") { back ->
 *           val id = back.arguments?.getString("id")?.toIntOrNull() ?: 0
 *       }
 *     - Si te equivocas al escribir "detalle", NO falla al compilar: crashea en runtime
 *     - Todo argumento viaja como String: conviertes a mano y rezas
 *     - Añadir un parámetro obliga a revisar cada navigate() a mano
 *
 *   ✔ AHORA (Navigation Compose 2.8+ con kotlinx.serialization):
 *       navController.navigate(Detalle(id = producto.id))
 *       composable<Detalle> { back -> val args = back.toRoute<Detalle>() }
 *     - Las rutas son CLASES: el compilador verifica tipos y nombres
 *     - Los argumentos llegan ya tipados (Int, Boolean, listas, objetos)
 *     - Renombrar o añadir un campo → error de compilación, no crash de usuario
 *
 * Requisito: el plugin kotlin-serialization (ya activado en este proyecto).
 */

// ============================================================
// --- 1. LAS RUTAS SON CLASES ---
// ============================================================
/** Sin argumentos → @Serializable object */
@Serializable
object RutaListaTS

/** Con argumentos → @Serializable data class. Los tipos son REALES. */
@Serializable
data class RutaDetalleTS(val id: Int, val destacado: Boolean = false)

/** Argumento opcional → tipo nullable con valor por defecto */
@Serializable
data class RutaFiltroTS(val categoria: String? = null)

data class ArticuloTS(val id: Int, val nombre: String, val categoria: String, val precio: Double)

val articulosTS = listOf(
    ArticuloTS(1, "Teclado mecánico", "Periféricos", 259.90),
    ArticuloTS(2, "Monitor 27\"", "Pantallas", 899.00),
    ArticuloTS(3, "Ratón vertical", "Periféricos", 149.50),
    ArticuloTS(4, "Webcam 4K", "Vídeo", 420.00)
)

// ============================================================
// --- 2. EL NAVHOST TIPADO ---
// ============================================================
@Composable
fun PantallaNavegacionTypeSafe() {
    val nav = rememberNavController()

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Navegación type-safe", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Las rutas son clases @Serializable. Prueba a navegar: los argumentos " +
                            "llegan ya convertidos, sin castear Strings.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // startDestination ya no es un String: es una INSTANCIA de la ruta
        NavHost(navController = nav, startDestination = RutaListaTS) {

            // composable<T> en vez de composable("ruta")
            composable<RutaListaTS> {
                ListaTS(
                    onAbrir = { articulo, destacado ->
                        // Navegar = construir el objeto. Autocompletado y tipos ✔
                        nav.navigate(RutaDetalleTS(id = articulo.id, destacado = destacado))
                    },
                    onFiltrar = { cat -> nav.navigate(RutaFiltroTS(categoria = cat)) }
                )
            }

            composable<RutaDetalleTS> { entrada ->
                // toRoute<T>() deserializa los argumentos ya tipados
                val args: RutaDetalleTS = entrada.toRoute()
                DetalleTS(args = args, nav = nav)
            }

            composable<RutaFiltroTS> { entrada ->
                val args: RutaFiltroTS = entrada.toRoute()
                FiltroTS(categoria = args.categoria, nav = nav)
            }
        }
    }
}

@Composable
fun ListaTS(
    onAbrir: (ArticuloTS, Boolean) -> Unit,
    onFiltrar: (String?) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onFiltrar("Periféricos") }) { Text("Filtrar") }
                OutlinedButton(onClick = { onFiltrar(null) }) { Text("Sin filtro") }
            }
        }
        items(articulosTS, key = { it.id }) { a ->
            Card(Modifier.fillMaxWidth().clickable { onAbrir(a, false) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(a.nombre, style = MaterialTheme.typography.titleSmall)
                    Text("${a.categoria} · S/ %.2f".format(a.precio),
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onAbrir(a, true) }) {
                        Text("Abrir como destacado ⭐")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Fíjate", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    BulletPoint("navigate(RutaDetalleTS(id = 3, destacado = true))")
                    BulletPoint("Si cambias el nombre del campo, no compila (antes: crash)")
                    BulletPoint("'destacado' tiene valor por defecto: no hay que pasarlo")
                }
            }
        }
    }
}

@Composable
fun DetalleTS(args: RutaDetalleTS, nav: NavHostController) {
    val articulo = articulosTS.firstOrNull { it.id == args.id }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Volver") }

        if (articulo == null) {
            Text("No encontrado")
        } else {
            Text(
                articulo.nombre + if (args.destacado) " ⭐" else "",
                style = MaterialTheme.typography.headlineSmall
            )
            Text("Categoría: ${articulo.categoria}")
            Text("Precio: S/ %.2f".format(articulo.precio))
        }

        HorizontalDivider()
        Card {
            Column(Modifier.padding(12.dp)) {
                Text("Argumentos recibidos (ya tipados)",
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text("id: Int = ${args.id}", style = MaterialTheme.typography.bodySmall)
                Text("destacado: Boolean = ${args.destacado}",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "No hubo ningún toIntOrNull() ni ?: 0. toRoute<RutaDetalleTS>() " +
                            "devolvió el objeto entero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun FiltroTS(categoria: String?, nav: NavHostController) {
    val filtrados = categoria?.let { c -> articulosTS.filter { it.categoria == c } }
        ?: articulosTS

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Volver") }
        Text(
            "Filtro: ${categoria ?: "(ninguno — argumento null)"}",
            style = MaterialTheme.typography.titleMedium
        )
        filtrados.forEach { Text("• ${it.nombre}") }

        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(12.dp)) {
                Text("📌 Chuleta type-safe", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                BulletPoint("@Serializable object → ruta sin argumentos")
                BulletPoint("@Serializable data class → ruta con argumentos tipados")
                BulletPoint("composable<Ruta> { it.toRoute<Ruta>() }")
                BulletPoint("navigate(Ruta(...)) construyendo el objeto")
                BulletPoint("Nullable + valor por defecto = argumento opcional")
                BulletPoint("Grafos anidados: navigation<RutaPadre>(startDestination = ...)")
                BulletPoint("Deep links: composable<T>(deepLinks = listOf(navDeepLink<T>(basePath)))")
            }
        }
    }
}
