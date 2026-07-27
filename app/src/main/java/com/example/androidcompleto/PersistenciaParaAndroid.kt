package com.example.androidcompleto

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * GUÍA MAESTRA DE PERSISTENCIA CON DATASTORE
 * DataStore es el reemplazo moderno de SharedPreferences para guardar
 * datos pequeños (preferencias, sesión, contadores) que sobreviven
 * al cierre de la app.
 *
 * VENTAJAS sobre SharedPreferences:
 * - Asíncrono con corrutinas (nunca bloquea la UI).
 * - Expone los datos como Flow: la UI se actualiza SOLA cuando cambian.
 * - Seguro ante errores de lectura/escritura.
 *
 * (Para datos grandes o relacionales — listas, tablas, búsquedas — el
 * siguiente paso es Room, que es una base de datos SQLite completa.)
 */

// --- 1. CREAR EL DATASTORE ---
// Extensión sobre Context: un único DataStore para toda la app (singleton).
val Context.dataStore by preferencesDataStore(name = "preferencias_app")

// --- 2. DEFINIR LAS CLAVES (tipadas) ---
object ClavesPrefs {
    val NOMBRE_USUARIO = stringPreferencesKey("nombre_usuario")
    val CONTADOR_VISITAS = intPreferencesKey("contador_visitas")
    val MODO_OSCURO = booleanPreferencesKey("modo_oscuro")
}

// --- 3. PANTALLA: leer con Flow + escribir con edit ---
@Composable
fun PantallaPersistencia() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // LECTURA: data es un Flow<Preferences>. Con map extraemos cada valor y
    // collectAsState lo convierte en estado de Compose. Si el valor cambia
    // en disco, la UI se recompone AUTOMÁTICAMENTE.
    val nombreGuardado by context.dataStore.data
        .map { prefs -> prefs[ClavesPrefs.NOMBRE_USUARIO] ?: "" }
        .collectAsState(initial = "")

    val visitas by context.dataStore.data
        .map { prefs -> prefs[ClavesPrefs.CONTADOR_VISITAS] ?: 0 }
        .collectAsState(initial = 0)

    val modoOscuro by context.dataStore.data
        .map { prefs -> prefs[ClavesPrefs.MODO_OSCURO] ?: false }
        .collectAsState(initial = false)

    // Estado local solo para el campo de texto (antes de guardar)
    var nombreEscrito by remember { mutableStateOf("") }

    // ESCRITURA: cada vez que entras a esta pantalla, suma una visita.
    // LaunchedEffect(Unit) = se ejecuta UNA vez al entrar.
    LaunchedEffect(Unit) {
        context.dataStore.edit { prefs ->
            prefs[ClavesPrefs.CONTADOR_VISITAS] = (prefs[ClavesPrefs.CONTADOR_VISITAS] ?: 0) + 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Persistencia (DataStore)", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Datos guardados en disco:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("👤 Nombre: ${nombreGuardado.ifEmpty { "(sin guardar)" }}")
                Text("🔢 Visitas a esta pantalla: $visitas")
                Text("🌙 Modo oscuro: ${if (modoOscuro) "Activado" else "Desactivado"}")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cierra la app por completo y vuelve: estos datos siguen aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        OutlinedTextField(
            value = nombreEscrito,
            onValueChange = { nombreEscrito = it },
            label = { Text("Escribe tu nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                // edit es suspend: se lanza dentro de una corrutina
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[ClavesPrefs.NOMBRE_USUARIO] = nombreEscrito
                    }
                    nombreEscrito = ""
                }
            }) {
                Text("Guardar nombre")
            }

            OutlinedButton(onClick = {
                scope.launch {
                    context.dataStore.edit { it.clear() } // borra TODO el DataStore
                }
            }) {
                Text("Borrar todo")
            }
        }

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = modoOscuro,
                onCheckedChange = { activado ->
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[ClavesPrefs.MODO_OSCURO] = activado
                        }
                    }
                }
            )
            Text("Preferencia de modo oscuro (persistente)")
        }
    }
}
