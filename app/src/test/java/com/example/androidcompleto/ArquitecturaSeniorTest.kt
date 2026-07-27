package com.example.androidcompleto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TESTS DEL DOMINIO DE LA LECCIÓN 17 (Arquitectura Senior)
 *
 * Fíjate en lo que NO hay aquí: ni Android, ni Compose, ni corrutinas,
 * ni mocks. Como el UseCase y el reducer son Kotlin PURO, se testean
 * con JUnit a secas y corren en milisegundos en la JVM.
 *
 * ESA es la recompensa de la Clean Architecture: la lógica de negocio
 * se prueba sin emulador.
 *
 * Patrón AAA en cada test: Arrange (preparar) → Act (actuar) → Assert (verificar).
 */
class ArquitecturaSeniorTest {

    private val calcularResumen = CalcularResumenUseCase()

    private val cafe = ProductoTienda(1, "Café", 3.50)
    private val pizza = ProductoTienda(2, "Pizza", 12.90)

    // ============ CalcularResumenUseCase ============

    @Test
    fun `subtotal suma precio por cantidad de cada linea`() {
        // Arrange
        val lineas = listOf(LineaCarrito(cafe, 2), LineaCarrito(pizza, 1))
        // Act
        val resumen = calcularResumen(lineas, cupon = "")
        // Assert
        assertEquals(19.90, resumen.subtotal, 0.001)
        assertEquals(0.0, resumen.descuento, 0.001)
        assertEquals(19.90, resumen.total, 0.001)
    }

    @Test
    fun `cupon SENIOR10 aplica 10 por ciento aunque venga en minusculas y con espacios`() {
        val lineas = listOf(LineaCarrito(pizza, 10)) // subtotal 129.0

        val resumen = calcularResumen(lineas, cupon = "  senior10 ")

        assertEquals(12.90, resumen.descuento, 0.001)
        assertEquals(116.10, resumen.total, 0.001)
    }

    @Test
    fun `cupon invalido no descuenta nada`() {
        val lineas = listOf(LineaCarrito(cafe, 1))

        val resumen = calcularResumen(lineas, cupon = "GRATIS_TOTAL")

        assertEquals(0.0, resumen.descuento, 0.001)
        assertEquals(resumen.subtotal, resumen.total, 0.001)
    }

    @Test
    fun `carrito vacio da resumen en cero`() {
        val resumen = calcularResumen(emptyList(), cupon = "COMPOSE20")

        assertEquals(0.0, resumen.subtotal, 0.001)
        assertEquals(0.0, resumen.descuento, 0.001)
        assertEquals(0.0, resumen.total, 0.001)
    }

    // ============ reducirCarrito (el reducer puro) ============

    @Test
    fun `agregar un producto nuevo crea una linea con cantidad 1`() {
        val estado = CarritoEstado()

        val nuevo = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)

        assertEquals(1, nuevo.lineas.size)
        assertEquals(1, nuevo.lineas.first().cantidad)
        assertEquals(3.50, nuevo.resumen.total, 0.001)
    }

    @Test
    fun `agregar el mismo producto dos veces incrementa la cantidad, no duplica lineas`() {
        var estado = CarritoEstado()

        estado = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)
        estado = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)

        assertEquals(1, estado.lineas.size)
        assertEquals(2, estado.lineas.first().cantidad)
        assertEquals(7.00, estado.resumen.total, 0.001)
    }

    @Test
    fun `quitar decrementa y elimina la linea al llegar a cero`() {
        var estado = CarritoEstado()
        estado = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)
        estado = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)

        estado = reducirCarrito(estado, CarritoEvento.Quitar(cafe.id), calcularResumen)
        assertEquals(1, estado.lineas.first().cantidad)

        estado = reducirCarrito(estado, CarritoEvento.Quitar(cafe.id), calcularResumen)
        assertTrue(estado.lineas.isEmpty())
        assertEquals(0.0, estado.resumen.total, 0.001)
    }

    @Test
    fun `cambiar cupon recalcula el total sin tocar las lineas`() {
        var estado = CarritoEstado()
        estado = reducirCarrito(estado, CarritoEvento.Agregar(pizza), calcularResumen)

        estado = reducirCarrito(estado, CarritoEvento.CambiarCupon("COMPOSE20"), calcularResumen)

        assertEquals(1, estado.lineas.size)          // las líneas no cambiaron
        assertEquals("COMPOSE20", estado.cupon)
        assertEquals(2.58, estado.resumen.descuento, 0.001)
        assertEquals(10.32, estado.resumen.total, 0.001)
    }

    @Test
    fun `vaciar limpia lineas y resumen pero conserva el cupon escrito`() {
        var estado = CarritoEstado()
        estado = reducirCarrito(estado, CarritoEvento.Agregar(cafe), calcularResumen)
        estado = reducirCarrito(estado, CarritoEvento.CambiarCupon("SENIOR10"), calcularResumen)

        estado = reducirCarrito(estado, CarritoEvento.Vaciar, calcularResumen)

        assertTrue(estado.lineas.isEmpty())
        assertEquals("SENIOR10", estado.cupon)
        assertEquals(0.0, estado.resumen.total, 0.001)
    }

    @Test
    fun `el reducer es puro - no muta el estado anterior`() {
        val estadoInicial = CarritoEstado()

        reducirCarrito(estadoInicial, CarritoEvento.Agregar(cafe), calcularResumen)

        // El estado original sigue intacto: el reducer devolvió una COPIA.
        assertTrue(estadoInicial.lineas.isEmpty())
        assertEquals(0.0, estadoInicial.resumen.total, 0.001)
    }
}
