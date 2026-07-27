package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * INYECCIÓN DE DEPENDENCIAS — PISTA KMP (Lección M3)
 *
 * En la lección de arquitectura usaste un contenedor manual (ContenedorApp).
 * Funciona, pero a escala real se vuelve un infierno: dependencias en cadena,
 * ciclos de vida, scopes, sustituir cosas en los tests...
 *
 * LAS DOS OPCIONES EN ANDROID, Y POR QUÉ AQUÍ ELEGIMOS KOIN:
 *
 *   HILT (Google)                      KOIN (Insert-Koin)
 *   ─────────────────────────          ─────────────────────────
 *   Genera código (KSP)                Resuelve en runtime (DSL)
 *   Errores en COMPILACIÓN ✔           Errores al arrancar
 *   Solo Android/JVM ✖                 MULTIPLATAFORMA ✔ (iOS incluido)
 *   Mucha ceremonia de anotaciones     Kotlin normal, se lee fácil
 *
 * Si tu app es solo Android, Hilt es la apuesta segura (y lo que pide la
 * mayoría de ofertas de trabajo). Si vas a KMP, Hilt NO SIRVE: no funciona
 * en iOS. Por eso el ecosistema multiplataforma usa Koin o kotlin-inject.
 *
 * Abajo tienes AMBOS: Koin funcionando de verdad y el equivalente en Hilt
 * comentado, para que sepas leer los dos.
 */

// ============================================================
// --- LAS DEPENDENCIAS DE EJEMPLO ---
// ============================================================
interface RelojApp {
    fun ahora(): String
}

class RelojReal : RelojApp {
    override fun ahora(): String {
        val ms = System.currentTimeMillis()
        return "instante $ms"
    }
}

/** Un fake para tests: la ventaja principal de inyectar por constructor. */
class RelojFijo(private val fijo: String = "2026-01-01T00:00") : RelojApp {
    override fun ahora() = fijo
}

class RegistroDeAuditoria(private val reloj: RelojApp) {
    private val entradas = mutableListOf<String>()

    fun registrar(accion: String) {
        entradas += "[${reloj.ahora()}] $accion"
    }

    fun todo(): List<String> = entradas.toList()
}

// ============================================================
// --- EL MÓDULO DE KOIN (en KMP esto va en commonMain) ---
// ============================================================
/**
 * single = una sola instancia para toda la app (singleton)
 * factory = una instancia NUEVA en cada petición
 * viewModel = ligado al ciclo de vida del ViewModel (koin-androidx-compose)
 */
val moduloDemo = module {
    // Registramos la INTERFAZ apuntando a la implementación:
    // cambiar a RelojFijo en un test es cambiar UNA línea.
    single<RelojApp> { RelojReal() }

    // get() resuelve la dependencia automáticamente
    single { RegistroDeAuditoria(reloj = get()) }

    // factory: cada llamada crea uno nuevo
    factory { PostsKtorRepository() }
}

/**
 * Arranque perezoso: normalmente esto va en tu clase Application
 * (o en un `initKoin()` compartido en commonMain para KMP).
 * Aquí lo hacemos bajo demanda para no tocar el arranque de la app.
 */
fun asegurarKoinIniciado() {
    if (GlobalContext.getOrNull() == null) {
        startKoin { modules(moduloDemo) }
    }
}

// ============================================================
// --- UN VIEWMODEL QUE RECIBE SUS DEPENDENCIAS ---
// ============================================================
class AuditoriaViewModel(
    private val registro: RegistroDeAuditoria,
    private val reloj: RelojApp
) : ViewModel() {

    private val _entradas = MutableStateFlow<List<String>>(emptyList())
    val entradas = _entradas.asStateFlow()

    fun accion(nombre: String) {
        viewModelScope.launch {
            registro.registrar(nombre)
            _entradas.value = registro.todo()
        }
    }

    fun instanteActual(): String = reloj.ahora()
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaInyeccion() {
    // Resolvemos desde Koin manualmente para no depender del arranque global
    val vm = remember {
        asegurarKoinIniciado()
        val koin = GlobalContext.get()
        AuditoriaViewModel(
            registro = koin.get(),
            reloj = koin.get()
        )
    }
    val entradas by vm.entradas.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Inyección de dependencias", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Koin resolviendo dependencias de verdad. Es la opción de DI que " +
                        "funciona también en iOS — Hilt no.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Koin en marcha", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "El ViewModel recibió RegistroDeAuditoria y RelojApp sin construirlos. " +
                                "El registro, a su vez, recibió el reloj: Koin encadenó las dos.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.accion("abrir pantalla") }) { Text("Registrar acción") }
                        OutlinedButton(onClick = { vm.accion("pulsar guardar") }) { Text("Otra") }
                    }
                    if (entradas.isEmpty()) {
                        Text("Sin entradas todavía.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        entradas.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("El módulo que acabas de usar", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "commonMain/di/Modulos.kt",
                        """
                        val moduloDemo = module {
                            single<RelojApp> { RelojReal() }        // singleton
                            single { RegistroDeAuditoria(get()) }   // get() encadena
                            factory { PostsKtorRepository() }       // nuevo cada vez
                        }

                        // Arranque (Application en Android, initKoin() en commonMain)
                        startKoin { modules(moduloDemo) }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Y en un Composable con koin-androidx-compose",
                        """
                        @Composable
                        fun MiPantalla(vm: MiViewModel = koinViewModel()) { ... }
                        """.trimIndent()
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("El mismo caso, con Hilt", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Debes saber leerlo: es lo que encontrarás en la mayoría de apps " +
                                "Android solo-nativas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Hilt: anotaciones + generación de código",
                        """
                        @HiltAndroidApp
                        class MiApp : Application()

                        @Module
                        @InstallIn(SingletonComponent::class)
                        object AppModule {
                            @Provides @Singleton
                            fun reloj(): RelojApp = RelojReal()

                            @Provides @Singleton
                            fun registro(reloj: RelojApp) = RegistroDeAuditoria(reloj)
                        }

                        @HiltViewModel
                        class AuditoriaViewModel @Inject constructor(
                            private val registro: RegistroDeAuditoria,
                        ) : ViewModel()

                        // En la pantalla:
                        @Composable
                        fun MiPantalla(vm: AuditoriaViewModel = hiltViewModel()) { ... }
                        """.trimIndent()
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Por qué DI hace posible el testing",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Sustituir la implementación real por un fake",
                        """
                        @Test
                        fun `el registro usa el reloj inyectado`() {
                            // Sin DI, RegistroDeAuditoria haría new RelojReal() dentro
                            // y el resultado cambiaría en cada ejecución: intesteable.
                            val registro = RegistroDeAuditoria(RelojFijo("T0"))

                            registro.registrar("guardar")

                            assertEquals(listOf("[T0] guardar"), registro.todo())
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ese test está en InyeccionTest.kt y pasa en milisegundos. " +
                                "Ese es TODO el motivo de existir de la DI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de DI", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Regla base: pide por constructor, nunca construyas dentro")
                    BulletPoint("Registra INTERFACES, no clases concretas")
                    BulletPoint("single = singleton · factory = nuevo cada vez · viewModel = por VM")
                    BulletPoint("Koin: multiplataforma, falla al arrancar (usa checkModules() en tests)")
                    BulletPoint("Hilt: solo Android, pero falla en COMPILACIÓN (más seguro)")
                    BulletPoint("En KMP: Koin o kotlin-inject. Hilt queda descartado")
                    BulletPoint("En tests casi nunca necesitas el framework: construye a mano con fakes")
                }
            }
        }
    }
}
