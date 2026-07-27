package com.example.androidcompleto

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DICCIONARIO FLUTTER → COMPOSE
 * Si ya dominas Flutter, NO empieces de cero: casi todo lo que sabes
 * tiene una traducción directa. Compose y Flutter comparten la misma
 * filosofía: UI declarativa = f(estado). Cambia el estado → se redibuja.
 *
 * Flutter: rebuild del Widget tree.
 * Compose: recomposición de los Composables.
 * ¡Es el MISMO modelo mental!
 */

data class Equivalencia(
    val flutter: String,
    val compose: String,
    val codigoFlutter: String,
    val codigoCompose: String,
    val nota: String
)

data class CategoriaEquivalencias(
    val titulo: String,
    val items: List<Equivalencia>
)

val diccionarioFlutterCompose = listOf(

    CategoriaEquivalencias("🧱 Widgets y Layout", listOf(
        Equivalencia(
            "Widget (build)", "@Composable fun",
            "class MiWidget extends StatelessWidget {\n  Widget build(ctx) => Text('Hola');\n}",
            "@Composable\nfun MiComponente() {\n    Text(\"Hola\")\n}",
            "No hay clases: solo funciones. Adiós al boilerplate de StatelessWidget."
        ),
        Equivalencia(
            "Column / Row", "Column / Row",
            "Column(\n  mainAxisAlignment: MainAxisAlignment.center,\n  children: [...]\n)",
            "Column(\n    verticalArrangement = Arrangement.Center\n) { ... }",
            "¡Se llaman igual! mainAxisAlignment → Arrangement, crossAxisAlignment → Alignment."
        ),
        Equivalencia(
            "Stack", "Box",
            "Stack(children: [fondo, frente])",
            "Box {\n    Fondo()\n    Frente()  // se dibuja encima\n}",
            "Positioned → Modifier.align(Alignment.TopStart) dentro del Box."
        ),
        Equivalencia(
            "Container", "Box + Modifier",
            "Container(\n  padding: EdgeInsets.all(16),\n  color: Colors.red,\n  width: 100,\n)",
            "Box(modifier = Modifier\n    .size(100.dp)\n    .background(Color.Red)\n    .padding(16.dp))",
            "Container no existe: TODO (padding, color, tamaño, bordes) se hace con Modifier encadenado. EL ORDEN de la cadena importa."
        ),
        Equivalencia(
            "Expanded / Flexible", "Modifier.weight()",
            "Expanded(flex: 2, child: MiWidget())",
            "MiComponente(modifier = Modifier.weight(2f))",
            "Idéntico concepto: reparte el espacio sobrante proporcionalmente."
        ),
        Equivalencia(
            "SizedBox", "Spacer / Modifier.size",
            "SizedBox(height: 16)",
            "Spacer(Modifier.height(16.dp))",
            "Para separar: Spacer. Para forzar tamaño: Modifier.size(...)."
        ),
        Equivalencia(
            "ListView.builder", "LazyColumn",
            "ListView.builder(\n  itemCount: lista.length,\n  itemBuilder: (ctx, i) => Tile(lista[i]),\n)",
            "LazyColumn {\n    items(lista) { item ->\n        Tile(item)\n    }\n}",
            "Mismo concepto lazy: solo dibuja lo visible. GridView → LazyVerticalGrid, ListView horizontal → LazyRow."
        ),
    )),

    CategoriaEquivalencias("🔄 Estado", listOf(
        Equivalencia(
            "StatefulWidget + setState", "remember + mutableStateOf",
            "int count = 0;\nsetState(() => count++);",
            "var count by remember { mutableIntStateOf(0) }\ncount++  // ¡y ya! la UI se redibuja",
            "No hay setState: la variable ES observable. Mutarla dispara la recomposición automáticamente."
        ),
        Equivalencia(
            "Bloc / Cubit / StateNotifier", "ViewModel + StateFlow",
            "class MiCubit extends Cubit<int> {\n  MiCubit() : super(0);\n  void sumar() => emit(state + 1);\n}",
            "class MiVM : ViewModel() {\n    private val _n = MutableStateFlow(0)\n    val n = _n.asStateFlow()\n    fun sumar() { _n.value++ }\n}",
            "emit() → _state.value = x. BlocBuilder → collectAsState(). Y el ViewModel sobrevive rotaciones gratis."
        ),
        Equivalencia(
            "BlocBuilder / Consumer", "collectAsState()",
            "BlocBuilder<MiCubit, int>(\n  builder: (ctx, estado) => Text('\$estado'),\n)",
            "val estado by viewModel.n.collectAsState()\nText(\"\$estado\")",
            "Sin widget builder intermedio: colectas el Flow y usas el valor directo."
        ),
        Equivalencia(
            "InheritedWidget / Provider", "CompositionLocal",
            "Provider.of<Tema>(context)",
            "val tema = LocalTema.current",
            "Mismo patrón de 'datos que bajan por el árbol'. MaterialTheme.colorScheme es exactamente esto."
        ),
        Equivalencia(
            "PageStorage / keys", "rememberSaveable",
            "// sobrevivir a rebuilds profundos",
            "var texto by rememberSaveable {\n    mutableStateOf(\"\")\n}",
            "remember sobrevive recomposiciones; rememberSaveable sobrevive TAMBIÉN rotaciones y muerte de proceso."
        ),
    )),

    CategoriaEquivalencias("⏳ Async y ciclo de vida", listOf(
        Equivalencia(
            "Future + async/await", "suspend fun + corrutinas",
            "Future<String> cargar() async {\n  await Future.delayed(...);\n  return 'dato';\n}",
            "suspend fun cargar(): String {\n    delay(1000)\n    return \"dato\"\n}",
            "async/await de Dart → suspend de Kotlin. La diferencia: en Kotlin eliges el hilo con Dispatchers (IO, Main, Default)."
        ),
        Equivalencia(
            "Stream", "Flow",
            "Stream.periodic(...)\n  .where((n) => n.isEven)\n  .map((n) => n * 2)\n  .listen(print);",
            "flow { emit(...) }\n    .filter { it % 2 == 0 }\n    .map { it * 2 }\n    .collect { println(it) }",
            "where → filter, listen → collect. StreamController → MutableStateFlow/MutableSharedFlow."
        ),
        Equivalencia(
            "FutureBuilder", "estado + LaunchedEffect",
            "FutureBuilder(\n  future: cargar(),\n  builder: (ctx, snap) =>\n    snap.hasData ? Dato() : Spinner(),\n)",
            "var dato by remember { mutableStateOf<String?>(null) }\nLaunchedEffect(Unit) { dato = cargar() }\nif (dato == null) Spinner() else Dato()",
            "No hay FutureBuilder: lanzas el efecto y pintas según el estado. En apps reales esto vive en el ViewModel (lección 9)."
        ),
        Equivalencia(
            "initState()", "LaunchedEffect(Unit)",
            "@override\nvoid initState() {\n  super.initState();\n  cargarDatos();\n}",
            "LaunchedEffect(Unit) {\n    cargarDatos()\n}",
            "Se ejecuta UNA vez al entrar el Composable a pantalla."
        ),
        Equivalencia(
            "dispose()", "DisposableEffect / cancelación",
            "@override\nvoid dispose() {\n  controller.dispose();\n  super.dispose();\n}",
            "DisposableEffect(Unit) {\n    val recurso = registrar()\n    onDispose { recurso.liberar() }\n}",
            "Y las corrutinas de LaunchedEffect se cancelan SOLAS al salir: la mitad de los dispose de Flutter aquí no hacen falta."
        ),
    )),

    CategoriaEquivalencias("🧭 Navegación", listOf(
        Equivalencia(
            "Navigator.push / rutas", "NavController.navigate",
            "Navigator.pushNamed(\n  context,\n  '/detalle',\n  arguments: 42,\n);",
            "navController.navigate(\"detalle/42\")",
            "Las rutas son strings tipo URL con argumentos incrustados. onGenerateRoute → NavHost con composable(route)."
        ),
        Equivalencia(
            "Navigator.pop", "popBackStack",
            "Navigator.pop(context);",
            "navController.popBackStack()",
            "El botón físico 'Atrás' ya funciona solo, igual que en Flutter."
        ),
        Equivalencia(
            "WillPopScope", "BackHandler",
            "WillPopScope(\n  onWillPop: () async => confirmar(),\n  child: ...,\n)",
            "BackHandler(enabled = true) {\n    // interceptar el atrás\n}",
            "Lo usamos en MainActivity.kt para volver al menú. Míralo en el código."
        ),
    )),

    CategoriaEquivalencias("📦 Proyecto y herramientas", listOf(
        Equivalencia(
            "pubspec.yaml", "build.gradle.kts + libs.versions.toml",
            "dependencies:\n  http: ^1.0.0",
            "implementation(libs.retrofit)\n// versiones en libs.versions.toml",
            "El catálogo de versiones (toml) centraliza las versiones como pubspec, y gradle sync = pub get."
        ),
        Equivalencia(
            "hot reload", "Live Edit / Preview",
            "r en la terminal / guardar",
            "@Preview(showBackground = true)\n@Composable\nfun MiPreview() { MiComponente() }",
            "Live Edit de Android Studio ≈ hot reload. Y @Preview renderiza el Composable SIN emulador: úsalo muchísimo."
        ),
        Equivalencia(
            "MediaQuery", "BoxWithConstraints / WindowSizeClass",
            "MediaQuery.of(context).size.width",
            "BoxWithConstraints {\n    if (maxWidth < 600.dp) Movil() else Tablet()\n}",
            "Para diseño adaptativo serio: material3-adaptive (ya lo usa MainActivity2.kt)."
        ),
        Equivalencia(
            "package:test", "JUnit",
            "test('suma', () {\n  expect(suma(2,2), 4);\n});",
            "@Test\nfun `suma correcta`() {\n    assertEquals(4, suma(2, 2))\n}",
            "Mira LogicaKotlinTest.kt: mismos conceptos, y los nombres de test con backticks pueden llevar espacios."
        ),
    )),
)

