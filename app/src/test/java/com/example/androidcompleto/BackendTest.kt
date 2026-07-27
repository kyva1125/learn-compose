package com.example.androidcompleto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TESTS DEL MODELO Y LA VALIDACIÓN COMPARTIDOS (Lección S1)
 *
 * La gracia del full-stack Kotlin: estos tests prueban A LA VEZ el
 * servidor y el cliente, porque los dos usan exactamente este código.
 * Escribes la validación una vez, la testeas una vez, la usas dos veces.
 */
class BackendTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ============ VALIDACIÓN COMPARTIDA ============

    @Test
    fun `un titulo valido no produce error`() {
        assertNull(ReglasTareaApi.validarTitulo("Aprender Ktor"))
    }

    @Test
    fun `un titulo vacio o en blanco se rechaza`() {
        assertEquals("El título no puede estar vacío", ReglasTareaApi.validarTitulo(""))
        assertEquals("El título no puede estar vacío", ReglasTareaApi.validarTitulo("    "))
    }

    @Test
    fun `un titulo demasiado corto se rechaza`() {
        val error = ReglasTareaApi.validarTitulo("ab")

        assertNotNull(error)
        assertTrue(error!!.contains("${ReglasTareaApi.LARGO_MIN}"))
    }

    @Test
    fun `un titulo demasiado largo se rechaza`() {
        val largo = "x".repeat(ReglasTareaApi.LARGO_MAX + 1)

        val error = ReglasTareaApi.validarTitulo(largo)

        assertNotNull(error)
        assertTrue(error!!.contains("${ReglasTareaApi.LARGO_MAX}"))
    }

    @Test
    fun `los limites exactos son validos`() {
        val minimo = "x".repeat(ReglasTareaApi.LARGO_MIN)
        val maximo = "x".repeat(ReglasTareaApi.LARGO_MAX)

        assertNull(ReglasTareaApi.validarTitulo(minimo))
        assertNull(ReglasTareaApi.validarTitulo(maximo))
    }

    // ============ EL CONTRATO JSON (cliente ↔ servidor) ============

    @Test
    fun `una tarea se serializa al JSON que espera el otro lado`() {
        val tarea = TareaApi(id = 7, titulo = "Compartir modelos", hecha = true)

        val texto = json.encodeToString(TareaApi.serializer(), tarea)

        assertTrue(texto.contains("\"id\":7"))
        assertTrue(texto.contains("\"titulo\":\"Compartir modelos\""))
        assertTrue(texto.contains("\"hecha\":true"))
    }

    @Test
    fun `ida y vuelta - lo que serializa el servidor lo lee el cliente`() {
        val original = TareaApi(1, "Round trip", false)

        val texto = json.encodeToString(TareaApi.serializer(), original)
        val recuperada = json.decodeFromString(TareaApi.serializer(), texto)

        assertEquals(original, recuperada)
    }

    @Test
    fun `el valor por defecto de hecha permite omitir el campo`() {
        val texto = """{"id":3,"titulo":"Sin campo hecha"}"""

        val tarea = json.decodeFromString(TareaApi.serializer(), texto)

        assertEquals(false, tarea.hecha)
    }

    @Test
    fun `ignoreUnknownKeys evita romper cuando el backend anade campos`() {
        // El servidor despliega una versión nueva con un campo extra.
        // El cliente antiguo NO debe reventar.
        val texto = """{"id":1,"titulo":"Compatible","hecha":false,"prioridad":"alta"}"""

        val tarea = json.decodeFromString(TareaApi.serializer(), texto)

        assertEquals("Compatible", tarea.titulo)
    }

    @Test
    fun `el error de la API tambien viaja tipado`() {
        val error = ErrorApi("VALIDACION", "Mínimo 3 caracteres")

        val texto = json.encodeToString(ErrorApi.serializer(), error)
        val recuperado = json.decodeFromString(ErrorApi.serializer(), texto)

        assertEquals(error, recuperado)
        assertEquals("VALIDACION", recuperado.codigo)
    }

    @Test
    fun `NuevaTarea solo lleva lo necesario para crear`() {
        val nueva = NuevaTareaApi("Solo el título")

        val texto = json.encodeToString(NuevaTareaApi.serializer(), nueva)

        // El id lo asigna el SERVIDOR: el cliente no debe mandarlo.
        assertTrue(texto.contains("titulo"))
        assertTrue("el cliente no debe enviar id", !texto.contains("\"id\""))
    }
}
