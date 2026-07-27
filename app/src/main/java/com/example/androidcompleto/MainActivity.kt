package com.example.androidcompleto

import android.os.Bundle
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.androidcompleto.ui.theme.AndroidCompletoTheme
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidCompletoTheme {
                AppPrincipal()
            }
        }
    }
}

// 1. DEFINIMOS NUESTRAS SECCIONES (Navegación sencilla tipo Flutter)
enum class Pantalla {
    MENU, FLUTTER, RUTA,

    // ---- PISTA KOTLIN (el lenguaje: corre igual en Android, iOS y servidor) ----
    KOTLIN, KOTLIN_AVANZADO, CORRUTINAS, CORRUTINAS_AVANZADAS, TESTING, ARQUITECTURA,

    // ---- PISTA COMPOSE (la UI) ----
    COMPOSE, ESTADO, EFECTOS, NAVEGACION, NAVEGACION_TS, ANIMACIONES, FORMULARIOS,
    GESTOS, ACCESIBILIDAD, ADAPTIVE, INTEROP, RENDIMIENTO, LAYOUTS_CUSTOM,
    COMPOSITION_LOCAL,

    // ---- PISTA KMP Y DATOS (la capa que se comparte entre plataformas) ----
    KMP, KTOR_KMP, INYECCION, RED, PERSISTENCIA, ROOM, RED_AVANZADA, OFFLINE
}

