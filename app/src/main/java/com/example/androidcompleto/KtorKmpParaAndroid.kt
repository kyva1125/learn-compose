package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * EL STACK DE DATOS KMP — PISTA KMP (Lección M2)
 *
 * ⚠️ LO IMPORTANTE DE ESTA LECCIÓN: todo el código de abajo (modelos,
 * cliente HTTP, repositorio, ViewModel) es EXACTAMENTE el que pondrías
 * en `commonMain` de un proyecto multiplataforma. No hay ni un import de
 * Android en la capa de datos. Corre igual en iPhone, Desktop y Web.
 *
 * EL CAMBIO RESPECTO A TU STACK ANDROID:
 *
 *   Retrofit  →  Ktor Client         (HTTP multiplataforma)
 *   Gson      →  kotlinx.serialization (JSON oficial de Kotlin, sin reflexión)
 *   Hilt      →  Koin                 (DI que funciona en iOS; Hilt NO)
 *   OkHttp    →  motor (engine) intercambiable por plataforma:
 *                  Android → OkHttp/CIO · iOS → Darwin · JS → Js
 *
 * LA ÚNICA PARTE ESPECÍFICA DE CADA PLATAFORMA es el MOTOR:
 *
 *   // commonMain
 *   expect fun crearHttpClient(): HttpClient
 *   // androidMain →  actual fun crearHttpClient() = HttpClient(OkHttp) { ... }
 *   // iosMain     →  actual fun crearHttpClient() = HttpClient(Darwin) { ... }
 *
 * kotlinx.serialization vs Gson: Gson usa REFLEXIÓN en runtime (lenta, y
 * no existe en Kotlin/Native). kotlinx.serialization genera el parser en
 * COMPILACIÓN con el plugin — más rápido, seguro y multiplataforma.
 */

// ============================================================
// --- 1. MODELOS con kotlinx.serialization (100% commonMain) ---
// ============================================================
@Serializable
data class PostKmp(
    val id: Int,
    val title: String,
    val body: String,
    // @SerialName mapea el nombre del JSON al nombre bonito de Kotlin
    @SerialName("userId") val autorId: Int
)

@Serializable
data class NuevoPostKmp(
    val title: String,
    val body: String,
    @SerialName("userId") val autorId: Int
)

// ============================================================
// --- 2. EL CLIENTE KTOR (commonMain salvo el motor) ---
// ============================================================
object ClienteKtor {

    /**
     * Este Json es idéntico en todas las plataformas.
     * ignoreUnknownKeys es CRÍTICO: si el backend añade un campo mañana,
     * tu app no revienta.
     */
    val json = Json {
        ignoreUnknownKeys = true   // campos nuevos del backend → ignorados
        isLenient = true           // tolera JSON no estricto
        prettyPrint = true
        encodeDefaults = true      // incluye los valores por defecto al serializar
    }

    /**
     * En KMP esto sería `expect fun`. El motor (OkHttp aquí, Darwin en iOS)
     * es lo ÚNICO que cambia: toda la configuración de abajo se comparte.
     */
    val http: HttpClient by lazy {
        HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
            // Plugin de JSON: serializa y deserializa automáticamente
            install(ContentNegotiation) { json(json) }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }

            // El equivalente a un Interceptor de OkHttp, multiplataforma
            defaultRequest {
                url("https://jsonplaceholder.typicode.com/")
                headers.append("X-Cliente", "learn-compose-kmp")
            }

            expectSuccess = true   // lanza excepción en 4xx/5xx (como Retrofit con Response<T>)
        }
    }
}

// ============================================================
// --- 3. EL REPOSITORIO (commonMain puro) ---
// ============================================================
class PostsKtorRepository(private val http: HttpClient = ClienteKtor.http) {

    /** GET con query params. Fíjate: el tipo de retorno hace el parseo solo. */
    suspend fun listarPosts(limite: Int): List<PostKmp> =
        http.get("posts") {
            parameter("_limit", limite)
        }.body()          // body<List<PostKmp>>() inferido por el tipo de retorno

    /** GET de un solo elemento */
    suspend fun obtenerPost(id: Int): PostKmp =
        http.get("posts/$id").body()

    /** POST con cuerpo JSON serializado automáticamente */
    suspend fun crearPost(nuevo: NuevoPostKmp): PostKmp =
        http.post("posts") {
            contentType(ContentType.Application.Json)
            setBody(nuevo)     // ← kotlinx.serialization lo convierte a JSON
        }.body()
}

// ============================================================
// --- 4. EL VIEWMODEL (en KMP iría también en commonMain) ---
// ============================================================
sealed interface KtorUiState {
    data object Inicial : KtorUiState
    data object Cargando : KtorUiState
    data class Exito(val posts: List<PostKmp>, val aviso: String? = null) : KtorUiState
    data class Error(val mensaje: String) : KtorUiState
}

