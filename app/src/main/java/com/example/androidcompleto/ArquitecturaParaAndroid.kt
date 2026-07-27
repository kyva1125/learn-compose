package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * GUÍA MAESTRA DE ARQUITECTURA SENIOR (Lección 17)
 *
 * Clean Architecture + MVI en una pantalla real (un mini carrito).
 *
 * LAS CAPAS (la regla: las flechas apuntan hacia el DOMINIO):
 *
 *   UI (Compose)  →  ViewModel  →  DOMAIN (UseCases, modelos)  ←  DATA (Room/Retrofit)
 *
 *   - DOMAIN: Kotlin PURO. Sin Android, sin Compose, sin Retrofit.
 *     Aquí viven las reglas de negocio → es lo más fácil de testear.
 *   - DATA: implementa lo que el dominio pide (repositorios).
 *   - UI: pinta estado y emite eventos. CERO lógica de negocio.
 *
 * MVI (Model-View-Intent) = MVVM con esteroides de disciplina:
 *   1. UN estado inmutable (data class) que describe TODA la pantalla
 *   2. UN canal de eventos (sealed interface): la UI solo dice "pasó esto"
 *   3. UN reducer PURO: (estadoActual, evento) → estadoNuevo
 *   Flujo circular: UI emite evento → reducer crea estado → UI repinta.
 *   Ventaja: el reducer es una función pura → se testea sin Android
 *   (mira ArquitecturaSeniorTest.kt).
 *
 * INYECCIÓN DE DEPENDENCIAS (DI):
 *   El ViewModel NO construye sus dependencias (new = acoplamiento),
 *   las RECIBE. Aquí usamos un contenedor manual para que veas la idea
 *   desnuda; en producción esto mismo lo automatizan:
 *     - Hilt: @HiltViewModel + @Inject constructor(...) (Android puro)
 *     - Koin: viewModel { CarritoViewModel(get()) } (también KMP)
 */

// ============================================================
// --- CAPA DOMAIN: Kotlin puro, testeable al 100% ---
// ============================================================
data class ProductoTienda(val id: Int, val nombre: String, val precio: Double)

data class LineaCarrito(val producto: ProductoTienda, val cantidad: Int)

data class ResumenCarrito(
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val total: Double = 0.0
)

/**
 * UN USE CASE = UNA regla de negocio con nombre propio.
 * "operator fun invoke" permite llamarlo como función: calcularResumen(...)
 */
class CalcularResumenUseCase {
    operator fun invoke(lineas: List<LineaCarrito>, cupon: String): ResumenCarrito {
        val subtotal = lineas.sumOf { it.producto.precio * it.cantidad }
        val descuento = when (cupon.trim().uppercase()) {
            "SENIOR10" -> subtotal * 0.10
            "COMPOSE20" -> subtotal * 0.20
            else -> 0.0
        }
        return ResumenCarrito(
            subtotal = subtotal,
            descuento = descuento,
            total = subtotal - descuento
        )
    }
}

// ============================================================
// --- MVI: estado único + eventos + reducer puro ---
// ============================================================
data class CarritoEstado(
    val lineas: List<LineaCarrito> = emptyList(),
    val cupon: String = "",
    val resumen: ResumenCarrito = ResumenCarrito()
)

sealed interface CarritoEvento {
    data class Agregar(val producto: ProductoTienda) : CarritoEvento
    data class Quitar(val productoId: Int) : CarritoEvento
    data class CambiarCupon(val texto: String) : CarritoEvento
    data object Vaciar : CarritoEvento
}

/**
 * EL REDUCER: función PURA. Sin corrutinas, sin Android, sin efectos.
 * Mismo estado + mismo evento = SIEMPRE el mismo resultado.
 * Por eso ArquitecturaSeniorTest.kt la prueba en milisegundos.
 */
fun reducirCarrito(
    estado: CarritoEstado,
    evento: CarritoEvento,
    calcularResumen: CalcularResumenUseCase
): CarritoEstado {
    val lineas = when (evento) {
        is CarritoEvento.Agregar -> {
            val existente = estado.lineas.find { it.producto.id == evento.producto.id }
            if (existente == null) {
                estado.lineas + LineaCarrito(evento.producto, 1)
            } else {
                estado.lineas.map {
                    if (it.producto.id == evento.producto.id) it.copy(cantidad = it.cantidad + 1)
                    else it
                }
            }
        }
        is CarritoEvento.Quitar -> estado.lineas
            .map {
                if (it.producto.id == evento.productoId) it.copy(cantidad = it.cantidad - 1)
                else it
            }
            .filter { it.cantidad > 0 }
        is CarritoEvento.CambiarCupon -> estado.lineas
        CarritoEvento.Vaciar -> emptyList()
    }
    val cupon = if (evento is CarritoEvento.CambiarCupon) evento.texto else estado.cupon

    return estado.copy(
        lineas = lineas,
        cupon = cupon,
        resumen = calcularResumen(lineas, cupon)
    )
}

