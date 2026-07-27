package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * CORRUTINAS Y FLOWS AVANZADOS — PISTA KOTLIN (Lección K4)
 *
 * La lección 4 te enseñó launch/async/suspend. Esta te enseña lo que
 * de verdad preguntan en una entrevista senior y lo que rompe apps en producción:
 *
 * 1. CONCURRENCIA ESTRUCTURADA: toda corrutina tiene un padre. Si el padre
 *    muere, los hijos mueren. Nunca hay corrutinas "huérfanas" filtrándose.
 * 2. CANCELACIÓN COOPERATIVA: cancelar NO mata el hilo. Solo funciona si tu
 *    código la comprueba (isActive, ensureActive, o llamando a otra suspend).
 * 3. EXCEPCIONES: en coroutineScope un hijo que falla cancela a TODOS los
 *    hermanos. En supervisorScope, cada hijo falla por su cuenta.
 * 4. OPERADORES DE FLOW: flatMapLatest, combine, debounce, retry, catch...
 *    son la diferencia entre "funciona" y "funciona bien bajo estrés".
 *
 * Cada botón EJECUTA corrutinas de verdad y va escribiendo en el registro.
 */

class CorrutinasAvanzadasViewModel : ViewModel() {

    private val _registro = MutableStateFlow<List<String>>(emptyList())
    val registro = _registro.asStateFlow()

    private val _ocupado = MutableStateFlow(false)
    val ocupado = _ocupado.asStateFlow()

    private fun log(linea: String) = _registro.update { it + linea }

    fun limpiar() = _registro.update { emptyList() }

