package com.example.androidcompleto


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE KOTLIN PARA ANDROID (INTERACTIVA)
 * Cada tarjeta tiene un botón "Ejecutar": el código de la derecha se ejecuta
 * de verdad y ves el resultado en pantalla. Lee el código fuente de cada
 * función mientras pruebas — así se aprende de verdad.
 */

// ============================================================
// --- 1. VARIABLES, TIPOS Y STRING TEMPLATES ---
// ============================================================
fun demoVariables(): String {
    val inmutable = "Kotlin"       // val: no se puede reasignar (como final)
    var mutable = 10               // var: sí se puede reasignar
    mutable += 5

    val pi: Double = 3.1416        // tipo explícito
    val inferido = true            // Kotlin INFIERE que es Boolean

    // String templates: $variable o ${expresión}
    return "Lenguaje: $inmutable | mutable=$mutable | " +
            "pi=$pi | ¿inferido es Boolean? ${inferido is Boolean}"
}

// ============================================================
// --- 2. NULL SAFETY (el corazón de la estabilidad) ---
// ============================================================
fun demoNullSafety(): String {
    val conValor: String? = "Hola"   // String? = puede ser null
    val sinValor: String? = null

    // ?.  llamada segura: si es null devuelve null, NO crashea
    // ?:  operador Elvis: valor por defecto si es null
    val a = conValor?.length ?: 0
    val b = sinValor?.length ?: 0

    // let + ?. : ejecuta el bloque SOLO si no es null
    val mensaje = sinValor?.let { "nunca me ejecuto" } ?: "sinValor era null, usé el Elvis"

    return "conValor?.length = $a | sinValor?.length ?: 0 = $b | $mensaje"
    // ¡OJO! input!!.length forzaría el valor y CRASHEA si es null. Evítalo.
}

// ============================================================
// --- 3. COLECCIONES Y LAMBDAS (lo que usarás TODOS los días) ---
// ============================================================
fun demoColecciones(): String {
    val numeros = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    val pares = numeros.filter { it % 2 == 0 }       // filtra
    val dobles = pares.map { it * 2 }                // transforma
    val suma = numeros.sumOf { it }                  // agrega
    val primerMayorQue5 = numeros.first { it > 5 }   // busca
    val agrupados = numeros.groupBy { if (it <= 5) "bajos" else "altos" }

    return "pares=$pares\ndobles=$dobles\nsuma=$suma | primero>5: $primerMayorQue5\n" +
            "agrupados=$agrupados"
}

// ============================================================
// --- 4. WHEN: el super-switch ---
// ============================================================
fun demoWhen(valor: Int): String {
    // when es una EXPRESIÓN: devuelve un valor directamente
    return when {
        valor < 0 -> "$valor es negativo"
        valor == 0 -> "es cero"
        valor in 1..9 -> "$valor tiene un dígito"
        valor in 10..99 -> "$valor tiene dos dígitos"
        else -> "$valor es grande"
    }
}

// ============================================================
// --- 5. CLASES, HERENCIA E INTERFACES ---
// ============================================================
// En Kotlin las clases son 'final' por defecto: 'open' permite heredar.
open class Animal(val nombre: String) {
    open fun sonido(): String = "..."
}

class Perro(nombre: String) : Animal(nombre) {
    override fun sonido() = "¡Guau!"
}

// Las interfaces pueden tener implementación por defecto
interface Volador {
    fun volar(): String = "Estoy volando"
}

class Pato(nombre: String) : Animal(nombre), Volador {
    override fun sonido() = "¡Cuac!"
}

fun demoClases(): String {
    // Polimorfismo: una lista de Animal con comportamientos distintos
    val animales: List<Animal> = listOf(Perro("Firulais"), Pato("Donald"))
    val sonidos = animales.joinToString { "${it.nombre}: ${it.sonido()}" }
    val vuelo = (animales[1] as Volador).volar() // cast con 'as'
    return "$sonidos\nAdemás, Donald: $vuelo"
}

// ============================================================
// --- 6. DATA CLASSES: copy, equals y destructuring ---
// ============================================================
data class Producto(val id: Int, val nombre: String, val precio: Double)

fun demoDataClass(): String {
    val original = Producto(1, "Laptop", 3500.0)

    // copy: crea una copia cambiando SOLO lo que indicas (inmutabilidad)
    val rebajado = original.copy(precio = 2999.0)

    // equals comparado por CONTENIDO (no por referencia)
    val iguales = original == Producto(1, "Laptop", 3500.0)

    // Destructuring: desempaqueta las propiedades en variables
    val (id, nombre, precio) = rebajado

    return "original=$original\nrebajado=$rebajado\n" +
            "¿misma data = iguales? $iguales | destructuring: id=$id, $nombre a S/$precio"
}

// ============================================================
// --- 7. SEALED CLASSES: estados cerrados (el patrón de UI) ---
// ============================================================
sealed class UIState {
    object Loading : UIState()
    data class Success(val datos: List<String>) : UIState()
    data class Error(val mensaje: String) : UIState()
}

