package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

/**
 * BACKEND CON KOTLIN — PISTA BACKEND (Lección S1)
 *
 * ⚡ ESTA LECCIÓN ARRANCA UN SERVIDOR HTTP DE VERDAD DENTRO DE TU MÓVIL.
 * No es una simulación: es Ktor Server con motor CIO escuchando en
 * localhost, y el cliente Ktor de la lección M2 haciéndole peticiones
 * reales por TCP. El mismo código, palabra por palabra, es el que
 * desplegarías en un servidor de producción.
 *
 * POR QUÉ KOTLIN EN EL BACKEND:
 *   - MISMO lenguaje que tu app: un solo equipo, un solo set de skills
 *   - MISMOS modelos: el data class @Serializable se COMPARTE entre
 *     servidor y cliente. Si cambias un campo, el cliente no compila.
 *     Se acabaron los errores de "el backend cambió el JSON y no avisó".
 *   - Corrutinas: miles de peticiones concurrentes sin hilos bloqueados
 *   - Ktor es el mismo framework en cliente y servidor
 *
 * ANATOMÍA DE UN SERVIDOR KTOR:
 *
 *   embeddedServer(CIO, port = 8080) {   // motor + puerto
 *       install(ContentNegotiation) { json() }   // plugins
 *       routing {                                 // rutas
 *           get("/tareas") { call.respond(lista) }
 *       }
 *   }.start()
 *
 * LOS 3 CONCEPTOS:
 *   1. ENGINE (motor): CIO, Netty, Jetty... quien habla TCP de verdad
 *   2. PLUGINS: funcionalidad transversal (JSON, errores, auth, CORS, logs)
 *   3. ROUTING: el árbol de rutas. Anidable y componible.
 */

// ============================================================
// --- 1. EL MODELO COMPARTIDO (la joya del full-stack Kotlin) ---
// ============================================================
/**
 * ⭐ En un proyecto full-stack real, ESTE archivo vive en el módulo
 * `shared/commonMain` y lo importan LOS DOS: el servidor y la app.
 * Una sola definición, cero desincronización.
 */
@Serializable
data class TareaApi(
    val id: Int,
    val titulo: String,
    val hecha: Boolean = false
)

@Serializable
data class NuevaTareaApi(val titulo: String)

/** Los errores también viajan tipados, no como Strings sueltos. */
@Serializable
data class ErrorApi(val codigo: String, val mensaje: String)

/**
 * Validación COMPARTIDA: el cliente valida antes de enviar (respuesta
 * inmediata al usuario) y el servidor vuelve a validar (nunca confíes
 * en el cliente). Una sola función, dos usos.
 */
object ReglasTareaApi {
    const val LARGO_MIN = 3
    const val LARGO_MAX = 60

    fun validarTitulo(titulo: String): String? = when {
        titulo.isBlank() -> "El título no puede estar vacío"
        titulo.length < LARGO_MIN -> "Mínimo $LARGO_MIN caracteres"
        titulo.length > LARGO_MAX -> "Máximo $LARGO_MAX caracteres"
        else -> null   // null = válido
    }
}

// ============================================================
// --- 2. EL SERVIDOR ---
// ============================================================
/** Excepción propia: StatusPages la traducirá a un 404 con JSON. */
class NoEncontradaApiException(val id: Int) : RuntimeException("Tarea $id no encontrada")

object ServidorDemo {

    const val PUERTO = 8099

    // "Base de datos" en memoria. En producción: Exposed + PostgreSQL.
    private val tareas = mutableListOf(
        TareaApi(1, "Aprender Ktor Server", true),
        TareaApi(2, "Compartir modelos con el cliente"),
        TareaApi(3, "Desplegar en producción")
    )
    private val siguienteId = AtomicInteger(4)

    private var servidor: EmbeddedServer<*, *>? = null

    val estaVivo: Boolean get() = servidor != null

    /** El registro de peticiones que ves en pantalla (nuestro "log" del servidor). */
    val registro = MutableStateFlow<List<String>>(emptyList())

    private fun log(linea: String) = registro.update { (it + linea).takeLast(40) }

