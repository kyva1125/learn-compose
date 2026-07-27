package com.example.androidcompleto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * GUÍA MAESTRA DE NAVIGATION COMPOSE
 * Hasta ahora navegamos con un enum + when (válido para apps pequeñas).
 * La forma PROFESIONAL es Navigation Compose:
 *
 * - NavController: el "cerebro" que recuerda dónde estás (back stack).
 * - NavHost: el contenedor que dibuja la pantalla según la ruta actual.
 * - Ruta: un String tipo URL ("detalle/{id}") que identifica cada pantalla.
 * - Argumentos: datos que viajan en la ruta (como query params de una URL).
 *
 * VENTAJAS sobre el enum: back stack automático, deep links, argumentos
 * tipados, animaciones de transición y soporte multi-módulo.
 */

// Definimos las rutas en un solo lugar para evitar errores de tipeo.
object Rutas {
    const val LISTA = "lista"
    const val DETALLE = "detalle/{productoId}" // {productoId} es un argumento
    fun detalleDe(id: Int) = "detalle/$id"     // helper para construir la ruta real
}

// Datos de ejemplo (en una app real vendrían de un ViewModel/Repository)
data class ProductoNav(val id: Int, val nombre: String, val precio: Double)

val productosDemo = listOf(
    ProductoNav(1, "Laptop Gamer", 4500.0),
    ProductoNav(2, "Mouse Inalámbrico", 89.9),
    ProductoNav(3, "Teclado Mecánico", 350.0),
    ProductoNav(4, "Monitor 27\"", 1200.0),
)

@Composable
fun PantallaNavegacion() {
    // 1. Creamos el controlador. remember* está implícito: sobrevive recomposiciones.
    val navController = rememberNavController()

    // 2. NavHost: mapea cada ruta a su Composable. startDestination = pantalla inicial.
    NavHost(navController = navController, startDestination = Rutas.LISTA) {

        // Pantalla A: la lista
        composable(Rutas.LISTA) {
            ListaProductos(onProductoClick = { producto ->
                // 3. NAVEGAR: construimos la ruta con el id real
                navController.navigate(Rutas.detalleDe(producto.id))
            })
        }

        // Pantalla B: el detalle, declarando el argumento y su tipo
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("productoId") { type = NavType.IntType })
        ) { backStackEntry ->
            // 4. LEER EL ARGUMENTO que viajó en la ruta
            val id = backStackEntry.arguments?.getInt("productoId") ?: 0
            DetalleProducto(
                productoId = id,
                onVolver = {
                    // 5. VOLVER: saca esta pantalla del back stack
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ListaProductos(onProductoClick: (ProductoNav) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Catálogo (toca un producto)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(productosDemo) { producto ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductoClick(producto) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(producto.nombre, modifier = Modifier.weight(1f))
                        Text("S/ ${producto.precio}")
                    }
                }
            }
        }
    }
}

@Composable
fun DetalleProducto(productoId: Int, onVolver: () -> Unit) {
    // Buscamos el producto por el id que llegó como argumento
    val producto = productosDemo.find { it.id == productoId }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Detalle del Producto", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (producto != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ID recibido por argumento: $productoId")
                    Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
                    Text("Precio: S/ ${producto.precio}")
                }
            }
        } else {
            Text("Producto no encontrado")
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onVolver) {
            Text("Volver (popBackStack)")
        }
        Text(
            "El botón físico 'Atrás' también funciona automáticamente: " +
                    "el NavController maneja el back stack por ti.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
