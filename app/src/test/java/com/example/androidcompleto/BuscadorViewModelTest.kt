package com.example.androidcompleto

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * TESTS DE UN VIEWMODEL ASÍNCRONO (debounce + flatMapLatest)
 *
 * Fíjate en dos cosas:
 *  - Ningún test tarda 300 ms de verdad: runTest usa TIEMPO VIRTUAL.
 *  - No hay mocks: un fake escrito a mano, mucho más legible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BuscadorViewModelTest {

    // Sustituye Dispatchers.Main (que no existe en la JVM) por uno de test
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---------- EL FAKE ----------
    class FakeBuscadorRepository(
        private val catalogo: List<String> = listOf("Kotlin", "Kotlin Multiplatform", "Compose"),
        private val fallaCon: Exception? = null
    ) : BuscadorRepository {
        var vecesLlamado = 0
            private set
        val consultasRecibidas = mutableListOf<String>()

        override suspend fun buscar(consulta: String): List<String> {
            vecesLlamado++
            consultasRecibidas += consulta
            fallaCon?.let { throw it }
            return catalogo.filter { it.contains(consulta, ignoreCase = true) }
        }
    }

    @Test
    fun `el estado inicial es Vacio`() = runTest {
        val vm = BuscadorViewModel(FakeBuscadorRepository())

        assertEquals(BuscadorEstado.Vacio, vm.estado.value)
    }

    @Test
    fun `no busca antes de que pasen los 300ms de debounce`() = runTest {
        val fake = FakeBuscadorRepository()
        val vm = BuscadorViewModel(fake)

        vm.estado.test {
            assertEquals(BuscadorEstado.Vacio, awaitItem())   // suscribe el stateIn

            vm.alEscribir("kot")
            advanceTimeBy(299)      // reloj VIRTUAL: 0 ms reales
            runCurrent()
            assertEquals(0, fake.vecesLlamado)                // aún no llamó al repo

            advanceTimeBy(2)
            runCurrent()
            assertEquals(BuscadorEstado.Buscando, awaitItem())
            assertEquals(1, fake.vecesLlamado)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emite Buscando y luego Resultados con los items filtrados`() = runTest {
        val vm = BuscadorViewModel(FakeBuscadorRepository())

        vm.estado.test {
            assertEquals(BuscadorEstado.Vacio, awaitItem())

            vm.alEscribir("kotlin")
            advanceUntilIdle()

            assertEquals(BuscadorEstado.Buscando, awaitItem())
            val resultados = awaitItem() as BuscadorEstado.Resultados
            assertEquals(listOf("Kotlin", "Kotlin Multiplatform"), resultados.items)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flatMapLatest descarta la busqueda intermedia al teclear rapido`() = runTest {
        val fake = FakeBuscadorRepository()
        val vm = BuscadorViewModel(fake)

        vm.estado.test {
            awaitItem()   // Vacio inicial

            // Tecleo rápido: cada pulsación llega antes de los 300ms
            vm.alEscribir("k")
            advanceTimeBy(100)
            vm.alEscribir("ko")
            advanceTimeBy(100)
            vm.alEscribir("kotlin")
            advanceUntilIdle()

            // El debounce descartó "k" y "ko": solo se buscó la última
            assertEquals(1, fake.vecesLlamado)
            assertEquals(listOf("kotlin"), fake.consultasRecibidas)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `consulta en blanco vuelve a Vacio sin llamar al repositorio`() = runTest {
        val fake = FakeBuscadorRepository()
        val vm = BuscadorViewModel(fake)

        vm.estado.test {
            assertEquals(BuscadorEstado.Vacio, awaitItem())

            vm.alEscribir("   ")
            advanceUntilIdle()

            assertEquals(0, fake.vecesLlamado)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `un fallo del repositorio se convierte en estado Error, no en crash`() = runTest {
        val fake = FakeBuscadorRepository(fallaCon = java.io.IOException("sin conexión"))
        val vm = BuscadorViewModel(fake)

        vm.estado.test {
            awaitItem()   // Vacio

            vm.alEscribir("kotlin")
            advanceUntilIdle()

            assertEquals(BuscadorEstado.Buscando, awaitItem())
            val error = awaitItem() as BuscadorEstado.Error
            assertEquals("sin conexión", error.mensaje)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinctUntilChanged ignora escribir dos veces lo mismo`() = runTest {
        val fake = FakeBuscadorRepository()
        val vm = BuscadorViewModel(fake)

        vm.estado.test {
            awaitItem()

            vm.alEscribir("compose")
            advanceUntilIdle()
            vm.alEscribir("compose")   // idéntica
            advanceUntilIdle()

            assertEquals(1, fake.vecesLlamado)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `la consulta expuesta refleja lo que escribe el usuario al instante`() = runTest {
        val vm = BuscadorViewModel(FakeBuscadorRepository())

        vm.alEscribir("ho")

        // La consulta NO tiene debounce: se actualiza ya (el campo debe responder)
        assertEquals("ho", vm.consulta.value)
        assertTrue(vm.estado.value is BuscadorEstado.Vacio)
    }
}
