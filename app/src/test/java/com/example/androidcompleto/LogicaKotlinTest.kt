package com.example.androidcompleto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LECCIÓN DE TESTING UNITARIO
 * Los tests unitarios prueban lógica PURA (sin Android, sin UI) y corren
 * en tu PC en milisegundos. Ejecútalos con:
 *
 *   ./gradlew :app:testDebugUnitTest
 *
 * o clic derecho → Run en Android Studio.
 *
 * PATRÓN AAA: Arrange (preparar) → Act (ejecutar) → Assert (verificar).
 * Por eso extraer la lógica a funciones puras (fuera de los Composables)
 * no es solo elegancia: es lo que las hace TESTEABLES.
 */
class LogicaKotlinTest {

    // --- Tests de la función de extensión String.esPalindromo() ---

    @Test
    fun `palindromo con espacios y mayusculas es detectado`() {
        assertTrue("Anita lava la tina".esPalindromo())
    }

    @Test
    fun `texto normal no es palindromo`() {
        assertFalse("Kotlin".esPalindromo())
    }

    @Test
    fun `palindromo con signos de puntuacion`() {
        // los signos ¿? y espacios se ignoran, solo cuentan letras y números
        assertTrue("¿Dabale arroz a la zorra el abad?".esPalindromo())
    }

    // --- Tests de List<Int>.segundoMasGrande() ---

    @Test
    fun `segundo mas grande ignora duplicados`() {
        // Arrange
        val lista = listOf(3, 7, 7, 1)
        // Act
        val resultado = lista.segundoMasGrande()
        // Assert
        assertEquals(3, resultado)
    }

    @Test
    fun `lista con un solo valor no tiene segundo`() {
        assertNull(listOf(5).segundoMasGrande())
    }

    @Test
    fun `lista vacia devuelve null en vez de crashear`() {
        assertNull(emptyList<Int>().segundoMasGrande())
    }

    // --- Tests del genérico primeroYUltimo() ---

    @Test
    fun `primero y ultimo funciona con cualquier tipo`() {
        assertEquals(Pair(10, 30), primeroYUltimo(listOf(10, 20, 30)))
        assertEquals(Pair("a", "c"), primeroYUltimo(listOf("a", "b", "c")))
    }

    @Test
    fun `primero y ultimo con lista vacia devuelve null`() {
        assertNull(primeroYUltimo(emptyList<Int>()))
    }

    // --- Tests de demoWhen() ---

    @Test
    fun `when clasifica los numeros correctamente`() {
        assertEquals("-3 es negativo", demoWhen(-3))
        assertEquals("es cero", demoWhen(0))
        assertEquals("7 tiene un dígito", demoWhen(7))
        assertEquals("42 tiene dos dígitos", demoWhen(42))
        assertEquals("1000 es grande", demoWhen(1000))
    }
}