    // El bloque recibe el CoroutineScope como receptor: así las demos pueden
    // usar launch/async directamente (una corrutina SIEMPRE necesita su scope).
    private fun demo(titulo: String, bloque: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            _ocupado.value = true
            log("▶ $titulo")
            try {
                bloque()
            } catch (e: Exception) {
                log("  ⚠ escapó al scope: ${e::class.simpleName}: ${e.message}")
            }
            log("─".repeat(28))
            _ocupado.value = false
        }
    }

    // ========================================================
    // 1. CONCURRENCIA ESTRUCTURADA: secuencial vs paralelo
    // ========================================================
    fun demoParalelo() = demo("Estructurada: secuencial vs paralelo") {
        suspend fun tarea(nombre: String, ms: Long): String {
            delay(ms)
            return nombre
        }

        val tSecuencial = System.currentTimeMillis()
        tarea("A", 300); tarea("B", 300)
        val secuencial = System.currentTimeMillis() - tSecuencial
        log("  Secuencial (una tras otra): ${secuencial}ms")

        val tParalelo = System.currentTimeMillis()
        // coroutineScope espera a que TODOS los hijos terminen antes de seguir
        coroutineScope {
            val a = async { tarea("A", 300) }
            val b = async { tarea("B", 300) }
            log("  async lanzados, esperando await...")
            a.await(); b.await()
        }
        val paralelo = System.currentTimeMillis() - tParalelo
        log("  Paralelo (async + await): ${paralelo}ms  ← la mitad")
    }

    // ========================================================
    // 2. CANCELACIÓN COOPERATIVA
    // ========================================================
    fun demoCancelacion() = demo("Cancelación cooperativa") {
        val job = launch {
            try {
                repeat(10) { i ->
                    // delay() es "cancellable": comprueba la cancelación por ti
                    delay(100)
                    log("  trabajando... paso $i")
                }
            } finally {
                // OJO: aquí ya no puedes suspender salvo con NonCancellable
                withContext(NonCancellable) {
                    delay(20)
                    log("  🧹 finally: liberando recursos (NonCancellable)")
                }
            }
        }
        delay(320)
        log("  cancelando el job...")
        job.cancelAndJoin()
        log("  job.isCancelled = ${job.isCancelled}")

        // Un bucle de CPU NO se cancela solo: hay que comprobarlo a mano
        val jobCpu = launch(Dispatchers.Default) {
            var i = 0
            while (isActive) {   // ← sin este isActive, seguiría corriendo
                i++
            }
            log("  bucle CPU salió limpio tras $i vueltas (isActive)")
        }
        delay(50)
        jobCpu.cancelAndJoin()
    }

    // ========================================================
    // 3. EXCEPCIONES: coroutineScope vs supervisorScope
    // ========================================================
    fun demoExcepciones() = demo("coroutineScope vs supervisorScope") {
        // (a) coroutineScope: si UN hijo falla, cancela a sus hermanos
        try {
            coroutineScope {
                launch {
                    delay(200)
                    log("  [coroutineScope] hermano sano... ¿llegará?")
                }
                launch {
                    delay(50)
                    throw IllegalStateException("fallo en hijo 1")
                }
            }
        } catch (e: IllegalStateException) {
            log("  [coroutineScope] ✖ ${e.message} → el hermano fue CANCELADO")
        }

        // (b) supervisorScope: cada hijo falla de forma independiente
        supervisorScope {
            val manejador = CoroutineExceptionHandler { _, e ->
                log("  [supervisorScope] ✖ capturado: ${e.message}")
            }
            launch(manejador) {
                delay(50)
                throw IllegalStateException("fallo en hijo 1")
            }
            launch {
                delay(200)
                log("  [supervisorScope] ✔ el hermano SÍ terminó")
            }
        }

        // (c) async guarda la excepción hasta que haces await()
        val diferido = async { throw RuntimeException("error dentro de async") }
        try {
            diferido.await()
        } catch (e: RuntimeException) {
            log("  [async] la excepción salta en await(): ${e.message}")
        }
    }

    // ========================================================
    // 4. FLOW: flatMapLatest — el patrón del buscador
    // ========================================================
    fun demoFlatMapLatest() = demo("flatMapLatest: cancelar la búsqueda vieja") {
        val teclas = flow {
            listOf("k", "ko", "kot", "kotl", "kotlin").forEach {
                emit(it); delay(80)
            }
        }

        suspend fun buscar(q: String): String {
            delay(150)          // simula la red: más lenta que el tecleo
            return "resultados de '$q'"
        }

        // flatMapLatest CANCELA la búsqueda anterior cuando llega una tecla nueva
        teclas
            .flatMapLatest { q -> flow { emit(buscar(q)) } }
            .collect { log("  flatMapLatest → $it") }

        log("  ↑ solo llegó la ÚLTIMA: las demás se cancelaron solas")
    }

    // ========================================================
    // 5. FLOW: debounce, distinctUntilChanged, combine
    // ========================================================
    fun demoOperadores() = demo("debounce · distinctUntilChanged · combine") {
        val tecleo = flow {
            emit("a"); delay(50)
            emit("ab"); delay(50)
            emit("ab"); delay(50)      // repetido
            emit("abc"); delay(400)    // pausa larga: aquí sí dispara
            emit("abcd"); delay(400)
        }

        tecleo
            .distinctUntilChanged()    // ignora repetidos consecutivos
            .debounce(200)             // espera 200ms de silencio antes de emitir
            .collect { log("  debounce(200) → '$it'") }

        // combine: un flujo nuevo cada vez que CUALQUIERA de los dos emite
        val nombre = flowOf("José", "Ana").onEach { delay(60) }
        val edad = flowOf(30, 31).onEach { delay(90) }
        combine(nombre, edad) { n, e -> "$n tiene $e" }
            .collect { log("  combine → $it") }
    }

    // ========================================================
    // 6. FLOW: retry y catch (resiliencia de red)
    // ========================================================
    fun demoRetryCatch() = demo("retry + catch: red poco fiable") {
        var intento = 0
        val apiInestable = flow {
            intento++
            log("  intento #$intento...")
            if (intento < 3) throw java.io.IOException("timeout de red")
            emit("✔ datos recibidos al intento $intento")
        }

        apiInestable
            .retry(retries = 3) { causa ->
                val reintentar = causa is java.io.IOException
                if (reintentar) log("  ↻ reintentando tras ${causa.message}")
                delay(100)               // backoff
                reintentar               // solo reintenta errores de red
            }
            .catch { e -> emit("✖ me rendí: ${e.message}") }   // último recurso
            .collect { log("  $it") }
    }

    // ========================================================
    // 7. StateFlow vs SharedFlow (estado vs eventos)
    // ========================================================
    fun demoStateVsShared() = demo("StateFlow (estado) vs SharedFlow (eventos)") {
        val estado = MutableStateFlow("inicial")
        estado.value = "valor 1"
        estado.value = "valor 2"
        // Un colector nuevo recibe SOLO el último valor (es un "estado")
        log("  StateFlow: un colector nuevo lee '${estado.value}' (se perdió 'valor 1')")

        val eventos = MutableSharedFlow<String>(replay = 0)
        val recibidos = mutableListOf<String>()
        val trabajo = launch { eventos.collect { recibidos += it } }
        delay(30)                                  // dar tiempo a suscribirse
        eventos.emit("navegar a detalle")
        eventos.emit("mostrar snackbar")
        delay(30)
        trabajo.cancel()
        log("  SharedFlow (replay=0): recibidos $recibidos")
        log("  → StateFlow para ESTADO de pantalla; SharedFlow para EVENTOS de una vez")
    }

    // ========================================================
    // 8. buffer / conflate: productor rápido, consumidor lento
    // ========================================================
    fun demoContrapresion() = demo("Contrapresión: buffer vs conflate") {
        suspend fun consumirLento(x: Int) { delay(120) }

        val rapido = flow { repeat(5) { emit(it); delay(30) } }

        val t1 = System.currentTimeMillis()
        rapido.collect { consumirLento(it) }
        log("  sin operador: ${System.currentTimeMillis() - t1}ms (se esperan mutuamente)")

        val t2 = System.currentTimeMillis()
        rapido.buffer().collect { consumirLento(it) }
        log("  buffer(): ${System.currentTimeMillis() - t2}ms (emite sin esperar)")

        val vistos = mutableListOf<Int>()
        rapido.conflate().collect { vistos += it; consumirLento(it) }
        log("  conflate(): solo procesó $vistos (descarta los intermedios)")
    }
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaCorrutinasAvanzadas() {
    val vm: CorrutinasAvanzadasViewModel = viewModel()
    val registro by vm.registro.collectAsState()
    val ocupado by vm.ocupado.collectAsState()

    val demos = listOf<Pair<String, () -> Unit>>(
        "1. Estructurada: paralelo" to vm::demoParalelo,
        "2. Cancelación" to vm::demoCancelacion,
        "3. Excepciones" to vm::demoExcepciones,
        "4. flatMapLatest" to vm::demoFlatMapLatest,
        "5. debounce/combine" to vm::demoOperadores,
        "6. retry + catch" to vm::demoRetryCatch,
        "7. State vs Shared" to vm::demoStateVsShared,
        "8. buffer/conflate" to vm::demoContrapresion,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Corrutinas y Flows avanzados", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cada botón ejecuta corrutinas DE VERDAD y escribe abajo lo que ocurre. " +
                        "Mira los tiempos: son reales, no simulados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                demos.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fila.forEach { (titulo, accion) ->
                            OutlinedButton(
                                onClick = accion,
                                enabled = !ocupado,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(titulo, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = vm::limpiar) { Text("🗑 Limpiar registro") }
                if (ocupado) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (registro.isEmpty()) {
                        Text(
                            "El registro aparecerá aquí. Pulsa una demo 👆",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    registro.forEach { linea ->
                        Text(
                            linea,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de entrevista", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("coroutineScope: un hijo falla → cancela a los hermanos")
                    BulletPoint("supervisorScope: cada hijo falla por su cuenta")
                    BulletPoint("Cancelar es COOPERATIVO: usa isActive/ensureActive en bucles de CPU")
                    BulletPoint("En finally que suspende → withContext(NonCancellable)")
                    BulletPoint("Buscador = debounce + distinctUntilChanged + flatMapLatest")
                    BulletPoint("Red inestable = retry(n) { it is IOException } + catch")
                    BulletPoint("StateFlow para estado (siempre tiene valor) · SharedFlow para eventos")
                    BulletPoint("Productor rápido: buffer() mantiene todo, conflate() descarta")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Todo esto está probado en CorrutinasFlowTest.kt con runTest y Turbine, " +
                                "sin esperas reales: el tiempo virtual lo controla el test.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
