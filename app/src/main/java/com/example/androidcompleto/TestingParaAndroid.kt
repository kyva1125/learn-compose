package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * TESTING DE VERDAD — PISTA KOTLIN (Lección K5)
 *
 * Testear funciones puras es fácil (ya lo hiciste en la lección de
 * arquitectura). Lo que separa a un senior es testear lo ASÍNCRONO:
 * ViewModels, StateFlow, debounce, cancelaciones, errores de red.
 *
 * LAS 3 HERRAMIENTAS:
 *
 * 1. runTest { }  — de kotlinx-coroutines-test. Ejecuta corrutinas con
 *    TIEMPO VIRTUAL: un delay(5000) tarda 0 ms de reloj real. Puedes
 *    adelantar el reloj a mano con advanceTimeBy() / advanceUntilIdle().
 *
 * 2. Turbine — awaitItem() sobre un Flow. Sin él tendrías que hacer
 *    malabares con collect + corrutinas y tests inestables (flaky).
 *
 * 3. FAKES (no mocks) — implementaciones falsas de tus interfaces,
 *    escritas a mano. Google recomienda fakes sobre mocks: son más
 *    legibles, no se rompen al refactorizar y no necesitan librería.
 *
 * LA PIRÁMIDE (cuántos tests de cada tipo):
 *      🔺 UI (androidTest, lentos)      ← pocos, los flujos críticos
 *     🔷🔷 Integración (ViewModel+repo)  ← algunos
 *   🟩🟩🟩 Unitarios (dominio puro)      ← muchísimos, rapidísimos
 *
 * REGLA DE ORO PARA QUE TODO ESTO SEA POSIBLE: inyecta las dependencias
 * (repositorio, dispatcher) por constructor. Si tu ViewModel hace
 * `Retrofit.create(...)` dentro, no hay forma humana de testearlo.
 */

// ============================================================
// --- EL CONTRATO (lo que el ViewModel necesita) ---
// ============================================================
interface BuscadorRepository {
    suspend fun buscar(consulta: String): List<String>
}

// ============================================================
// --- EL ESTADO DE LA PANTALLA ---
// ============================================================
sealed interface BuscadorEstado {
    data object Vacio : BuscadorEstado
    data object Buscando : BuscadorEstado
    data class Resultados(val items: List<String>) : BuscadorEstado
    data class Error(val mensaje: String) : BuscadorEstado
}