// --- LA PANTALLA ---
@Composable
fun PantallaFlutter() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("De Flutter a Compose 🚀", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Buena noticia: ya sabes el 80%. Compose y Flutter son el MISMO " +
                                "modelo mental (UI = función del estado). Solo cambia el vocabulario. " +
                                "Toca cada equivalencia para ver el código lado a lado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        diccionarioFlutterCompose.forEach { categoria ->
            item {
                Text(
                    categoria.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(categoria.items) { eq ->
                TarjetaEquivalencia(eq)
            }
        }
    }
}

@Composable
fun TarjetaEquivalencia(eq: Equivalencia) {
    var expandida by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expandida = !expandida }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(eq.flutter, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("→", style = MaterialTheme.typography.titleMedium)
                Text(
                    eq.compose,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            if (expandida) {
                Spacer(Modifier.height(10.dp))
                BloqueCodigo(etiqueta = "Flutter (Dart)", codigo = eq.codigoFlutter)
                Spacer(Modifier.height(6.dp))
                BloqueCodigo(etiqueta = "Compose (Kotlin)", codigo = eq.codigoCompose)
                Spacer(Modifier.height(8.dp))
                Text(
                    "💡 ${eq.nota}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    "toca para ver el código",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun BloqueCodigo(etiqueta: String, codigo: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)
        Text(
            codigo,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(6.dp)
                )
                .padding(8.dp)
        )
    }
}
