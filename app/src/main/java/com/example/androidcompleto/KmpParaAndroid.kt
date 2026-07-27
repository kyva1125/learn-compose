package com.example.androidcompleto

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * GUÍA MAESTRA DE KOTLIN MULTIPLATFORM — KMP (Lección 18)
 *
 * KMP = escribir la LÓGICA una vez (Kotlin) y correrla en Android, iOS,
 * Desktop, Web y servidor. Compose Multiplatform (CMP) va un paso más
 * allá: también la UI que ya sabes de esta app corre en todas partes.
 *
 * LO MÁS IMPORTANTE: TODO lo que aprendiste aquí APLICA DIRECTO.
 *   - Composables, remember, ViewModel, StateFlow, corrutinas, MVI →
 *     idénticos en KMP. No empiezas de cero: ya sabes el 80%.
 *
 * ESTRUCTURA DE UN PROYECTO KMP:
 *
 *   shared/ (o composeApp/)
 *   ├── commonMain/   ← el 80-90% de tu código: lógica, UI Compose,
 *   │                   ViewModels, repositorios, modelos
 *   ├── androidMain/  ← solo lo específico de Android (Context, permisos)
 *   ├── iosMain/      ← solo lo específico de iOS (UIKit, notificaciones)
 *   └── commonTest/   ← tests que corren en TODAS las plataformas
 *   iosApp/           ← proyecto Xcode delgado que arranca el shared
 *
 * EXPECT / ACTUAL — el puente entre mundos:
 *   En commonMain DECLARAS qué necesitas (expect) y cada plataforma
 *   lo IMPLEMENTA (actual). Es como interface/implementación, pero
 *   resuelto en COMPILACIÓN: cero costo en runtime.
 *
 * ¿QUIÉN LO USA? Netflix, McDonald's, Forbes, Cash App, Google Docs...
 * Ser "senior en Compose" hoy incluye saber llevarlo multiplataforma.
 */

// ============================================================
// --- 1. SIMULACIÓN DE EXPECT/ACTUAL (patrón real, en Android) ---
// ============================================================
// En un proyecto KMP esto sería: expect fun infoPlataforma(): InfoPlataforma
// Aquí lo simulamos con una interfaz para que VEAS el patrón funcionando.
interface InfoPlataforma {
    val nombre: String
    val version: String
}

// Esta clase viviría en androidMain (usa APIs de Android):
class InfoPlataformaAndroid : InfoPlataforma {
    override val nombre = "Android"
    override val version = "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
}
// Y en iosMain existiría la gemela:
//   class InfoPlataformaIos : InfoPlataforma {
//       override val nombre = "iOS"
//       override val version = UIDevice.currentDevice.systemVersion
//   }

// ============================================================
// --- 2. LÓGICA 100% COMPARTIBLE (viviría en commonMain) ---
// ============================================================
// Mira los imports que usa: SOLO Kotlin + coroutines. Ni una línea de
// Android. Esta clase compilaría hoy mismo para iOS sin tocarla.
class ContadorCompartido {
    private val _cuenta = MutableStateFlow(0)
    val cuenta: StateFlow<Int> = _cuenta.asStateFlow()

    fun incrementar() = _cuenta.update { it + 1 }
    fun reiniciar() { _cuenta.value = 0 }
}

// ============================================================
// --- PANTALLA DE LA LECCIÓN ---
// ============================================================
@Composable
fun PantallaKmp() {
    val plataforma = remember { InfoPlataformaAndroid() }
    val contador = remember { ContadorCompartido() }
    val cuenta by contador.cuenta.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kotlin Multiplatform", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Todo lo que aprendiste en esta app (Compose, ViewModel, Flow, MVI) " +
                        "corre igual en iOS y Desktop. Esta lección te enseña el mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // --- Demo expect/actual ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. expect / actual en acción", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Este dato viene de la implementación 'actual' de TU plataforma:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "🤖 ${plataforma.nombre} — ${plataforma.version}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    BloqueCodigo(
                        "Así se escribe en un proyecto KMP real",
                        """
                        // commonMain: DECLARO lo que necesito
                        expect fun infoPlataforma(): InfoPlataforma

                        // androidMain: lo IMPLEMENTO con Android
                        actual fun infoPlataforma() = InfoPlataformaAndroid()

                        // iosMain: lo IMPLEMENTO con UIKit
                        actual fun infoPlataforma() = InfoPlataformaIos()
                        """.trimIndent()
                    )
                }
            }
        }

        // --- Demo lógica compartida ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Lógica compartida de verdad", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ContadorCompartido usa SOLO Kotlin + StateFlow (mira sus imports: " +
                                "cero Android). En un proyecto KMP iría en commonMain y este " +
                                "mismo botón existiría en iPhone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { contador.incrementar() }) { Text("Incrementar") }
                        OutlinedButton(onClick = { contador.reiniciar() }) { Text("Reiniciar") }
                        Text("Cuenta: $cuenta", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // --- Mapa de librerías ---
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("3. Tu stack Android → su gemelo KMP", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Retrofit → Ktor Client (HTTP multiplataforma, de JetBrains)")
                    BulletPoint("Gson → kotlinx.serialization (JSON oficial de Kotlin)")
                    BulletPoint("Room → Room KMP (¡ya es multiplataforma!) o SQLDelight")
                    BulletPoint("Hilt → Koin o kotlin-inject (Hilt NO funciona en iOS)")
                    BulletPoint("Coil 3 → Coil 3 (el que usa esta app ya es KMP 🎉)")
                    BulletPoint("DataStore → DataStore KMP (también ya es multiplataforma)")
                    BulletPoint("ViewModel + Navigation Compose → versiones JetBrains en CMP")
                    BulletPoint("Fechas: java.time → kotlinx-datetime")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fíjate el patrón: el ecosistema ENTERO ya migró o tiene gemelo. " +
                                "El conocimiento de las lecciones 9-13 se recicla completo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // --- Compose Multiplatform ---
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("4. Compose Multiplatform (CMP)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "CMP es Jetpack Compose mantenido por JetBrains para todas las " +
                                "plataformas. iOS es estable desde 2025. La MISMA función " +
                                "@Composable pinta en Android, iPhone, Mac, Windows y Web.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "App.kt en commonMain: UI compartida al 100%",
                        """
                        @Composable
                        fun App() {                  // ¡esto corre en 5 plataformas!
                            MaterialTheme {
                                var contador by remember { mutableStateOf(0) }
                                Button(onClick = { contador++ }) {
                                    Text("Clicks: " + contador)
                                }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dos estrategias de adopción:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BulletPoint("Compartir SOLO lógica: UI nativa en cada lado (SwiftUI en iOS)")
                    BulletPoint("Compartir TODO con CMP: una sola UI Compose para todos")
                }
            }
        }

        // --- Cómo empezar ---
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Tu ruta para entrar a KMP", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("1. Genera un proyecto en kmp.jetbrains.com (el wizard oficial)")
                    BulletPoint("2. Reconoce la estructura: commonMain / androidMain / iosMain")
                    BulletPoint("3. Migra tu dominio (como la Lección 17: era Kotlin puro, entra directo)")
                    BulletPoint("4. Cambia Retrofit por Ktor y Gson por kotlinx.serialization")
                    BulletPoint("5. DI con Koin y ViewModels compartidos en commonMain")
                    BulletPoint("6. Al final, la UI: pantallas Compose a commonMain una por una")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "El orden importa: primero lógica (riesgo bajo), al final UI. Así " +
                                "migran las empresas grandes sus apps reales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