/** Las tres pistas de aprendizaje en las que se agrupan las lecciones. */
enum class Pista(val titulo: String, val emoji: String, val descripcion: String) {
    KOTLIN_P("Kotlin", "🟣", "El lenguaje. Todo esto corre igual en Android, iOS y servidor."),
    COMPOSE_P("Compose", "🎨", "La interfaz declarativa: de layouts a gestos y accesibilidad."),
    KMP_P("KMP y datos", "🌍", "La capa que se comparte: red, persistencia e inyección.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrincipal() {

    val context = LocalContext.current;

    // Color del recurso
    val colorBar = colorResource(R.color.purple_500);

    // Snackbar
    val scope = rememberCoroutineScope();
    val snackbarHostState = remember { SnackbarHostState() }

    // Menu
    var expandedMenu by remember { mutableStateOf(false) }

    // FabPosition
    var fabPosition by remember { mutableStateOf(FabPosition.End) }

    var pantallaActual by remember { mutableStateOf(Pantalla.MENU) }

    // Manejo del botón "Atrás" físico del dispositivo
    BackHandler(enabled = pantallaActual != Pantalla.MENU) {
        pantallaActual = Pantalla.MENU
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            BottomAppBar(
                containerColor = colorBar
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Horas locas");
                        }
                    }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")

                    }
                }
                Spacer(Modifier.weight(1f, true))
                Box{
                    IconButton(
                        onClick = {
                            expandedMenu = true;
                        }
                    ) {
                        Icon(Icons.Filled.MoreVert,
                            contentDescription = "Options")

                    }
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = {
                        expandedMenu = false;
                    }) {
                        DropdownMenuItem(
                            text = {
                                Text("Salir")
                            },
                            onClick = {
                                expandedMenu = false;
                                Toast.makeText(context, "Saliendo", Toast.LENGTH_SHORT).show();

                            }
                        )
                    }
                }


            }
        },
        floatingActionButton = {
            FloatingActionButton(

                onClick = {
                    fabPosition = if(fabPosition == FabPosition.End){
                        FabPosition.Center;
                    }else{
                        FabPosition.End;
                    }
                }
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
            }
        },
        floatingActionButtonPosition = fabPosition,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = when(pantallaActual) {
                        Pantalla.MENU -> "Academia Android"
                        Pantalla.FLUTTER -> "Flutter → Compose"
                        Pantalla.RUTA -> "Ruta de Aprendizaje"
                        // Pista Kotlin
                        Pantalla.KOTLIN -> "Fundamentos Kotlin"
                        Pantalla.KOTLIN_AVANZADO -> "Kotlin Avanzado"
                        Pantalla.CORRUTINAS -> "Corrutinas y Flows"
                        Pantalla.CORRUTINAS_AVANZADAS -> "Corrutinas Avanzadas"
                        Pantalla.TESTING -> "Testing Asíncrono"
                        Pantalla.ARQUITECTURA -> "Arquitectura Senior"
                        // Pista Compose
                        Pantalla.COMPOSE -> "Jetpack Compose"
                        Pantalla.ESTADO -> "Gestión de Estado"
                        Pantalla.EFECTOS -> "Efectos Secundarios"
                        Pantalla.NAVEGACION -> "Navigation Compose"
                        Pantalla.NAVEGACION_TS -> "Navegación Type-Safe"
                        Pantalla.ANIMACIONES -> "Animaciones"
                        Pantalla.FORMULARIOS -> "Formularios"
                        Pantalla.GESTOS -> "Gestos Táctiles"
                        Pantalla.ACCESIBILIDAD -> "Accesibilidad"
                        Pantalla.ADAPTIVE -> "Layouts Adaptativos"
                        Pantalla.INTEROP -> "Interop con Views"
                        Pantalla.RENDIMIENTO -> "Rendimiento en Compose"
                        Pantalla.LAYOUTS_CUSTOM -> "Layouts Custom y Canvas"
                        Pantalla.COMPOSITION_LOCAL -> "CompositionLocal"
                        // Pista KMP y datos
                        Pantalla.KMP -> "Kotlin Multiplatform"
                        Pantalla.KTOR_KMP -> "Ktor + Serialization"
                        Pantalla.INYECCION -> "Inyección (Koin/Hilt)"
                        Pantalla.RED -> "Red con Retrofit"
                        Pantalla.PERSISTENCIA -> "Persistencia"
                        Pantalla.ROOM -> "Room: BD Offline"
                        Pantalla.RED_AVANZADA -> "Retrofit Avanzado"
                        Pantalla.OFFLINE -> "Offline-First"
                    })
                },
                navigationIcon = {
                    if (pantallaActual != Pantalla.MENU) {
                        IconButton(onClick = { pantallaActual = Pantalla.MENU }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (pantallaActual) {
                Pantalla.MENU -> MenuPrincipal(onNavigate = { pantallaActual = it })
                Pantalla.FLUTTER -> PantallaFlutter()
                Pantalla.RUTA -> PantallaRuta()
                // Pista Kotlin
                Pantalla.KOTLIN -> PantallaKotlin()
                Pantalla.KOTLIN_AVANZADO -> PantallaKotlinAvanzado()
                Pantalla.CORRUTINAS -> PantallaCorrutinas()
                Pantalla.CORRUTINAS_AVANZADAS -> PantallaCorrutinasAvanzadas()
                Pantalla.TESTING -> PantallaTesting()
                Pantalla.ARQUITECTURA -> PantallaArquitectura()
                // Pista Compose
                Pantalla.COMPOSE -> LeccionCompose()
                Pantalla.ESTADO -> LeccionEstado()
                Pantalla.EFECTOS -> PantallaEfectos()
                Pantalla.NAVEGACION -> PantallaNavegacion()
                Pantalla.NAVEGACION_TS -> PantallaNavegacionTypeSafe()
                Pantalla.ANIMACIONES -> PantallaAnimaciones()
                Pantalla.FORMULARIOS -> PantallaFormularios()
                Pantalla.GESTOS -> PantallaGestos()
                Pantalla.ACCESIBILIDAD -> PantallaAccesibilidad()
                Pantalla.ADAPTIVE -> PantallaAdaptive()
                Pantalla.INTEROP -> PantallaInterop()
                Pantalla.RENDIMIENTO -> PantallaRendimiento()
                Pantalla.LAYOUTS_CUSTOM -> PantallaLayoutsCustom()
                Pantalla.COMPOSITION_LOCAL -> PantallaCompositionLocal()
                // Pista KMP y datos
                Pantalla.KMP -> PantallaKmp()
                Pantalla.KTOR_KMP -> PantallaKtorKmp()
                Pantalla.INYECCION -> PantallaInyeccion()
                Pantalla.RED -> PantallaRed()
                Pantalla.PERSISTENCIA -> PantallaPersistencia()
                Pantalla.ROOM -> PantallaRoom()
                Pantalla.RED_AVANZADA -> PantallaRedAvanzada()
                Pantalla.OFFLINE -> PantallaOfflineFirst()
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.i("Message", "LaunchedEffect");
    }

//    DisposableEffect(LocalLifecycleOwner.current) {
//        Log.i("Message", "LaunchedEffect");
//
//    }
}

/**
 * EL TEMARIO COMPLETO, AGRUPADO EN TRES PISTAS.
 *
 *  🟣 KOTLIN  — el lenguaje y su runtime. Nada de esto depende de Android:
 *               es exactamente lo que se comparte en un proyecto KMP.
 *  🎨 COMPOSE — la interfaz declarativa, de lo básico a gestos y a11y.
 *  🌍 KMP y datos — la capa de red, persistencia e inyección: la que en un
 *               proyecto multiplataforma vive en commonMain.
 */
val temario: Map<Pista, List<OpcionMenu>> = mapOf(
    Pista.KOTLIN_P to listOf(
        OpcionMenu("K1. Kotlin Interactivo", "Ejecuta 10 demos: null safety, lambdas, sealed...", Pantalla.KOTLIN),
        OpcionMenu("K2. Kotlin Avanzado 🧬", "Varianza, reified, delegados, inline, DSLs, value class", Pantalla.KOTLIN_AVANZADO),
        OpcionMenu("K3. Corrutinas", "Suspend, Async/Await y Flows", Pantalla.CORRUTINAS),
        OpcionMenu("K4. Corrutinas Avanzadas ⚙️", "Cancelación, supervisorScope, flatMapLatest, retry", Pantalla.CORRUTINAS_AVANZADAS),
        OpcionMenu("K5. Testing Asíncrono 🧪", "runTest, tiempo virtual, Turbine y fakes", Pantalla.TESTING),
        OpcionMenu("K6. Arquitectura Senior 🏛️", "Clean Architecture + MVI + reducer puro testeado", Pantalla.ARQUITECTURA),
    ),
    Pista.COMPOSE_P to listOf(
        OpcionMenu("C1. Compose", "Layouts, Weight, Grids, LazyRow y Modifiers", Pantalla.COMPOSE),
        OpcionMenu("C2. Estado", "ViewModel, Flow y Remember", Pantalla.ESTADO),
        OpcionMenu("C3. Efectos Secundarios", "LaunchedEffect, DisposableEffect, derivedStateOf", Pantalla.EFECTOS),
        OpcionMenu("C4. Navegación", "NavHost, Rutas y Argumentos", Pantalla.NAVEGACION),
        OpcionMenu("C5. Navegación Type-Safe 🔒", "Rutas como clases @Serializable, sin Strings", Pantalla.NAVEGACION_TS),
        OpcionMenu("C6. Animaciones", "animateAsState, Visibility y Transiciones", Pantalla.ANIMACIONES),
        OpcionMenu("C7. Formularios", "TextFields, Validación y State Hoisting", Pantalla.FORMULARIOS),
        OpcionMenu("C8. Gestos Táctiles 👆", "pointerInput, arrastrar, deslizar para borrar, zoom", Pantalla.GESTOS),
        OpcionMenu("C9. Accesibilidad ♿", "Semantics, TalkBack, 48dp y por qué hace testeable tu app", Pantalla.ACCESIBILIDAD),
        OpcionMenu("C10. Layouts Adaptativos 📱", "WindowSizeClass, list-detail, tablets y plegables", Pantalla.ADAPTIVE),
        OpcionMenu("C11. Interop con Views 🔌", "AndroidView y ComposeView: migrar apps legacy", Pantalla.INTEROP),
        OpcionMenu("C12. Rendimiento ⚡", "Recomposición, estabilidad y lecturas diferidas EN VIVO", Pantalla.RENDIMIENTO),
        OpcionMenu("C13. Layouts Custom 📐", "Layout(), Modifier.layout y gráficas con Canvas", Pantalla.LAYOUTS_CUSTOM),
        OpcionMenu("C14. CompositionLocal 🎨", "Design tokens y el secreto de MaterialTheme", Pantalla.COMPOSITION_LOCAL),
    ),
    Pista.KMP_P to listOf(
        OpcionMenu("M1. Kotlin Multiplatform 🌍", "expect/actual, commonMain y Compose Multiplatform", Pantalla.KMP),
        OpcionMenu("M2. Ktor + Serialization 🚀", "El stack de red que compila también para iOS", Pantalla.KTOR_KMP),
        OpcionMenu("M3. Inyección: Koin vs Hilt 💉", "DI real funcionando, y por qué Hilt no vale en KMP", Pantalla.INYECCION),
        OpcionMenu("M4. Red (Retrofit)", "MVVM: API + Repository + ViewModel + Coil", Pantalla.RED),
        OpcionMenu("M5. Persistencia", "DataStore: guardar datos en disco", Pantalla.PERSISTENCIA),
        OpcionMenu("M6. Room 💾", "Base de datos SQLite offline: Entity, DAO, CRUD", Pantalla.ROOM),
        OpcionMenu("M7. Retrofit Avanzado 🌐", "Interceptores, POST, headers y errores HTTP", Pantalla.RED_AVANZADA),
        OpcionMenu("M8. Offline-First 🏆", "Room + Retrofit: la app que funciona sin internet", Pantalla.OFFLINE),
    )
)

@Composable
fun MenuPrincipal(onNavigate: (Pantalla) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Cabecera + accesos sueltos ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(Pantalla.RUTA) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎯 Tu Ruta a Senior", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Checklist de las 3 pistas con progreso guardado en DataStore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(Pantalla.FLUTTER) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🚀 ¿Vienes de Flutter?", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Diccionario: setState→remember, Bloc→ViewModel...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // --- Las tres pistas ---
        temario.forEach { (pista, lecciones) ->
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        "${pista.emoji} ${pista.titulo}  ·  ${lecciones.size} lecciones",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        pista.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            items(lecciones) { opcion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(opcion.destino) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = opcion.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = opcion.subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

data class OpcionMenu(val titulo: String, val subtitulo: String, val destino: Pantalla)

// --- INTEGRACIÓN CON TUS OTRAS CLASES ---

@Composable
fun LeccionCompose() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Llamamos a los ejemplos que creamos en ComposeParaAndroid.kt
        EjemploLayouts()
        Spacer(modifier = Modifier.height(20.dp))
        EjemploModifiers()
    }
}

@Composable
fun LeccionEstado() {
    // viewModel() (de lifecycle-viewmodel-compose) en lugar de remember { MiViewModel() }:
    // así el ViewModel SÍ sobrevive a rotaciones de pantalla.
    val viewModel: MiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Estado Local:", style = MaterialTheme.typography.titleMedium)
        ContadorLocal() // De EstadoParaAndroid.kt

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Estado Global (ViewModel):", style = MaterialTheme.typography.titleMedium)
        PantallaPrincipal(viewModel = viewModel) // De EstadoParaAndroid.kt
    }
}
