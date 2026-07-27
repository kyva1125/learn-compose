package com.example.androidcompleto

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * GUÍA MAESTRA DE ROOM (Base de datos OFFLINE)
 * Room es la capa oficial sobre SQLite. Tus datos viven en el teléfono:
 * cierra la app, apaga el WiFi, reinicia el móvil — siguen ahí.
 *
 * LAS 3 PIEZAS DE ROOM:
 * 1. @Entity   -> una tabla (cada propiedad = una columna)
 * 2. @Dao      -> las consultas (Room GENERA la implementación por ti via KSP)
 * 3. @Database -> la base de datos que une todo (singleton)
 *
 * LA MAGIA SENIOR: el DAO devuelve Flow<List<T>>. Cuando insertas o borras,
 * TODAS las pantallas que observan ese Flow se actualizan SOLAS.
 * Nada de "recargar la lista a mano".
 */

// ============================================================
// --- 1. LA ENTIDAD (la tabla) ---
// ============================================================
@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Room asigna el id solo
    val titulo: String,
    val hecha: Boolean = false
)

// Entidad para la lección de Offline-First (posts cacheados de la API)
@Entity(tableName = "posts_cache")
data class PostCacheado(
    @PrimaryKey val id: Int,
    val titulo: String,
    val cuerpo: String
)

// ============================================================
// --- 2. EL DAO (las consultas) ---
// ============================================================
@Dao
interface TareaDao {
    // Flow = consulta REACTIVA: se re-emite sola cuando la tabla cambia
    @Query("SELECT * FROM tareas ORDER BY hecha ASC, id DESC")
    fun observarTodas(): Flow<List<Tarea>>

    @Query("SELECT COUNT(*) FROM tareas WHERE hecha = 0")
    fun observarPendientes(): Flow<Int>

    // Las operaciones de escritura son suspend: nunca bloquean la UI
    @Insert
    suspend fun insertar(tarea: Tarea)

    @Update
    suspend fun actualizar(tarea: Tarea)

    @Delete
    suspend fun borrar(tarea: Tarea)
}

@Dao
interface PostCacheDao {
    @Query("SELECT * FROM posts_cache ORDER BY id")
    fun observarTodos(): Flow<List<PostCacheado>>

    // REPLACE: si el post ya existe (mismo id), lo sobreescribe
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(posts: List<PostCacheado>)

    @Query("DELETE FROM posts_cache")
    suspend fun limpiar()
}

// ============================================================
// --- 3. LA BASE DE DATOS (singleton) ---
// ============================================================
// exportSchema=false: en apps reales se exporta el esquema para las MIGRACIONES
// (cuando cambias la versión de la BD sin perder datos del usuario)
@Database(entities = [Tarea::class, PostCacheado::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tareaDao(): TareaDao
    abstract fun postCacheDao(): PostCacheDao

    companion object {
        // @Volatile + synchronized = patrón singleton seguro entre hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtener(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "academia.db" // el archivo SQLite en el teléfono
                ).build().also { INSTANCE = it }
            }
        }
    }
}

// ============================================================
// --- 4. EL VIEWMODEL ---
// ============================================================
class TareasViewModel(private val dao: TareaDao) : ViewModel() {

    // El Flow del DAO expuesto tal cual: la BD es la fuente de la verdad
    val tareas: Flow<List<Tarea>> = dao.observarTodas()
    val pendientes: Flow<Int> = dao.observarPendientes()

    fun agregar(titulo: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch { dao.insertar(Tarea(titulo = titulo.trim())) }
    }

    fun alternar(tarea: Tarea) {
        viewModelScope.launch { dao.actualizar(tarea.copy(hecha = !tarea.hecha)) }
    }

    fun eliminar(tarea: Tarea) {
        viewModelScope.launch { dao.borrar(tarea) }
    }
}

// ============================================================
// --- 5. LA PANTALLA (CRUD completo) ---
// ============================================================
@Composable
fun PantallaRoom() {
    val context = LocalContext.current
    // viewModel { } con inicializador: le pasamos el DAO al constructor
    val vm: TareasViewModel = viewModel {
        TareasViewModel(AppDatabase.obtener(context).tareaDao())
    }

    val tareas by vm.tareas.collectAsState(initial = emptyList())
    val pendientes by vm.pendientes.collectAsState(initial = 0)
    var nuevoTitulo by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Tareas Offline (Room)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "CRUD real sobre SQLite. Cierra la app, apaga el WiFi, reinicia el " +
                    "teléfono: tus tareas siguen aquí. Pendientes: $pendientes",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        // --- CREATE ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nuevoTitulo,
                onValueChange = { nuevoTitulo = it },
                label = { Text("Nueva tarea") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                vm.agregar(nuevoTitulo)
                nuevoTitulo = ""
            }) { Text("＋") }
        }

        Spacer(Modifier.height(12.dp))

        // --- READ (reactivo: se refresca solo al insertar/editar/borrar) ---
        if (tareas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin tareas. Agrega la primera 👆")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tareas, key = { it.id }) { tarea ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // --- UPDATE (toggle hecha/pendiente) ---
                            Checkbox(
                                checked = tarea.hecha,
                                onCheckedChange = { vm.alternar(tarea) }
                            )
                            Text(
                                tarea.titulo,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (tarea.hecha)
                                    TextDecoration.LineThrough else TextDecoration.None
                            )
                            // --- DELETE ---
                            IconButton(onClick = { vm.eliminar(tarea) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar")
                            }
                        }
                    }
                }
            }
        }
    }
}
