package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * GUÍA MAESTRA DE CORRUTINAS Y FLOWS
 * Las corrutinas son la forma de hacer trabajo asíncrono en Kotlin sin bloquear
 * el hilo principal (donde se dibuja la UI). Es el equivalente a async/await
 * de otros lenguajes, pero mucho más potente.
 *
 * CONCEPTOS CLAVE:
 * - suspend fun: función que puede "pausarse" sin bloquear el hilo.
 * - launch: dispara una corrutina "fire and forget" (no devuelve resultado).
 * - async/await: dispara corrutinas en PARALELO y espera sus resultados.
 * - Dispatchers: en qué hilo se ejecuta (Main = UI, IO = red/disco, Default = CPU).
 * - Flow: un stream de valores que llegan con el tiempo (como un río de datos).
 */

// --- 1. FUNCIONES SUSPEND ---
// "suspend" marca que esta función puede tardar (red, disco, cálculo pesado).
// Solo se puede llamar desde una corrutina u otra función suspend.
suspend fun descargarDato(nombre: String, tardanzaMs: Long): String {
    // withContext cambia de hilo: Dispatchers.IO es el pool para red/disco.
    return withContext(Dispatchers.IO) {
        delay(tardanzaMs) // delay pausa SIN bloquear el hilo (a diferencia de Thread.sleep)
        "Dato '$nombre' descargado en ${tardanzaMs}ms"
    }
}

// --- 2. EJECUCIÓN SECUENCIAL VS PARALELA ---
// Secuencial: una tras otra (total = suma de tiempos)
suspend fun descargaSecuencial(log: (String) -> Unit) {
    val inicio = System.currentTimeMillis()
    val a = descargarDato("A", 1000)
    val b = descargarDato("B", 1000)
    log(a)
    log(b)
    log("SECUENCIAL: tardó ${System.currentTimeMillis() - inicio}ms (≈2000)")
}

// Paralela con async: las dos a la vez (total = la más lenta)
suspend fun descargaParalela(log: (String) -> Unit) = coroutineScope {
    val inicio = System.currentTimeMillis()
    // async lanza la corrutina YA, pero no espera todavía
    val a = async { descargarDato("A", 1000) }
    val b = async { descargarDato("B", 1000) }
    // await() es donde realmente esperamos el resultado
    log(a.await())
    log(b.await())
    log("PARALELO: tardó ${System.currentTimeMillis() - inicio}ms (≈1000)")
}

// --- 3. FLOW: STREAMS DE DATOS ---
// Un Flow emite varios valores a lo largo del tiempo. Es "frío": no hace nada
// hasta que alguien lo colecta (collect).
fun contadorFlow() = flow {
    for (i in 1..5) {
        delay(400)
        emit(i) // emit envía un valor al colector
    }
}

// Los Flows se transforman con OPERADORES (igual que las listas):
suspend fun demoOperadoresFlow(log: (String) -> Unit) {
    contadorFlow()
        .filter { it % 2 != 0 }        // deja pasar solo impares: 1, 3, 5
        .map { "Número transformado: ${it * 10}" } // los convierte: 10, 30, 50
        .collect { valor -> log(valor) } // collect es la terminal: aquí llegan
    log("Flow completado ✔")
}

// --- PANTALLA INTERACTIVA ---
@Composable
fun PantallaCorrutinas() {
    // rememberCoroutineScope: un scope atado a este Composable.
    // Si el Composable sale de pantalla, las corrutinas se CANCELAN solas.
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var ocupado by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll al final cuando llega un log nuevo
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    fun ejecutar(demo: suspend ((String) -> Unit) -> Unit) {
        scope.launch {
            ocupado = true
            demo { mensaje -> logs.add(mensaje) }
            ocupado = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Corrutinas y Flows", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pulsa cada demo y observa el log. Fíjate en los tiempos: " +
                    "secuencial suma, paralelo no.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !ocupado, onClick = { ejecutar { descargaSecuencial(it) } }) {
                Text("Secuencial")
            }
            Button(enabled = !ocupado, onClick = { ejecutar { descargaParalela(it) } }) {
                Text("Paralelo")
            }
            Button(enabled = !ocupado, onClick = { ejecutar { demoOperadoresFlow(it) } }) {
                Text("Flow")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (ocupado) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Trabajando en segundo plano (la UI sigue viva)...")
            } else {
                TextButton(onClick = { logs.clear() }) { Text("Limpiar log") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { linea ->
                    Text("> $linea", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