fun demoSealed(estado: UIState): String {
    // El when es EXHAUSTIVO: si mañana añades un estado, el compilador
    // te OBLIGA a manejarlo aquí. Imposible olvidar un caso.
    return when (estado) {
        is UIState.Loading -> "⏳ Cargando..."
        is UIState.Success -> "✔ Llegaron ${estado.datos.size} datos: ${estado.datos}"
        is UIState.Error -> "✖ Error: ${estado.mensaje}"
    }
}

// ============================================================
// --- 8. SCOPE FUNCTIONS: let, apply, run, also, with ---
// ============================================================
fun demoScopeFunctions(): String {
    data class Persona(var nombre: String = "", var edad: Int = 0)

    // apply: CONFIGURA un objeto y lo devuelve (this)
    val persona = Persona().apply {
        nombre = "Juan"
        edad = 30
    }

    // let: transforma y devuelve el RESULTADO (it)
    val enMayusculas = persona.nombre.let { it.uppercase() }

    // run: como apply pero devuelve la última línea
    val edadEn10Anios = persona.run { edad + 10 }

    // also: "haz algo extra" (logs) sin alterar la cadena
    val log = StringBuilder()
    persona.also { log.append("[log] procesando a ${it.nombre}") }

    return "apply creó: $persona\nlet: $enMayusculas | run: $edadEn10Anios\n$log"
}

// ============================================================
// --- 9. FUNCIONES DE EXTENSIÓN: añade métodos a clases ajenas ---
// ============================================================
// ¡Le estamos añadiendo un método a String sin tocar su código!
fun String.esPalindromo(): Boolean {
    val limpio = this.lowercase().filter { it.isLetterOrDigit() }
    return limpio == limpio.reversed()
}

// Y a las listas de enteros:
fun List<Int>.segundoMasGrande(): Int? = this.distinct().sortedDescending().getOrNull(1)

fun demoExtensiones(): String {
    return "\"Anita lava la tina\".esPalindromo() = ${"Anita lava la tina".esPalindromo()}\n" +
            "\"Kotlin\".esPalindromo() = ${"Kotlin".esPalindromo()}\n" +
            "listOf(3,7,7,1).segundoMasGrande() = ${listOf(3, 7, 7, 1).segundoMasGrande()}"
}

// ============================================================
// --- 10. GENÉRICOS: código que funciona con cualquier tipo ---
// ============================================================
// <T> hace la función reutilizable con cualquier tipo de lista
fun <T> primeroYUltimo(lista: List<T>): Pair<T, T>? =
    if (lista.isEmpty()) null else Pair(lista.first(), lista.last())

fun demoGenericos(): String {
    val conNumeros = primeroYUltimo(listOf(10, 20, 30))
    val conTextos = primeroYUltimo(listOf("alfa", "beta", "gamma"))
    return "Con Int: $conNumeros | Con String: $conTextos\n" +
            "La MISMA función sirve para ambos tipos gracias a <T>"
}

// ============================================================
// --- LA PANTALLA INTERACTIVA ---
// ============================================================
@Composable
fun PantallaKotlin() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kotlin Interactivo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pulsa 'Ejecutar' en cada tarjeta y compara el resultado con el " +
                    "código fuente de KotlinParaAndroid.kt",
            style = MaterialTheme.typography.bodySmall
        )

        TarjetaDemo("1. Variables y tipos", "val, var, inferencia y string templates") { demoVariables() }
        TarjetaDemo("2. Null Safety", "?. llamada segura y ?: operador Elvis") { demoNullSafety() }
        TarjetaDemo("3. Colecciones y lambdas", "filter, map, sumOf, groupBy") { demoColecciones() }
        TarjetaDemo("4. When", "el super-switch que devuelve valores") {
            (listOf(-5, 0, 7, 42, 1000)).joinToString("\n") { demoWhen(it) }
        }
        TarjetaDemo("5. Clases y herencia", "open, override, interfaces, polimorfismo") { demoClases() }
        TarjetaDemo("6. Data classes", "copy, equals por contenido, destructuring") { demoDataClass() }
        TarjetaDemo("7. Sealed classes", "estados de UI con when exhaustivo") {
            demoSealed(UIState.Loading) + "\n" +
                    demoSealed(UIState.Success(listOf("A", "B"))) + "\n" +
                    demoSealed(UIState.Error("sin internet"))
        }
        TarjetaDemo("8. Scope functions", "let, apply, run, also") { demoScopeFunctions() }
        TarjetaDemo("9. Funciones de extensión", "añade métodos a String o List") { demoExtensiones() }
        TarjetaDemo("10. Genéricos", "funciones <T> para cualquier tipo") { demoGenericos() }
    }
}

// Componente reutilizable: título + descripción + botón + resultado
@Composable
fun TarjetaDemo(titulo: String, descripcion: String, codigo: () -> String) {
    var resultado by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(descripcion, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { resultado = codigo() }) {
                Text(if (resultado == null) "▶ Ejecutar" else "▶ Ejecutar de nuevo")
            }
            resultado?.let {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BulletPoint(texto: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
