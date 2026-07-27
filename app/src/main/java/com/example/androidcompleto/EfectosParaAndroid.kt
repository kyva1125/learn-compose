package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

/**
 * GUÍA MAESTRA DE EFECTOS SECUNDARIOS (Side Effects)
 * Este es EL tema que separa a un principiante de un experto en Compose.
 *
 * Un Composable puede recomponerse MUCHAS veces por segundo. Si pones código
 * "normal" dentro (una llamada de red, un log, un timer), se ejecutaría en
 * cada recomposición = desastre. Los efectos controlan CUÁNDO se ejecuta:
 *
 * - LaunchedEffect(key)   -> corrutina que corre al ENTRAR (y se cancela al salir).
 *                            Si 'key' cambia, se reinicia.
 * - DisposableEffect      -> para recursos que hay que LIMPIAR (listeners, sensores).
 * - derivedStateOf        -> estado CALCULADO de otro estado (optimiza recomposiciones).
 * - snapshotFlow          -> convierte estado de Compose en un Flow (para operadores).
 * - rememberUpdatedState  -> captura el valor MÁS RECIENTE dentro de un efecto largo.
 */

@Composable
fun PantallaEfectos() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Efectos Secundarios", style = MaterialTheme.typography.headlineSmall)

        DemoLaunchedEffect()
        DemoDisposableEffect()
        DemoDerivedStateOf()
        DemoSnapshotFlow()
        TarjetaTeoriaEfectos()
    }
}

// ============================================================
// --- 1. LaunchedEffect: el cronómetro perfecto ---
// ============================================================
@Composable
fun DemoLaunchedEffect() {
    var corriendo by remember { mutableStateOf(false) }
    var segundos by remember { mutableIntStateOf(0) }

    // La CLAVE es 'corriendo': cuando cambia, el efecto anterior SE CANCELA
    // y arranca uno nuevo. Al salir de la pantalla, se cancela solo.
    LaunchedEffect(corriendo) {
        while (corriendo) {
            delay(1000)
            segundos++
        }
    }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("1. LaunchedEffect (cronómetro)", style = MaterialTheme.typography.titleMedium)
            Text(
                "La corrutina vive atada al Composable: sal de esta pantalla y muere sola. " +
                        "Sin fugas de memoria, sin timers zombis.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⏱ $segundos s", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { corriendo = !corriendo }) {
                    Text(if (corriendo) "Pausar" else "Iniciar")
                }
                OutlinedButton(onClick = { corriendo = false; segundos = 0 }) {
                    Text("Reset")
                }
            }
        }
    }
}

// ============================================================
// --- 2. DisposableEffect: montar y LIMPIAR recursos ---
// ============================================================
@Composable
fun DemoDisposableEffect() {
    var mostrarHijo by remember { mutableStateOf(false) }
    val eventos = remember { mutableStateListOf<String>() }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("2. DisposableEffect (ciclo de vida)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Simula un listener (GPS, sensor, broadcast): al montarse se REGISTRA " +
                        "y al desmontarse se LIBERA en onDispose. Actívalo y desactívalo.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = mostrarHijo, onCheckedChange = { mostrarHijo = it })
                Spacer(Modifier.width(8.dp))
                Text(if (mostrarHijo) "Componente montado" else "Componente desmontado")
            }

            if (mostrarHijo) {
                ComponenteConRecurso(onEvento = { eventos.add(it) })
            }

            Spacer(Modifier.height(8.dp))
            eventos.takeLast(4).forEach {
                Text("> $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ComponenteConRecurso(onEvento: (String) -> Unit) {
    // DisposableEffect DEBE terminar con onDispose { }: ahí liberas el recurso.
    DisposableEffect(Unit) {
        onEvento("🟢 Recurso REGISTRADO (ej: sensor.register())")
        onDispose {
            onEvento("🔴 Recurso LIBERADO (ej: sensor.unregister())")
        }
    }
    Text("📡 Escuchando el sensor...", modifier = Modifier.padding(top = 4.dp))
}

// ============================================================
// --- 3. derivedStateOf: estado calculado eficiente ---
// ============================================================
@Composable
fun DemoDerivedStateOf() {
    var texto by remember { mutableStateOf("") }

    // MAL: calcularlo directo recompone TODO en cada tecla.
    // BIEN: derivedStateOf solo notifica cuando el RESULTADO cambia
    // (pasar de 4 a 5 palabras), no en cada letra.
    val palabras by remember {
        derivedStateOf {
            texto.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        }
    }
    val esLargo by remember { derivedStateOf { palabras > 5 } }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("3. derivedStateOf (contador de palabras)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Escribe una frase") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Palabras: $palabras ${if (esLargo) "— ¡frase larga! ✍" else ""}")
        }
    }
}

// ============================================================
// --- 4. snapshotFlow: de estado Compose a Flow ---
// ============================================================
@Composable
fun DemoSnapshotFlow() {
    var contador by remember { mutableIntStateOf(0) }
    var logros by remember { mutableStateOf(listOf<String>()) }

    // snapshotFlow observa el estado como un Flow: puedes usar filter,
    // debounce, distinctUntilChanged... aquí solo "escuchamos" múltiplos de 5.
    LaunchedEffect(Unit) {
        snapshotFlow { contador }
            .filter { it > 0 && it % 5 == 0 }
            .collect { hito ->
                logros = logros + "🏆 ¡Alcanzaste $hito clics!"
            }
    }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("4. snapshotFlow (logros cada 5 clics)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { contador++ }) { Text("Clics: $contador") }
            logros.takeLast(3).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ============================================================
// --- 5. Chuleta final ---
// ============================================================
@Composable
fun TarjetaTeoriaEfectos() {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("📌 Chuleta: ¿cuál uso?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BulletPoint("Cargar datos al entrar → LaunchedEffect(Unit)")
            BulletPoint("Reaccionar a un cambio de id → LaunchedEffect(id)")
            BulletPoint("Registrar/liberar listener → DisposableEffect + onDispose")
            BulletPoint("Estado calculado de otro → derivedStateOf")
            BulletPoint("Operadores de Flow sobre estado → snapshotFlow")
            BulletPoint("Callback fresco en efecto largo → rememberUpdatedState")
            Spacer(Modifier.height(4.dp))
            Text(
                "Regla de oro: NUNCA hagas trabajo (red, disco, logs) directo en el " +
                        "cuerpo de un @Composable. Siempre dentro de un efecto o un ViewModel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