    fun arrancar() {
        if (servidor != null) return

        servidor = embeddedServer(CIO, port = PUERTO) {

            // --- PLUGIN 1: JSON automático en peticiones y respuestas ---
            install(ContentNegotiation) {
                json(Json { prettyPrint = true; ignoreUnknownKeys = true })
            }

            // --- PLUGIN 2: manejo CENTRAL de errores ---
            // Sin esto, cada ruta necesitaría su try/catch.
            install(StatusPages) {
                exception<NoEncontradaApiException> { call, causa ->
                    log("  ✖ 404 → ${causa.message}")
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorApi("NO_ENCONTRADA", causa.message ?: "")
                    )
                }
                exception<IllegalArgumentException> { call, causa ->
                    log("  ✖ 400 → ${causa.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorApi("VALIDACION", causa.message ?: "Petición inválida")
                    )
                }
                exception<Throwable> { call, causa ->
                    log("  ✖ 500 → ${causa::class.simpleName}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorApi("INTERNO", "Algo salió mal")
                    )
                }
            }

            // --- LAS RUTAS ---
            routing {
                get("/salud") {
                    log("→ GET /salud")
                    call.respondText("OK", ContentType.Text.Plain)
                }

                // Rutas anidadas: todo lo de dentro cuelga de /tareas
                route("/tareas") {

                    get {
                        log("→ GET /tareas (${tareas.size} elementos)")
                        call.respond(tareas)     // se serializa a JSON solo
                    }

                    // Parámetro de ruta
                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                            ?: throw IllegalArgumentException("El id debe ser un número")
                        log("→ GET /tareas/$id")
                        val tarea = tareas.find { it.id == id }
                            ?: throw NoEncontradaApiException(id)   // → 404 vía StatusPages
                        call.respond(tarea)
                    }

                    post {
                        // receive<T>() deserializa el cuerpo JSON al tipo compartido
                        val nueva = call.receive<NuevaTareaApi>()
                        log("→ POST /tareas  {\"titulo\":\"${nueva.titulo}\"}")

                        // MISMA validación que usa el cliente:
                        ReglasTareaApi.validarTitulo(nueva.titulo)?.let {
                            throw IllegalArgumentException(it)   // → 400
                        }

                        val creada = TareaApi(siguienteId.getAndIncrement(), nueva.titulo)
                        tareas += creada
                        log("  ✔ 201 Created → id ${creada.id}")
                        call.respond(HttpStatusCode.Created, creada)
                    }

                    // PATCH para alternar el estado
                    patch("/{id}/alternar") {
                        val id = call.parameters["id"]?.toIntOrNull()
                            ?: throw IllegalArgumentException("id inválido")
                        log("→ PATCH /tareas/$id/alternar")
                        val i = tareas.indexOfFirst { it.id == id }
                        if (i == -1) throw NoEncontradaApiException(id)
                        tareas[i] = tareas[i].copy(hecha = !tareas[i].hecha)
                        call.respond(tareas[i])
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                            ?: throw IllegalArgumentException("id inválido")
                        log("→ DELETE /tareas/$id")
                        if (!tareas.removeIf { it.id == id }) throw NoEncontradaApiException(id)
                        // 204: éxito sin cuerpo de respuesta
                        call.respond(HttpStatusCode.NoContent)
                    }
                }

                // Query params: /buscar?q=ktor
                get("/buscar") {
                    val q = call.request.queryParameters["q"].orEmpty()
                    log("→ GET /buscar?q=$q")
                    call.respond(tareas.filter { it.titulo.contains(q, ignoreCase = true) })
                }
            }
        }.also { it.start(wait = false) }   // wait = false: no bloquea el hilo

        log("🟢 Servidor Ktor escuchando en http://127.0.0.1:$PUERTO")
    }

    fun detener() {
        servidor?.stop(gracePeriodMillis = 100, timeoutMillis = 500)
        servidor = null
        log("🔴 Servidor detenido")
    }
}

// ============================================================
// --- 3. EL CLIENTE (el mismo Ktor Client de la lección M2) ---
// ============================================================
private val clienteLocal by lazy {
    HttpClient {
        install(ClientContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("http://127.0.0.1:${ServidorDemo.PUERTO}/") }
    }
}

// ============================================================
// --- 4. EL VIEWMODEL ---
// ============================================================
class BackendViewModel : ViewModel() {

    private val _tareas = MutableStateFlow<List<TareaApi>>(emptyList())
    val tareas = _tareas.asStateFlow()

    private val _servidorVivo = MutableStateFlow(false)
    val servidorVivo = _servidorVivo.asStateFlow()

