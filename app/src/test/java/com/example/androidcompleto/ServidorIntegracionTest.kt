package com.example.androidcompleto

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * TEST DE INTEGRACIÓN DEL SERVIDOR (Lección S1)
 *
 * Arranca el servidor Ktor DE VERDAD y le hace peticiones HTTP reales
 * por TCP con el cliente Ktor. Si estos tests pasan, el servidor de la
 * lección funciona: no es una simulación.
 *
 * En un proyecto de servidor real usarías `testApplication { }` (más
 * rápido, sin abrir puertos), como se explica en la lección S2. Aquí
 * levantamos el servidor completo a propósito, para demostrar que el
 * mismo código que corre dentro de la app responde por la red.
 */
class ServidorIntegracionTest {

    companion object {
        private lateinit var cliente: HttpClient

        @BeforeClass
        @JvmStatic
        fun arrancarServidor() {
            ServidorDemo.arrancar()
            cliente = HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                defaultRequest { url("http://127.0.0.1:${ServidorDemo.PUERTO}/") }
                expectSuccess = false     // queremos inspeccionar los 4xx a mano
            }
            // Dar un margen al motor CIO para abrir el puerto
            Thread.sleep(1_500)
        }

        @AfterClass
        @JvmStatic
        fun detenerServidor() {
            cliente.close()
            ServidorDemo.detener()
        }
    }

    @Test
    fun `el servidor responde en la ruta de salud`() = runBlocking {
        val respuesta: HttpResponse = cliente.get("salud")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals("OK", respuesta.bodyAsText())
    }

    @Test
    fun `GET tareas devuelve 200 y una lista deserializable`() = runBlocking {
        val respuesta: HttpResponse = cliente.get("tareas")
        assertEquals(HttpStatusCode.OK, respuesta.status)

        val tareas: List<TareaApi> = respuesta.body()
        assertTrue("el servidor debe traer datos de ejemplo", tareas.isNotEmpty())
    }

    @Test
    fun `GET de un id inexistente devuelve 404 con el error tipado`() = runBlocking {
        val respuesta: HttpResponse = cliente.get("tareas/99999")

        assertEquals(HttpStatusCode.NotFound, respuesta.status)
        val error: ErrorApi = respuesta.body()
        assertEquals("NO_ENCONTRADA", error.codigo)
    }

    @Test
    fun `GET con un id no numerico devuelve 400`() = runBlocking {
        val respuesta: HttpResponse = cliente.get("tareas/abc")

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
        val error: ErrorApi = respuesta.body()
        assertEquals("VALIDACION", error.codigo)
    }

    @Test
    fun `POST valido crea la tarea y devuelve 201`() = runBlocking {
        val respuesta: HttpResponse = cliente.post("tareas") {
            contentType(ContentType.Application.Json)
            setBody(NuevaTareaApi("Tarea creada desde el test"))
        }

        assertEquals(HttpStatusCode.Created, respuesta.status)
        val creada: TareaApi = respuesta.body()
        assertEquals("Tarea creada desde el test", creada.titulo)
        assertTrue("el servidor asigna el id", creada.id > 0)

        // Y de verdad quedó guardada:
        val todas: List<TareaApi> = cliente.get("tareas").body()
        assertTrue(todas.any { it.id == creada.id })
    }

    @Test
    fun `POST con titulo corto lo rechaza el servidor con 400`() = runBlocking {
        val respuesta: HttpResponse = cliente.post("tareas") {
            contentType(ContentType.Application.Json)
            setBody(NuevaTareaApi("ab"))     // más corto que LARGO_MIN
        }

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
        val error: ErrorApi = respuesta.body()
        assertEquals("VALIDACION", error.codigo)
        // El mensaje viene de la MISMA función que usa el cliente:
        assertEquals(ReglasTareaApi.validarTitulo("ab"), error.mensaje)
    }

    @Test
    fun `la busqueda por query param filtra`() = runBlocking {
        val respuesta: HttpResponse = cliente.get("buscar?q=ktor")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        val encontradas: List<TareaApi> = respuesta.body()
        assertTrue(encontradas.all { it.titulo.contains("ktor", ignoreCase = true) })
    }
}
