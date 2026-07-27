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
    MENU, FLUTTER, KOTLIN, COMPOSE, ESTADO, CORRUTINAS, EFECTOS,
    NAVEGACION, ANIMACIONES, FORMULARIOS, RED, PERSISTENCIA,
    ROOM, RED_AVANZADA, OFFLINE, RUTA
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
                        Pantalla.KOTLIN -> "Fundamentos Kotlin"
                        Pantalla.COMPOSE -> "Jetpack Compose"
                        Pantalla.ESTADO -> "Gestión de Estado"
                        Pantalla.CORRUTINAS -> "Corrutinas y Flows"
                        Pantalla.EFECTOS -> "Efectos Secundarios"
                        Pantalla.NAVEGACION -> "Navigation Compose"
                        Pantalla.ANIMACIONES -> "Animaciones"
                        Pantalla.FORMULARIOS -> "Formularios"
                        Pantalla.RED -> "Red con Retrofit"
                        Pantalla.PERSISTENCIA -> "Persistencia"
                        Pantalla.ROOM -> "Room: BD Offline"
                        Pantalla.RED_AVANZADA -> "Retrofit Avanzado"
                        Pantalla.OFFLINE -> "Offline-First"
                        Pantalla.RUTA -> "Ruta de Aprendizaje"
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
                Pantalla.KOTLIN -> PantallaKotlin()
                Pantalla.COMPOSE -> LeccionCompose()
                Pantalla.ESTADO -> LeccionEstado()
                Pantalla.CORRUTINAS -> PantallaCorrutinas()
                Pantalla.EFECTOS -> PantallaEfectos()
                Pantalla.NAVEGACION -> PantallaNavegacion()
                Pantalla.ANIMACIONES -> PantallaAnimaciones()
                Pantalla.FORMULARIOS -> PantallaFormularios()
                Pantalla.RED -> PantallaRed()
                Pantalla.PERSISTENCIA -> PantallaPersistencia()
                Pantalla.ROOM -> PantallaRoom()
                Pantalla.RED_AVANZADA -> PantallaRedAvanzada()
                Pantalla.OFFLINE -> PantallaOfflineFirst()
                Pantalla.RUTA -> PantallaRuta()
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

@Composable
fun MenuPrincipal(onNavigate: (Pantalla) -> Unit) {
    val opciones = listOf(
        OpcionMenu("0. ¿Vienes de Flutter? 🚀", "Diccionario: setState→remember, Bloc→ViewModel...", Pantalla.FLUTTER),
        OpcionMenu("1. Kotlin Interactivo", "Ejecuta 10 demos: null safety, lambdas, sealed...", Pantalla.KOTLIN),
        OpcionMenu("2. Compose", "Layouts, Weight, Grids, LazyRow y Modifiers", Pantalla.COMPOSE),
        OpcionMenu("3. Estado", "ViewModel, Flow y Remember", Pantalla.ESTADO),
        OpcionMenu("4. Corrutinas", "Suspend, Async/Await y Flows", Pantalla.CORRUTINAS),
        OpcionMenu("5. Efectos Secundarios", "LaunchedEffect, DisposableEffect, derivedStateOf", Pantalla.EFECTOS),
        OpcionMenu("6. Navegación", "NavHost, Rutas y Argumentos", Pantalla.NAVEGACION),
        OpcionMenu("7. Animaciones", "animateAsState, Visibility y Transiciones", Pantalla.ANIMACIONES),
        OpcionMenu("8. Formularios", "TextFields, Validación y State Hoisting", Pantalla.FORMULARIOS),
        OpcionMenu("9. Red (Retrofit)", "MVVM: API + Repository + ViewModel + Coil", Pantalla.RED),
        OpcionMenu("10. Persistencia", "DataStore: guardar datos en disco", Pantalla.PERSISTENCIA),
        OpcionMenu("11. Room 💾", "Base de datos SQLite offline: Entity, DAO, CRUD", Pantalla.ROOM),
        OpcionMenu("12. Retrofit Avanzado 🌐", "Interceptores, POST, headers y errores HTTP", Pantalla.RED_AVANZADA),
        OpcionMenu("13. Offline-First 🏆", "Room + Retrofit: la app que funciona sin internet", Pantalla.OFFLINE),
        OpcionMenu("🎯 Tu Ruta a Senior", "Checklist con progreso guardado en DataStore", Pantalla.RUTA)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(opciones) { opcion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(opcion.destino) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = opcion.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = opcion.subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
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