// ============================================================
// --- EL VIEWMODEL (diseñado para ser testeable) ---
// ============================================================
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BuscadorViewModel(
    // Dependencia por constructor → en el test le paso un fake.
    private val repo: BuscadorRepository
) : ViewModel() {

    private val _consulta = MutableStateFlow("")
    val consulta = _consulta.asStateFlow()

    val estado: StateFlow<BuscadorEstado> = _consulta
        .debounce(300)                 // espera a que deje de teclear
        .distinctUntilChanged()        // ignora repeticiones
        .flatMapLatest { q ->          // cancela la búsqueda anterior
            if (q.isBlank()) {
                flowOf(BuscadorEstado.Vacio)
            } else {
                flow {
                    emit(BuscadorEstado.Buscando)
                    emit(BuscadorEstado.Resultados(repo.buscar(q)))
                }.catch { e ->
                    emit(BuscadorEstado.Error(e.message ?: "error desconocido"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BuscadorEstado.Vacio
        )

    fun alEscribir(texto: String) { _consulta.value = texto }
}

// ============================================================
// --- PANTALLA (con testTag para los tests de UI) ---
// ============================================================
@Composable
fun PantallaTesting() {
    // Fake también en la demo: así la pantalla funciona sin red.
    val vm = remember {
        BuscadorViewModel(object : BuscadorRepository {
            private val catalogo = listOf(
                "Kotlin", "Kotlin Multiplatform", "Compose", "Compose Multiplatform",
                "Coroutines", "Flow", "Ktor", "Koin", "Room", "Retrofit"
            )
            override suspend fun buscar(consulta: String) =
                catalogo.filter { it.contains(consulta, ignoreCase = true) }
        })
    }
    val consulta by vm.consulta.collectAsState()
    val estado by vm.estado.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Testing asíncrono", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Este buscador usa debounce + flatMapLatest. Abajo tienes los tests " +
                        "reales que lo prueban con tiempo virtual, sin esperar ni un segundo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pruébalo", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = consulta,
                        onValueChange = vm::alEscribir,
                        label = { Text("Buscar (ej: 'kot', 'compose')") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            // testTag = el "id" que usan los tests de UI para encontrarlo
                            .semantics { testTag = "campo_busqueda" }
                    )
                    when (val e = estado) {
                        BuscadorEstado.Vacio -> Text("Escribe algo para buscar",
                            style = MaterialTheme.typography.bodySmall)
                        BuscadorEstado.Buscando -> Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Buscando...")
                        }
                        is BuscadorEstado.Resultados ->
                            if (e.items.isEmpty()) {
                                Text("Sin resultados 🤷")
                            } else {
                                Column(Modifier.semantics { testTag = "lista_resultados" }) {
                                    e.items.forEach { Text("• $it") }
                                }
                            }
                        is BuscadorEstado.Error ->
                            Text("✖ ${e.mensaje}", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Fíjate: hay 300ms de debounce. Teclea rápido y verás que solo " +
                                "busca cuando paras.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("1. runTest: el tiempo virtual", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Un delay(10.000) que tarda 0 ms reales",
                        """
                        @Test
                        fun `el debounce espera 300ms`() = runTest {
                            vm.alEscribir("kot")
                            // Nadie espera de verdad: adelantamos el reloj virtual
                            advanceTimeBy(299)
                            assertEquals(Vacio, vm.estado.value)   // aún no busca

                            advanceTimeBy(2)
                            runCurrent()
                            assertTrue(vm.estado.value is Resultados)
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("2. Turbine: assertions sobre Flows", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "awaitItem() en lugar de malabares con collect",
                        """
                        @Test
                        fun `emite Buscando y luego Resultados`() = runTest {
                            vm.estado.test {                    // Turbine
                                assertEquals(Vacio, awaitItem())
                                vm.alEscribir("kot")
                                assertEquals(Buscando, awaitItem())
                                val r = awaitItem() as Resultados
                                assertTrue(r.items.isNotEmpty())
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("3. Fakes, no mocks", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "Un fake es solo una implementación falsa a mano",
                        """
                        class FakeBuscadorRepository(
                            private val resultados: List<String> = emptyList(),
                            private val fallaCon: Exception? = null,
                        ) : BuscadorRepository {
                            var vecesLlamado = 0; private set
                            override suspend fun buscar(consulta: String): List<String> {
                                vecesLlamado++
                                fallaCon?.let { throw it }
                                return resultados.filter { it.contains(consulta, true) }
                            }
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ventaja sobre Mockito/MockK: se lee como código normal, no se " +
                                "rompe al renombrar métodos y funciona igual en KMP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("4. Tests de UI en Compose", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "androidTest/BuscadorUiTest.kt — corre en emulador",
                        """
                        @get:Rule val compose = createComposeRule()

                        @Test fun muestra_resultados_al_escribir() {
                            compose.setContent { PantallaTesting() }

                            compose.onNodeWithTag("campo_busqueda")
                                .performTextInput("kot")

                            compose.waitUntil(2_000) {
                                compose.onAllNodesWithTag("lista_resultados")
                                    .fetchSemanticsNodes().isNotEmpty()
                            }
                            compose.onNodeWithText("• Kotlin").assertIsDisplayed()
                        }
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Compose no busca por 'id' de vista: busca por SEMANTICS (texto, " +
                                "contentDescription, testTag). Es el mismo árbol que usa " +
                                "TalkBack — por eso testear bien y ser accesible van juntos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de testing", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("runTest { } = tiempo virtual: advanceTimeBy, advanceUntilIdle, runCurrent")
                    BulletPoint("Turbine: flujo.test { awaitItem() } — legible y sin flakiness")
                    BulletPoint("Inyecta el Dispatcher: MainDispatcherRule con StandardTestDispatcher")
                    BulletPoint("Fakes > mocks: más legibles y compatibles con KMP")
                    BulletPoint("Compose se testea por semantics: testTag, texto, contentDescription")
                    BulletPoint("Muchos unitarios, algunos de integración, pocos de UI")
                    BulletPoint("Un test que no puede fallar no sirve: verifica que falle al romper el código")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Archivos reales de esta lección: test/CorrutinasFlowTest.kt, " +
                                "test/BuscadorViewModelTest.kt y androidTest/BuscadorUiTest.kt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