class KtorViewModel(
    private val repo: PostsKtorRepository = PostsKtorRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow<KtorUiState>(KtorUiState.Inicial)
    val estado = _estado.asStateFlow()

    private val _jsonCrudo = MutableStateFlow<String?>(null)
    val jsonCrudo = _jsonCrudo.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _estado.value = KtorUiState.Cargando
            try {
                val posts = repo.listarPosts(limite = 8)
                _estado.value = KtorUiState.Exito(posts)
                // Serializamos de vuelta para que veas el JSON generado
                _jsonCrudo.value = ClienteKtor.json.encodeToString(posts.take(1))
            } catch (e: Exception) {
                _estado.value = KtorUiState.Error(
                    "${e::class.simpleName}: ${e.message ?: "fallo de red"}"
                )
            }
        }
    }

    fun publicar() {
        viewModelScope.launch {
            val actuales = (_estado.value as? KtorUiState.Exito)?.posts ?: emptyList()
            try {
                val creado = repo.crearPost(
                    NuevoPostKmp(
                        title = "Escrito con Ktor",
                        body = "Este POST salió de código que compila también para iOS.",
                        autorId = 1
                    )
                )
                _estado.value = KtorUiState.Exito(
                    posts = listOf(creado) + actuales,
                    aviso = "✔ POST creado (id ${creado.id}) — el cuerpo se serializó solo"
                )
            } catch (e: Exception) {
                _estado.value = KtorUiState.Error("Al publicar: ${e.message}")
            }
        }
    }
}

// ============================================================
// --- 5. LA PANTALLA (lo único que en KMP sería Compose Multiplatform) ---
// ============================================================
@Composable
fun PantallaKtorKmp() {
    val vm: KtorViewModel = viewModel()
    val estado by vm.estado.collectAsState()
    val jsonCrudo by vm.jsonCrudo.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ktor + kotlinx.serialization", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Todo el código de datos de esta pantalla es válido en commonMain: " +
                        "cero imports de Android. Lo único específico de plataforma es el motor HTTP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::cargar,
                    enabled = estado !is KtorUiState.Cargando
                ) { Text("GET /posts") }
                OutlinedButton(
                    onClick = vm::publicar,
                    enabled = estado is KtorUiState.Exito
                ) { Text("POST /posts") }
            }
        }

        item {
            when (val e = estado) {
                KtorUiState.Inicial -> Text("Pulsa 'GET /posts' para llamar a la API con Ktor.")
                KtorUiState.Cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Llamando con Ktor...")
                }
                is KtorUiState.Error -> Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("✖ ${e.mensaje}")
                        Text(
                            "expectSuccess = true convierte un 404 o un 500 en excepción, " +
                                    "igual que harías a mano con Response<T> en Retrofit.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                is KtorUiState.Exito -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    e.aviso?.let {
                        Text(it, color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${e.posts.size} posts deserializados",
                        style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        (estado as? KtorUiState.Exito)?.let { exito ->
            items(exito.posts, key = { it.id }) { post ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(post.title, style = MaterialTheme.typography.titleSmall)
                        Text(post.body, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        Text("autorId ${post.autorId} (mapeado de \"userId\" con @SerialName)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        jsonCrudo?.let {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("JSON generado por kotlinx.serialization",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        BloqueCodigo("json.encodeToString(posts.take(1))", it)
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Retrofit → Ktor, lado a lado",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Retrofit (solo Android/JVM)",
                        """
                        interface Api {
                            @GET("posts")
                            suspend fun posts(@Query("_limit") n: Int): List<Post>
                        }
                        val api = Retrofit.Builder()
                            .baseUrl(BASE)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build().create(Api::class.java)
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Ktor (Android + iOS + Desktop + Web)",
                        """
                        suspend fun posts(n: Int): List<Post> =
                            http.get("posts") { parameter("_limit", n) }.body()
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ktor no necesita interfaz con anotaciones: son funciones suspend " +
                                "normales. Menos magia, más control, y compila para todas partes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta del stack KMP", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("ignoreUnknownKeys = true SIEMPRE (el backend cambiará)")
                    BulletPoint("@SerialName mapea nombres del JSON a nombres Kotlin")
                    BulletPoint("ContentNegotiation + json() = parseo automático")
                    BulletPoint("defaultRequest = baseUrl + headers comunes (como un Interceptor)")
                    BulletPoint("expectSuccess = true → los 4xx/5xx lanzan excepción")
                    BulletPoint("El motor es lo único por plataforma: OkHttp / Darwin / CIO / Js")
                    BulletPoint("kotlinx.serialization genera en compilación: sin reflexión")
                    BulletPoint("Persistencia KMP: SQLDelight o Room KMP (Room ya es multiplataforma)")
                }
            }
        }
    }
}
