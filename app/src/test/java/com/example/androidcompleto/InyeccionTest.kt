package com.example.androidcompleto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * POR QUÉ LA INYECCIÓN DE DEPENDENCIAS EXISTE (Lección M3)
 *
 * Estos tests son la demostración práctica: sin inyectar por constructor,
 * ninguno de ellos sería posible.
 */
class InyeccionTest {

    @Test
    fun `con un reloj fijo el registro es predecible`() {
        // Arrange: inyectamos un fake determinista
        val registro = RegistroDeAuditoria(RelojFijo("T0"))

        // Act
        registro.registrar("guardar")
        registro.registrar("salir")

        // Assert: el resultado es exacto, no "depende de la hora"
        assertEquals(listOf("[T0] guardar", "[T0] salir"), registro.todo())
    }

    @Test
    fun `con el reloj real el instante cambia entre llamadas`() {
        val reloj = RelojReal()

        val primero = reloj.ahora()
        Thread.sleep(2)
        val segundo = reloj.ahora()

        // Justo por esto el reloj real no sirve para aserciones exactas:
        // en el test usamos el fake.
        assertNotEquals(primero, segundo)
    }

    @Test
    fun `el registro delega en el reloj que le inyectaron, sea cual sea`() {
        val relojDeBroma = object : RelojApp {
            var vecesLlamado = 0
            override fun ahora(): String {
                vecesLlamado++
                return "llamada-$vecesLlamado"
            }
        }
        val registro = RegistroDeAuditoria(relojDeBroma)

        registro.registrar("a")
        registro.registrar("b")

        assertEquals(2, relojDeBroma.vecesLlamado)
        assertEquals(listOf("[llamada-1] a", "[llamada-2] b"), registro.todo())
    }

    @Test
    fun `todo devuelve una instantanea, no la lista viva`() {
        val registro = RegistroDeAuditoria(RelojFijo("T0"))
        registro.registrar("primera")

        // Guardamos el resultado ANTES de añadir más entradas
        val instantanea = registro.todo()
        registro.registrar("segunda")

        // Gracias al toList() del repositorio, la instantánea no cambió...
        assertEquals(1, instantanea.size)
        assertTrue(instantanea.first().endsWith("primera"))
        // ...pero el registro interno sí siguió creciendo.
        assertEquals(2, registro.todo().size)
    }

    @Test
    fun `el modulo de Koin declara las dependencias esperadas`() {
        asegurarKoinIniciado()
        val koin = org.koin.core.context.GlobalContext.get()

        // Koin resuelve la cadena completa: RegistroDeAuditoria necesita un RelojApp
        val registro = koin.get<RegistroDeAuditoria>()
        val reloj = koin.get<RelojApp>()

        registro.registrar("desde koin")

        assertTrue(reloj is RelojReal)
        assertEquals(1, registro.todo().size)
    }
}
