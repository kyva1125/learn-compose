package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * GUÍA MAESTRA DE RETROFIT AVANZADO
 * La lección 9 te enseñó el GET básico. Esto es lo que usarás en un
 * trabajo real:
 *
 * 1. OkHttpClient personalizado -> timeouts, interceptores
 * 2. Interceptor de LOGGING     -> ver cada petición/respuesta en Logcat
 * 3. Interceptor de HEADERS     -> añadir tokens de auth a TODAS las peticiones
 * 4. @Path y @Query             -> URLs dinámicas (/posts/5, ?postId=3)
 * 5. @POST con @Body            -> enviar JSON al servidor
 * 6. Response<T>                -> leer el código HTTP y manejar 404, 500...
 */

// ============================================================
// --- 1. MODELOS ---
// ============================================================
data class Post(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val titulo: String,
    @SerializedName("body") val cuerpo: String,
    @SerializedName("userId") val userId: Int
)

data class Comentario(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("body") val cuerpo: String
)

// El body que ENVIAREMOS en el POST (sin id: lo asigna el servidor)
data class NuevoPost(
    @SerializedName("title") val titulo: String,
    @SerializedName("body") val cuerpo: String,
    @SerializedName("userId") val userId: Int = 1
)

// ============================================================
// --- 2. EL API SERVICE AVANZADO ---
// ============================================================
interface ApiAvanzada {

    // @Path: el valor se INCRUSTA en la URL -> GET /posts/5
    @GET("posts/{id}")
    suspend fun obtenerPost(@Path("id") id: Int): Response<Post>

    // @Query: parámetros de búsqueda -> GET /comments?postId=3
    @GET("comments")
    suspend fun obtenerComentarios(@Query("postId") postId: Int): List<Comentario>

    // @POST con @Body: Gson convierte el objeto a JSON y lo envía
    @POST("posts")
    suspend fun crearPost(@Body post: NuevoPost): Response<Post>

    // Para la lección Offline-First
    @GET("posts")
    suspend fun obtenerPosts(): List<Post>
}

// ============================================================
// --- 3. EL CLIENTE PROFESIONAL (OkHttp + interceptores) ---
// ============================================================
object ClienteAvanzado {

    // INTERCEPTOR DE LOGGING: imprime cada petición en Logcat (busca "okhttp")
    // ⚠ En producción: Level.NONE en release (¡los logs filtran datos sensibles!)
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // INTERCEPTOR DE HEADERS: así se añade un token de auth a TODA petición.
    // En una app real el token vendría de DataStore tras hacer login.
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val peticionConHeaders = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer token-de-ejemplo-123")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(peticionConHeaders)
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)          // el logging SIEMPRE al final (ve todo)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: ApiAvanzada by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(okHttp)               // ← aquí conectamos OkHttp con Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiAvanzada::class.java)
    }
}

// ============================================================
// --- 4. VIEWMODEL: cada demo maneja Response y errores ---
// ============================================================
class RedAvanzadaViewModel : ViewModel() {

    private val _salida = MutableStateFlow("Pulsa una demo y mira también Logcat (filtro: okhttp)")
    val salida = _salida.asStateFlow()

    private val api = ClienteAvanzado.api

    fun demoPathExitoso() = ejecutar {
        // Response<T>: acceso al código HTTP + body
        val respuesta = api.obtenerPost(5)
        if (respuesta.isSuccessful) {
            val post = respuesta.body()
            "HTTP ${respuesta.code()} ✔\n@Path → GET /posts/5\n\nTítulo: ${post?.titulo}"
        } else {
            "HTTP ${respuesta.code()} ✖"
        }
    }

    fun demoError404() = ejecutar {
        // Un id que NO existe: el servidor responde 404.
        // Con Response<T> el 404 NO lanza excepción: lo manejas tú.
        val respuesta = api.obtenerPost(99999)
        if (respuesta.isSuccessful) "No debería pasar"
        else "HTTP ${respuesta.code()} ✖ (esperado)\n\n" +
                "El post 99999 no existe. isSuccessful=false y tú decides qué " +
                "mostrar al usuario. Sin Response<T> esto sería una excepción."
    }

    fun demoQuery() = ejecutar {
        val comentarios = api.obtenerComentarios(postId = 3)
        "@Query → GET /comments?postId=3\n\n" +
                "Llegaron ${comentarios.size} comentarios. Primero:\n" +
                "${comentarios.first().email}: ${comentarios.first().cuerpo.take(60)}..."
    }

    fun demoPost() = ejecutar {
        val respuesta = api.crearPost(
            NuevoPost(titulo = "Mi primer POST", cuerpo = "Enviado desde la Academia Android")
        )
        "HTTP ${respuesta.code()} ✔ (201 = Created)\n@POST con @Body\n\n" +
                "El servidor asignó el id=${respuesta.body()?.id}.\n" +
                "Gson convirtió tu data class a JSON automáticamente."
    }

    // Helper: todas las demos comparten el manejo de carga y errores
    private fun ejecutar(bloque: suspend () -> String) {
        viewModelScope.launch {
            _salida.value = "⏳ Llamando a la API..."
            _salida.value = try {
                bloque()
            } catch (e: Exception) {
                // Sin internet, timeout, DNS caído... SIEMPRE envuelve en try/catch
                "✖ Excepción: ${e.javaClass.simpleName}\n${e.message}"
            }
        }
    }
}

// ============================================================
// --- 5. LA PANTALLA ---
// ============================================================
@Composable
fun PantallaRedAvanzada(vm: RedAvanzadaViewModel = viewModel()) {
    val salida by vm.salida.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Retrofit Avanzado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Interceptores (auth + logging), URLs dinámicas, POST y códigos HTTP. " +
                    "Abre Logcat con el filtro 'okhttp' para ver las peticiones reales.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.demoPathExitoso() }, modifier = Modifier.weight(1f)) {
                Text("@Path 200")
            }
            Button(onClick = { vm.demoError404() }, modifier = Modifier.weight(1f)) {
                Text("404 ✖")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.demoQuery() }, modifier = Modifier.weight(1f)) {
                Text("@Query")
            }
            Button(onClick = { vm.demoPost() }, modifier = Modifier.weight(1f)) {
                Text("@POST")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                salida,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Card {
            Column(Modifier.padding(14.dp)) {
                Text("📌 Chuleta senior", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                BulletPoint("Token de auth → interceptor, NUNCA repetido en cada llamada")
                BulletPoint("Logging solo en debug (filtra datos sensibles)")
                BulletPoint("Response<T> cuando te importa el código HTTP")
                BulletPoint("T directo cuando solo importa el dato (lanza excepción si falla)")
                BulletPoint("try/catch SIEMPRE: la red puede fallar en cualquier momento")
            }
        }
    }
}
