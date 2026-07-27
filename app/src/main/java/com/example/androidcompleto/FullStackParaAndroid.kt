package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FULL-STACK KOTLIN: PRODUCCIÓN Y WEB — PISTA BACKEND (Lección S2)
 *
 * La lección S1 levantó un servidor real. Aquí ves lo que le falta para
 * ser producción (base de datos, autenticación, despliegue) y cómo se
 * completa el círculo con el FRONTEND WEB, también en Kotlin.
 *
 * EL SUEÑO DE UN SOLO LENGUAJE, HOY REAL:
 *
 *     ┌───────────────────────────────────────────────┐
 *     │           shared/ (commonMain)                │
 *     │  modelos @Serializable · validación · lógica  │
 *     └───────────────────────────────────────────────┘
 *        ▲         ▲          ▲          ▲         ▲
 *     Android     iOS      Desktop      Web      Servidor
 *     Compose   Compose    Compose   Compose/JS   Ktor
 *
 * Un data class. Cinco destinos. Cero duplicación, cero desincronización.
 */

@Composable
fun PantallaFullStack() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Full-stack Kotlin", style = MaterialTheme.typography.headlineSmall)
            Text(
                "De un servidor de juguete a uno de producción, y el frontend web " +
                        "cerrando el círculo. Todo en Kotlin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // ============ ESTRUCTURA DEL PROYECTO ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("1. La estructura de un proyecto full-stack",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "settings.gradle.kts — un repo, cuatro módulos",
                        """
                        mi-proyecto/
                        ├── shared/          ← KMP: modelos, validación, casos de uso
                        │   └── commonMain/  ← lo importan TODOS los demás
                        ├── server/          ← JVM: Ktor Server + Exposed
                        │   └── main/kotlin/Application.kt
                        ├── composeApp/      ← Android + iOS + Desktop (Compose MP)
                        │   ├── commonMain/  ← la UI compartida
                        │   ├── androidMain/
                        │   └── iosMain/
                        └── web/             ← Kotlin/Wasm o Kotlin/JS

                        // El server depende del shared, igual que la app:
                        // server/build.gradle.kts
                        dependencies { implementation(projects.shared) }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Punto clave: `shared` NO puede usar APIs de Android ni de JVM " +
                                "exclusivas. Es Kotlin común: por eso Ktor, kotlinx.serialization " +
                                "y Koin son los que valen (lecciones M2 y M3).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // ============ BASE DE DATOS ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("2. Base de datos: Exposed",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "En S1 guardábamos en una lista en memoria. Exposed es el ORM " +
                                "oficial de JetBrains: SQL con tipos, sin strings mágicos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "server/db/Tareas.kt",
                        """
                        object Tareas : IntIdTable() {          // define la tabla
                            val titulo = varchar("titulo", 60)
                            val hecha  = bool("hecha").default(false)
                            val creada = timestamp("creada")
                        }

                        class TareaRepo {
                            // newSuspendedTransaction: no bloquea el hilo (corrutinas)
                            suspend fun todas(): List<Tarea> = newSuspendedTransaction {
                                Tareas.selectAll().map { it.aTarea() }
                            }

                            suspend fun crear(nueva: NuevaTarea): Tarea = newSuspendedTransaction {
                                val id = Tareas.insertAndGetId {
                                    it[titulo] = nueva.titulo
                                    it[creada] = Clock.System.now()
                                }
                                Tarea(id.value, nueva.titulo)
                            }

                            // La consulta es TYPE-SAFE: 'Tareas.hecha eq true' no compila
                            // si te equivocas de columna o de tipo.
                            suspend fun pendientes() = newSuspendedTransaction {
                                Tareas.selectAll().where { Tareas.hecha eq false }.map { it.aTarea() }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Exposed: ORM oficial, DSL type-safe sobre SQL")
                    BulletPoint("HikariCP para el pool de conexiones")
                    BulletPoint("Flyway o Liquibase para migraciones de esquema")
                    BulletPoint("Alternativa KMP: SQLDelight (escribes SQL, genera Kotlin)")
                }
            }
        }

        // ============ AUTENTICACIÓN ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("3. Autenticación con JWT",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "server/Seguridad.kt",
                        """
                        install(Authentication) {
                            jwt("auth-jwt") {
                                verifier(
                                    JWT.require(Algorithm.HMAC256(secreto))
                                        .withAudience(audiencia)
                                        .withIssuer(emisor)
                                        .build()
                                )
                                validate { credencial ->
                                    if (credencial.payload.getClaim("usuario").asString() != "")
                                        JWTPrincipal(credencial.payload) else null
                                }
                            }
                        }

                        routing {
                            post("/login") {
                                val datos = call.receive<Credenciales>()
                                val usuario = repo.autenticar(datos)
                                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                                val token = JWT.create()
                                    .withAudience(audiencia)
                                    .withClaim("usuario", usuario.nombre)
                                    .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
                                    .sign(Algorithm.HMAC256(secreto))
                                call.respond(TokenResponse(token))
                            }

                            authenticate("auth-jwt") {          // ← todo lo de dentro exige token
                                get("/perfil") {
                                    val principal = call.principal<JWTPrincipal>()
                                    call.respond(principal!!.payload.getClaim("usuario").asString())
                                }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Y en el cliente Ktor, el token se adjunta solo",
                        """
                        HttpClient {
                            install(Auth) {
                                bearer {
                                    loadTokens { BearerTokens(almacen.token, almacen.refresh) }
                                    refreshTokens {                    // se renueva solo al caducar
                                        val nuevo: TokenResponse = client.post("refresh").body()
                                        BearerTokens(nuevo.token, nuevo.refresh)
                                    }
                                }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠ NUNCA guardes el secreto JWT en el código. Va en variables de " +
                                "entorno o en un gestor de secretos. Y el token, en el cliente, " +
                                "en almacenamiento cifrado — no en DataStore en claro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ============ FRONTEND WEB ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("4. Frontend web, también en Kotlin",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hay dos caminos, y eliges según lo que necesites:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))

                    Text("A) Compose Multiplatform para Web (Kotlin/Wasm)",
                        style = MaterialTheme.typography.titleSmall)
                    BulletPoint("La MISMA UI Compose que ya sabes, compilada a WebAssembly")
                    BulletPoint("Ideal si quieres una sola UI para móvil, escritorio y web")
                    BulletPoint("Dibuja sobre canvas: no genera HTML semántico")
                    BulletPoint("Contra: bundle grande y SEO/accesibilidad web limitados")
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "web/src/wasmJsMain/kotlin/main.kt",
                        """
                        fun main() {
                            ComposeViewport(document.body!!) {
                                App()        // ¡el MISMO @Composable de tu app Android!
                            }
                        }
                        """.trimIndent()
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("B) Kotlin/JS con HTML (kotlinx.html o React)",
                        style = MaterialTheme.typography.titleSmall)
                    BulletPoint("Genera HTML/DOM real: bueno para SEO y webs de contenido")
                    BulletPoint("Puedes usar React desde Kotlin con los wrappers oficiales")
                    BulletPoint("Compartes modelos y lógica, pero la UI se escribe aparte")
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Compose HTML (antes 'Compose for Web')",
                        """
                        renderComposable(rootElementId = "root") {
                            var tareas by remember { mutableStateOf(emptyList<Tarea>()) }
                            LaunchedEffect(Unit) { tareas = api.listar() }  // shared!

                            Ul {
                                tareas.forEach { t ->
                                    Li { Text(t.titulo) }        // esto SÍ es un <li> real
                                }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Regla práctica: si es una APLICACIÓN (dashboard, herramienta interna) " +
                                "usa Compose/Wasm y reaprovechas la UI entera. Si es una WEB " +
                                "pública que necesita SEO, usa Kotlin/JS con HTML y comparte " +
                                "solo la lógica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // ============ TESTING DEL SERVIDOR ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("5. Testear el servidor sin levantarlo",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "server/src/test/kotlin/RutasTest.kt",
                        """
                        @Test
                        fun `GET tareas devuelve 200 y la lista`() = testApplication {
                            application { modulo() }        // tu módulo Ktor real

                            val cliente = createClient {
                                install(ContentNegotiation) { json() }
                            }
                            val respuesta = cliente.get("/tareas")

                            assertEquals(HttpStatusCode.OK, respuesta.status)
                            assertTrue(respuesta.body<List<Tarea>>().isNotEmpty())
                        }

                        @Test
                        fun `POST con titulo corto devuelve 400`() = testApplication {
                            application { modulo() }
                            val cliente = createClient {
                                install(ContentNegotiation) { json() }
                            }
                            val r = cliente.post("/tareas") {
                                contentType(ContentType.Application.Json)
                                setBody(NuevaTarea(titulo = "ab"))   // modelo compartido
                            }
                            assertEquals(HttpStatusCode.BadRequest, r.status)
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "testApplication levanta el servidor EN MEMORIA: sin puertos, sin " +
                                "esperas, milisegundos por test. Es el equivalente servidor de " +
                                "lo que hiciste con runTest en la lección K6.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // ============ DESPLIEGUE ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("6. Llevarlo a producción",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Dockerfile del servidor",
                        """
                        # 1) Construir el fat-jar
                        FROM gradle:8-jdk21 AS build
                        COPY . /app
                        WORKDIR /app
                        RUN gradle :server:buildFatJar --no-daemon

                        # 2) Imagen final, ligera
                        FROM eclipse-temurin:21-jre-alpine
                        COPY --from=build /app/server/build/libs/*-all.jar /app/server.jar
                        EXPOSE 8080
                        ENTRYPOINT ["java","-jar","/app/server.jar"]
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Ktor plugin: buildFatJar genera un jar autocontenido")
                    BulletPoint("Motor en producción: Netty (CIO está bien, Netty está más probado)")
                    BulletPoint("Configuración por variables de entorno, nunca en el código")
                    BulletPoint("Dónde: Railway, Fly.io, Render, Google Cloud Run, cualquier VPS")
                    BulletPoint("Añade CallLogging, CORS y Compression como plugins")
                    BulletPoint("Métricas con Micrometer y health checks en /salud")
                }
            }
        }

        // ============ CHULETA ============
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta full-stack", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("shared/ con los modelos = el cliente no compila si el API cambia")
                    BulletPoint("Valida en el cliente (UX) Y en el servidor (seguridad), misma función")
                    BulletPoint("Exposed para SQL type-safe; SQLDelight si quieres compartir con KMP")
                    BulletPoint("JWT: secreto en variables de entorno, token en almacén cifrado")
                    BulletPoint("testApplication para probar rutas sin abrir puertos")
                    BulletPoint("Compose/Wasm si es una app; Kotlin/JS + HTML si necesitas SEO")
                    BulletPoint("Fat-jar + Docker y a cualquier proveedor")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Con esto cierras el círculo: el mismo lenguaje y los mismos modelos " +
                                "desde la base de datos hasta el botón que pulsa el usuario, en " +
                                "Android, iOS, escritorio y web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