// ============================================================
// --- DI MANUAL: el contenedor de la app ---
// ============================================================
// La versión artesanal de lo que Hilt/Koin hacen con anotaciones.
// Un único lugar donde se construyen las dependencias.
object ContenedorApp {
    val calcularResumen: CalcularResumenUseCase by lazy { CalcularResumenUseCase() }
}

// ============================================================
// --- EL VIEWMODEL: pegamento fino entre UI y dominio ---
// ============================================================
class CarritoViewModel(
    // La dependencia ENTRA por constructor → en tests le pasas un fake.
    private val calcularResumen: CalcularResumenUseCase = ContenedorApp.calcularResumen
) : ViewModel() {

    private val _estado = MutableStateFlow(CarritoEstado())
    val estado = _estado.asStateFlow()

    // UN solo punto de entrada: la UI no llama métodos sueltos,
    // emite eventos. Refactorizar y loguear se vuelve trivial.
    fun onEvento(evento: CarritoEvento) {
        _estado.update { actual -> reducirCarrito(actual, evento, calcularResumen) }
    }
}

// ============================================================
// --- CAPA UI: pinta estado, emite eventos, cero lógica ---
// ============================================================
val catalogoDemo = listOf(
    ProductoTienda(1, "Café ☕", 3.50),
    ProductoTienda(2, "Pizza 🍕", 12.90),
    ProductoTienda(3, "Sushi 🍣", 18.00)
)

@Composable
fun PantallaArquitectura() {
    val vm: CarritoViewModel = viewModel()
    val estado by vm.estado.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Clean Architecture + MVI", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Toda la pantalla es UN data class (CarritoEstado). Toda interacción es " +
                        "UN evento. Toda la lógica vive en un reducer puro + un UseCase, " +
                        "ambos testeados en ArquitecturaSeniorTest.kt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // --- Catálogo ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catálogo", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        catalogoDemo.forEach { producto ->
                            OutlinedButton(
                                onClick = { vm.onEvento(CarritoEvento.Agregar(producto)) }
                            ) { Text(producto.nombre) }
                        }
                    }
                    Text(
                        "Cada botón emite CarritoEvento.Agregar(producto). La UI no sabe " +
                                "qué pasa después: eso decide el reducer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // --- Carrito ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Carrito (${estado.lineas.sumOf { it.cantidad }})",
                            style = MaterialTheme.typography.titleMedium)
                        if (estado.lineas.isNotEmpty()) {
                            TextButton(onClick = { vm.onEvento(CarritoEvento.Vaciar) }) {
                                Text("Vaciar")
                            }
                        }
                    }

                    if (estado.lineas.isEmpty()) {
                        Text("Vacío. Agrega algo del catálogo 👆",
                            style = MaterialTheme.typography.bodySmall)
                    }

                    estado.lineas.forEach { linea ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${linea.producto.nombre} × ${linea.cantidad}")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("S/ ${"%.2f".format(linea.producto.precio * linea.cantidad)}")
                                IconButton(onClick = {
                                    vm.onEvento(CarritoEvento.Quitar(linea.producto.id))
                                }) { Text("➖") }
                            }
                        }
                    }
                }
            }
        }

        // --- Cupón + resumen (el UseCase en acción) ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = estado.cupon,
                        onValueChange = { vm.onEvento(CarritoEvento.CambiarCupon(it)) },
                        label = { Text("Cupón (prueba SENIOR10 o COMPOSE20)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal")
                        Text("S/ ${"%.2f".format(estado.resumen.subtotal)}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Descuento")
                        Text("- S/ ${"%.2f".format(estado.resumen.descuento)}",
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL", style = MaterialTheme.typography.titleMedium)
                        Text("S/ ${"%.2f".format(estado.resumen.total)}",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // --- Chuleta ---
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de arquitectura", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Dominio en Kotlin puro: sin imports de Android = testeable")
                    BulletPoint("1 pantalla = 1 data class de estado + 1 sealed de eventos")
                    BulletPoint("Reducer puro: (estado, evento) → estado. Sin sorpresas")
                    BulletPoint("Dependencias por CONSTRUCTOR, nunca new adentro")
                    BulletPoint("Hilt (Android) o Koin (KMP) automatizan el ContenedorApp")
                    BulletPoint("Lo async (red/BD) va en el ViewModel con viewModelScope y el resultado entra al reducer como otro evento")
                }
            }
        }
    }
}
