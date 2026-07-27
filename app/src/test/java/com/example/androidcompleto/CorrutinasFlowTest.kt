package com.example.androidcompleto

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * TESTS DE CORRUTINAS Y FLOWS (lo que demuestra la Lección K4)
 *
 * Aquí se ve la magia del TIEMPO VIRTUAL: hay delays de segundos y
 * la suite entera corre en milisegundos. runTest lleva su propio reloj.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CorrutinasFlowTest {

    // ============ TIEMPO VIRTUAL ============

    @Test
    fun `un delay de 10 segundos no tarda nada real`() = runTest {
        val inicioReal = System.currentTimeMillis()

        delay(10_000)   // 10 s de tiempo VIRTUAL

        assertEquals(10_000, testScheduler.currentTime)                        // el reloj virtual avanzó
        assertTrue(System.currentTimeMillis() - inicioReal < 1_000)  // el real, no
    }

    // ============ CONCURRENCIA ESTRUCTURADA ============

    @Test
    fun `async en paralelo tarda lo que el mas lento, no la suma`() = runTest {
        suspend fun tarea(ms: Long): Long { delay(ms); return ms }

        val inicio = testScheduler.currentTime
        coroutineScope {
            val a = async { tarea(300) }
            val b = async { tarea(500) }
            a.await() + b.await()
        }

        // Secuencial serían 800 ms; en paralelo, 500.
        assertEquals(500, testScheduler.currentTime - inicio)
    }

    @Test
    fun `cancelar el padre cancela a todos los hijos`() = runTest {
        var hijoTermino = false

        val padre = launch {
            launch {
                delay(1_000)
                hijoTermino = true
            }
        }

        advanceTimeBy(100)
        padre.cancelAndJoin()
        advanceUntilIdle()

        assertTrue(padre.isCancelled)
        assertFalse("el hijo no debió terminar", hijoTermino)
    }

    @Test
    fun `el bloque finally se ejecuta al cancelar`() = runTest {
        var limpiezaHecha = false

        val job = launch {
            try {
                delay(1_000)
            } finally {
                limpiezaHecha = true
            }
        }

        advanceTimeBy(100)
        job.cancelAndJoin()

        assertTrue("finally debe correr para liberar recursos", limpiezaHecha)
    }

    // ============ EXCEPCIONES ============

    @Test
    fun `en coroutineScope un hijo que falla cancela a su hermano`() = runTest {
        var hermanoTermino = false

        val error = runCatching {
            coroutineScope {
                launch {
                    delay(500)
                    hermanoTermino = true
                }
                launch {
                    delay(100)
                    throw IllegalStateException("boom")
                }
            }
        }.exceptionOrNull()

        assertEquals("boom", error?.message)
        assertFalse("el hermano debió ser cancelado", hermanoTermino)
    }

    @Test
    fun `en supervisorScope el hermano sobrevive al fallo`() = runTest {
        var hermanoTermino = false
        val manejador = CoroutineExceptionHandler { _, _ -> /* tragado a propósito */ }

        supervisorScope {
            launch(manejador) {
                delay(100)
                throw IllegalStateException("boom")
            }
            launch {
                delay(500)
                hermanoTermino = true
            }
        }

        assertTrue("con supervisorScope el hermano SÍ termina", hermanoTermino)
    }

    @Test
    fun `async guarda la excepcion hasta el await`() = runTest {
        // OJO: dentro de un supervisorScope. Si lanzas el async directamente en
        // el scope raíz, su fallo cancela al padre y tumba el test antes del
        // await — justo lo que evita supervisorScope.
        val error = runCatching {
            supervisorScope {
                val diferido = async { throw IllegalArgumentException("tarde") }
                diferido.await()      // la excepción salta AQUÍ, no al lanzarla
            }
        }.exceptionOrNull()

        assertEquals("tarde", error?.message)
    }

    // ============ OPERADORES DE FLOW (con Turbine) ============

    @Test
    fun `debounce solo emite tras el silencio`() = runTest {
        val tecleo = flow {
            emit("a"); delay(50)
            emit("ab"); delay(50)
            emit("abc"); delay(500)   // pausa larga
            emit("abcd")
        }

        tecleo.debounce(200).test {
            assertEquals("abc", awaitItem())    // las rápidas se descartaron
            assertEquals("abcd", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `distinctUntilChanged filtra repetidos consecutivos`() = runTest {
        flowOf(1, 1, 2, 2, 2, 3, 1).distinctUntilChanged().test {
            assertEquals(1, awaitItem())
            assertEquals(2, awaitItem())
            assertEquals(3, awaitItem())
            assertEquals(1, awaitItem())   // repetido NO consecutivo: sí pasa
            awaitComplete()
        }
    }

    @Test
    fun `flatMapLatest cancela la emision anterior`() = runTest {
        val procesados = mutableListOf<Int>()

        flowOf(1, 2, 3)
            .onEach { delay(50) }
            .flatMapLatest { n ->
                flow {
                    delay(100)          // más lento que la fuente: 1 y 2 se cancelan
                    procesados += n
                    emit(n)
                }
            }
            .test {
                assertEquals(3, awaitItem())
                awaitComplete()
            }

        assertEquals("solo el último debió completarse", listOf(3), procesados)
    }

    @Test
    fun `combine emite cuando cualquiera de los dos flujos cambia`() = runTest {
        val nombres = flow { emit("Ana"); delay(100); emit("José") }
        val edades = flow { delay(50); emit(30) }

        combine(nombres, edades) { n, e -> "$n-$e" }.test {
            assertEquals("Ana-30", awaitItem())
            assertEquals("José-30", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `retry reintenta solo los errores de red y luego catch rescata`() = runTest {
        var intentos = 0
        val apiInestable = flow {
            intentos++
            if (intentos < 3) throw IOException("timeout")
            emit("ok en intento $intentos")
        }

        apiInestable
            .retry(retries = 5) { it is IOException }
            .catch { emit("rescatado") }
            .test {
                assertEquals("ok en intento 3", awaitItem())
                awaitComplete()
            }

        assertEquals(3, intentos)
    }

    @Test
    fun `catch atrapa el error cuando se agotan los reintentos`() = runTest {
        val siempreFalla = flow<String> { throw IOException("caída total") }

        siempreFalla
            .retry(retries = 2) { it is IOException }
            .catch { e -> emit("✖ ${e.message}") }
            .test {
                assertEquals("✖ caída total", awaitItem())
                awaitComplete()
            }
    }

    // ============ StateFlow vs SharedFlow ============

    @Test
    fun `StateFlow entrega solo el ultimo valor a un colector nuevo`() = runTest {
        val estado = MutableStateFlow("inicial")
        estado.value = "uno"
        estado.value = "dos"

        estado.test {
            assertEquals("dos", awaitItem())   // "uno" se perdió: es estado, no historial
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SharedFlow sin replay pierde lo emitido antes de suscribirse`() = runTest {
        val eventos = MutableSharedFlow<String>(replay = 0)
        val recibidos = mutableListOf<String>()

        eventos.emit("evento perdido")    // nadie escuchaba todavía

        val trabajo = launch { eventos.collect { recibidos += it } }
        runCurrent()
        eventos.emit("evento recibido")
        runCurrent()
        trabajo.cancelAndJoin()

        assertEquals(listOf("evento recibido"), recibidos)
    }

    @Test
    fun `SharedFlow con replay 1 si entrega el ultimo evento`() = runTest {
        val eventos = MutableSharedFlow<String>(replay = 1)
        eventos.emit("con replay")

        eventos.test {
            assertEquals("con replay", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