    private val _ultimaRespuesta = MutableStateFlow<String?>(null)
    val ultimaRespuesta = _ultimaRespuesta.asStateFlow()

    private val _errorValidacion = MutableStateFlow<String?>(null)
    val errorValidacion = _errorValidacion.asStateFlow()

    val registroServidor = ServidorDemo.registro.asStateFlow()

    fun alternarServidor() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (ServidorDemo.estaVivo) ServidorDemo.detener() else ServidorDemo.arrancar()
            }
            _servidorVivo.value = ServidorDemo.estaVivo
            if (ServidorDemo.estaVivo) cargar() else _tareas.value = emptyList()
        }
    }

    fun cargar() = peticion {
        _tareas.value = clienteLocal.get("tareas").body()
        "GET /tareas → 200 (${_tareas.value.size} tareas)"
    }

    fun crear(titulo: String) {
        // VALIDACIÓN EN EL CLIENTE con la MISMA función del servidor:
        // feedback instantáneo, sin gastar una petición de red.
        val error = ReglasTareaApi.validarTitulo(titulo)
        if (error != null) {
            _errorValidacion.value = "$error (detectado en el CLIENTE, sin llamar al servidor)"
            return
        }
        _errorValidacion.value = null

        peticion {
            val respuesta: HttpResponse = clienteLocal.post("tareas") {
                contentType(ContentType.Application.Json)
                setBody(NuevaTareaApi(titulo))
            }
            val creada: TareaApi = respuesta.body()
            cargarInterno()
            "POST /tareas → ${respuesta.status.value} ${respuesta.status.description} (id ${creada.id})"
        }
    }

    /** Envía a propósito un título inválido para que el SERVIDOR lo rechace. */
    fun probarValidacionServidor() = peticion {
        val respuesta: HttpResponse = clienteLocal.post("tareas") {
            contentType(ContentType.Application.Json)
            setBody(NuevaTareaApi("ab"))     // demasiado corto
        }
        val cuerpo = respuesta.bodyAsText()
        "POST /tareas con \"ab\" → ${respuesta.status.value}\n$cuerpo"
    }

    fun probar404() = peticion {
        val respuesta: HttpResponse = clienteLocal.get("tareas/9999")
        "GET /tareas/9999 → ${respuesta.status.value}\n${respuesta.bodyAsText()}"
    }

    fun alternarTarea(id: Int) = peticion {
        val respuesta: HttpResponse = clienteLocal.patch("tareas/$id/alternar")
        cargarInterno()
        "PATCH /tareas/$id/alternar → ${respuesta.status.value}"
    }

    fun borrar(id: Int) = peticion {
        val respuesta: HttpResponse = clienteLocal.delete("tareas/$id")
        cargarInterno()
        "DELETE /tareas/$id → ${respuesta.status.value} (204 = sin contenido)"
    }

    private suspend fun cargarInterno() {
        _tareas.value = clienteLocal.get("tareas").body()
    }

    private fun peticion(bloque: suspend () -> String) {
        viewModelScope.launch {
            _ultimaRespuesta.value = try {
                bloque()
            } catch (e: Exception) {
                "✖ ${e::class.simpleName}: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ServidorDemo.detener()
    }
}

// ============================================================
// --- 5. LA PANTALLA ---
// ============================================================
@Composable
fun PantallaBackendKtor() {
    val vm: BackendViewModel = viewModel()
    val tareas by vm.tareas.collectAsState()
    val vivo by vm.servidorVivo.collectAsState()
    val respuesta by vm.ultimaRespuesta.collectAsState()
    val registro by vm.registroServidor.collectAsState()
    val errorValidacion by vm.errorValidacion.collectAsState()
    var titulo by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Backend con Ktor Server", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Esta pantalla arranca un servidor HTTP REAL dentro de tu móvil y le " +
                        "hace peticiones por TCP. El mismo código que desplegarías en producción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // --- Control del servidor ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (vivo) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (vivo) "🟢 Servidor ENCENDIDO" else "⚫ Servidor apagado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Button(onClick = vm::alternarServidor) {
                            Text(if (vivo) "Apagar" else "Encender")
                        }
                    }
                    Text(
                        "http://127.0.0.1:${ServidorDemo.PUERTO}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!vivo) {
                        Text("Enciéndelo para poder hacer peticiones 👆",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (vivo) {
            // --- Peticiones ---
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Haz peticiones al servidor",
                            style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Título de la tarea") },
                            supportingText = {
                                Text("Entre ${ReglasTareaApi.LARGO_MIN} y ${ReglasTareaApi.LARGO_MAX} caracteres")
                            },
                            isError = errorValidacion != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        errorValidacion?.let {
                            Text("⚠ $it", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                vm.crear(titulo); titulo = ""
                            }) { Text("POST") }
                            OutlinedButton(onClick = vm::cargar) { Text("GET") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = vm::probar404) { Text("Probar 404") }
                            OutlinedButton(onClick = vm::probarValidacionServidor) {
                                Text("Probar 400")
                            }
                        }
                    }
                }
            }

            // --- Respuesta HTTP ---
            respuesta?.let {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Respuesta HTTP", style = MaterialTheme.typography.labelLarge)
                            Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                lineHeight = 15.sp)
                        }
                    }
                }
            }

            // --- Las tareas que devuelve el servidor ---
            item {
                Text("Datos del servidor (${tareas.size})",
                    style = MaterialTheme.typography.titleMedium)
            }
            items(tareas.size) { i ->
                val t = tareas[i]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = t.hecha, onCheckedChange = { vm.alternarTarea(t.id) })
                        Column(Modifier.weight(1f)) {
                            Text(t.titulo, style = MaterialTheme.typography.bodyMedium)
                            Text("id ${t.id}", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { vm.borrar(t.id) }) { Text("Borrar") }
                    }
                }
            }

            // --- Log del servidor ---
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Log del servidor", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                registro.forEach {
                                    Text(it, fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Teoría ---
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("El modelo COMPARTIDO: la clave del full-stack Kotlin",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "El data class TareaApi y el objeto ReglasTareaApi que acabas de usar " +
                                "son los MISMOS en el servidor y en el cliente. Un solo sitio.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "shared/commonMain/Modelos.kt — lo importan los dos",
                        """
                        @Serializable
                        data class TareaApi(val id: Int, val titulo: String, val hecha: Boolean = false)

                        object ReglasTareaApi {
                            fun validarTitulo(t: String): String? = when {
                                t.isBlank()    -> "El título no puede estar vacío"
                                t.length < 3   -> "Mínimo 3 caracteres"
                                else           -> null
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Si mañana renombras 'titulo' a 'nombre', el cliente DEJA DE COMPILAR. " +
                                "Con un backend en otro lenguaje, te enterarías con un crash en " +
                                "producción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("El servidor completo, en 20 líneas",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "server/src/main/kotlin/Application.kt",
                        """
                        fun main() {
                            embeddedServer(Netty, port = 8080) {
                                install(ContentNegotiation) { json() }
                                install(StatusPages) {
                                    exception<NoEncontrada> { call, e ->
                                        call.respond(HttpStatusCode.NotFound, ErrorApi(...))
                                    }
                                }
                                routing {
                                    route("/tareas") {
                                        get { call.respond(repo.todas()) }
                                        get("/{id}") {
                                            val id = call.parameters["id"]!!.toInt()
                                            call.respond(repo.porId(id) ?: throw NoEncontrada(id))
                                        }
                                        post {
                                            val nueva = call.receive<NuevaTareaApi>()
                                            call.respond(HttpStatusCode.Created, repo.crear(nueva))
                                        }
                                    }
                                }
                            }.start(wait = true)
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de Ktor Server", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("embeddedServer(motor, puerto) { plugins + routing }")
                    BulletPoint("Motores: Netty (producción), CIO (Kotlin puro), Jetty, Tomcat")
                    BulletPoint("install(ContentNegotiation) { json() } → JSON automático")
                    BulletPoint("install(StatusPages) → errores en UN sitio, no try/catch por ruta")
                    BulletPoint("call.receive<T>() para el cuerpo · call.parameters para la ruta")
                    BulletPoint("call.request.queryParameters para ?clave=valor")
                    BulletPoint("Códigos: 200 OK · 201 Created · 204 NoContent · 400 · 404 · 500")
                    BulletPoint("route(\"/x\") { } anida rutas y permite componer módulos")
                    BulletPoint("Cada handler es una corrutina: I/O sin bloquear hilos")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Siguiente paso real: cambiar la lista en memoria por Exposed + " +
                                "PostgreSQL, añadir Authentication con JWT y desplegar con Docker. " +
                                "Lo ves en la lección S2.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
