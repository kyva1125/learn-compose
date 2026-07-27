package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * KOTLIN AVANZADO — PISTA KOTLIN (Lección K2)
 *
 * Estos son los temas que separan a quien "escribe Kotlin como Java"
 * de quien escribe Kotlin idiomático. Salen en TODA entrevista senior.
 *
 * Pulsa cada botón: el código se ejecuta de verdad y ves el resultado.
 */

// ============================================================
// --- 1. GENÉRICOS Y VARIANZA (in / out) ---
// ============================================================
/**
 * `out T` = COVARIANTE: la clase solo PRODUCE T (lo devuelve).
 * Permite que Caja<Perro> sea subtipo de Caja<Animal>.
 * Regla mnemotécnica: out = sale, in = entra.
 */
interface Productor<out T> {
    fun producir(): T
}

/** `in T` = CONTRAVARIANTE: la clase solo CONSUME T (lo recibe). */
interface Consumidor<in T> {
    fun consumir(item: T)
}

// (La clase Animal ya existe en la Lección 1; aquí usamos una jerarquía propia
//  para no chocar con ella y centrarnos en la varianza.)
open class Felino(val nombre: String)
class Gato(nombre: String) : Felino(nombre)

class CriaderoDeGatos : Productor<Gato> {
    override fun producir() = Gato("Michi")
}

/**
 * reified: normalmente el tipo genérico se BORRA en runtime (type erasure)
 * y no puedes hacer `T::class`. Con `inline` + `reified` sí puedes.
 */
inline fun <reified T> List<Any>.soloDelTipo(): List<T> = filterIsInstance<T>()

inline fun <reified T> nombreDelTipo(): String = T::class.simpleName ?: "?"

// ============================================================
// --- 2. DELEGADOS DE PROPIEDAD (by) ---
// ============================================================
/**
 * `by` delega el get/set de una propiedad a otro objeto.
 * Ya lo usas sin saberlo: `var x by remember { mutableStateOf(0) }`
 * y `val vm by viewModels()` son delegados.
 */
class DelegadoQueRegistra(private var valor: String) {
    val historial = mutableListOf<String>()

    // Firma exacta que Kotlin exige para un delegado de lectura/escritura
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        historial += "leí ${property.name}"
        return valor
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, nuevo: String) {
        historial += "escribí ${property.name}: '$valor' → '$nuevo'"
        valor = nuevo
    }
}

class Configuracion {
    val espia = DelegadoQueRegistra("es")
    var idioma: String by espia

    // Delegados de la librería estándar:
    val conexionCara: String by lazy {   // se calcula UNA sola vez, al primer acceso
        "conexión abierta (calculada solo al usarse)"
    }
    var contador: Int by Delegates.observable(0) { prop, viejo, nuevo ->
        cambios += "${prop.name}: $viejo → $nuevo"
    }
    val cambios = mutableListOf<String>()
}

// ============================================================
// --- 3. FUNCIONES INLINE, CROSSINLINE Y NOINLINE ---
// ============================================================
/**
 * `inline` copia el cuerpo de la función en la llamada: evita crear un
 * objeto Function por cada lambda. Por eso `forEach`, `let` y `run` son inline.
 * Bonus: dentro de una lambda inline SÍ puedes hacer `return` de la función externa.
 */
inline fun medirTiempo(bloque: () -> Unit): Long {
    val inicio = System.nanoTime()
    bloque()
    return (System.nanoTime() - inicio) / 1_000
}

/** `crossinline` prohíbe el return no-local: necesario si la lambda se ejecuta en otro contexto. */
inline fun ejecutarDosVeces(crossinline bloque: () -> String): String =
    "${bloque()} | ${bloque()}"

// ============================================================
// --- 4. SOBRECARGA DE OPERADORES ---
// ============================================================
data class Dinero(val centimos: Int) {
    operator fun plus(otro: Dinero) = Dinero(centimos + otro.centimos)
    operator fun times(factor: Int) = Dinero(centimos * factor)
    operator fun compareTo(otro: Dinero) = centimos.compareTo(otro.centimos)
    override fun toString() = "S/ %.2f".format(centimos / 100.0)
}

// ============================================================
// --- 5. VALUE CLASS (antes inline class) ---
// ============================================================
/**
 * Envuelve un tipo primitivo SIN costo en runtime: el compilador
 * lo reemplaza por el String desnudo. Te da seguridad de tipos gratis:
 * ya no puedes pasar un UserId donde va un Email.
 */
@JvmInline
value class UserId(val valor: String)

@JvmInline
value class Email(val valor: String) {
    init { require("@" in valor) { "Email inválido: $valor" } }
}

fun enviarCorreo(a: Email, de: UserId) = "Correo a ${a.valor} enviado por ${de.valor}"

// ============================================================
// --- 6. DSL CON RECEPTORES (type-safe builders) ---
// ============================================================
/**
 * Un lambda "con receptor" (A.() -> Unit) hace que dentro del bloque
 * `this` sea el objeto. Así funcionan Compose, Gradle KTS y kotlinx.html.
 * @DslMarker evita mezclar scopes anidados por error.
 */
@DslMarker
annotation class MenuDsl

@MenuDsl
class MenuBuilder {
    private val platos = mutableListOf<String>()
    var titulo: String = "Menú"

    fun plato(nombre: String, precio: Double) {
        platos += "  • $nombre — S/ %.2f".format(precio)
    }

    fun construir() = (listOf("$titulo:") + platos).joinToString("\n")
}

fun menu(bloque: MenuBuilder.() -> Unit): String =
    MenuBuilder().apply(bloque).construir()

// ============================================================
// --- 7. SEALED + INTERFACES SELLADAS EXHAUSTIVAS ---
// ============================================================
sealed interface Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>
    data class Error(val causa: String) : Resultado<Nothing>
    data object Cargando : Resultado<Nothing>
}

/** `when` sobre un sealed NO necesita `else`: el compilador verifica que estén todos. */
fun <T> describir(r: Resultado<T>): String = when (r) {
    is Resultado.Exito -> "✔ ${r.dato}"
    is Resultado.Error -> "✖ ${r.causa}"
    Resultado.Cargando -> "⏳ cargando"
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaKotlinAvanzado() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kotlin avanzado", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Genéricos, delegados, inline, operadores, value classes y DSLs. " +
                        "Todo esto es Kotlin puro: funciona igual en Android, iOS y servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            TarjetaDemo(
                "1. Varianza: out (produce) e in (consume)",
                "Un Productor<Gato> se puede usar donde piden Productor<Animal> gracias a 'out'."
            ) {
                val criadero: Productor<Gato> = CriaderoDeGatos()
                // Esto SOLO compila porque Productor es <out T>:
                val comoAnimal: Productor<Felino> = criadero
                val a = comoAnimal.producir()

                val consumidor = object : Consumidor<Felino> {
                    override fun consumir(item: Felino) {}
                }
                // Y esto solo compila porque Consumidor es <in T>:
                val comoGatos: Consumidor<Gato> = consumidor
                comoGatos.consumir(Gato("Pelusa"))

                """
                Productor<Gato> asignado a Productor<Felino> ✔ (covarianza 'out')
                  → produjo: ${a.nombre}
                Consumidor<Felino> asignado a Consumidor<Gato> ✔ (contravarianza 'in')

                Regla: 'out' si solo lo DEVUELVES, 'in' si solo lo RECIBES.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "2. reified: el tipo sobrevive en runtime",
                "Sin 'inline + reified' no podrías escribir T::class ni filtrar por tipo."
            ) {
                val mezcla: List<Any> = listOf(1, "hola", 2.5, "mundo", 3, true)
                val textos = mezcla.soloDelTipo<String>()
                val enteros = mezcla.soloDelTipo<Int>()

                """
                Lista original: $mezcla
                soloDelTipo<String>() → $textos
                soloDelTipo<Int>()    → $enteros
                nombreDelTipo<Dinero>() → ${nombreDelTipo<Dinero>()}

                Sin 'reified' el tipo se borra (type erasure) y esto era imposible.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "3. Delegados de propiedad (by)",
                "Lo mismo que hace 'var x by remember { ... }' en Compose, hecho a mano."
            ) {
                val config = Configuracion()
                config.idioma            // lectura → queda registrada
                config.idioma = "qu"     // escritura → queda registrada
                config.contador = 5      // observable dispara callback
                config.contador = 8

                """
                --- Delegado propio (getValue/setValue) ---
                ${config.espia.historial.joinToString("\n")}

                --- by Delegates.observable ---
                ${config.cambios.joinToString("\n")}

                --- by lazy (se calcula al primer acceso) ---
                ${config.conexionCara}
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "4. inline y crossinline",
                "inline evita crear objetos Function por cada lambda. Por eso forEach es gratis."
            ) {
                val micros = medirTiempo {
                    var s = 0L
                    repeat(100_000) { s += it }
                }
                val doble = ejecutarDosVeces { "ping" }

                """
                medirTiempo { ... } → ${micros} µs (la lambda se copió en el sitio)
                ejecutarDosVeces { "ping" } → $doble

                inline      = copia el cuerpo, permite 'return' no-local
                crossinline = inline PERO prohíbe el return no-local
                noinline    = esta lambda concreta NO se inlinea (para guardarla)
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "5. Sobrecarga de operadores",
                "Defines plus, times, compareTo... y tu tipo se usa con +, * y <."
            ) {
                val precio = Dinero(1250)
                val envio = Dinero(500)
                val total = precio + envio
                val triple = precio * 3

                """
                Dinero(1250) + Dinero(500) = $total
                Dinero(1250) * 3           = $triple
                ¿precio > envio?           = ${precio > envio}

                Con 'operator fun compareTo' obtienes <, >, <= y >= de regalo.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "6. value class: seguridad de tipos sin costo",
                "En runtime es un String pelado, pero el compilador ya no te deja confundirlos."
            ) {
                val id = UserId("u-42")
                val correo = Email("jose@ejemplo.com")
                val resultado = enviarCorreo(a = correo, de = id)

                val invalido = runCatching { Email("sin-arroba") }
                    .exceptionOrNull()?.message

                """
                $resultado

                enviarCorreo(a = id, de = correo) → NO COMPILA ✔
                (con Strings sueltos habrías invertido los argumentos sin enterarte)

                Email("sin-arroba") → $invalido
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "7. DSL con receptor: así funciona Compose por dentro",
                "MenuBuilder.() -> Unit hace que dentro del bloque 'this' sea el builder."
            ) {
                menu {
                    titulo = "Menú del día"      // 'this' es el MenuBuilder
                    plato("Ceviche", 32.0)
                    plato("Lomo saltado", 28.5)
                    plato("Suspiro limeño", 12.0)
                } + "\n\nEl mismo truco de Column { }, buildString { } y Gradle KTS."
            }
        }

        item {
            TarjetaDemo(
                "8. sealed genérico + when exhaustivo",
                "El patrón de UiState que usas en toda app real, con genéricos y Nothing."
            ) {
                val casos = listOf(
                    Resultado.Exito("42 usuarios"),
                    Resultado.Error("timeout"),
                    Resultado.Cargando
                )
                casos.joinToString("\n") { describir(it) } +
                        "\n\nOjo con 'Resultado<Nothing>': Nothing es subtipo de TODO, " +
                        "por eso Error y Cargando valen para cualquier Resultado<T>."
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta Kotlin avanzado", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("out = produce (covariante) · in = consume (contravariante)")
                    BulletPoint("reified solo existe dentro de funciones inline")
                    BulletPoint("by lazy (una vez) · by observable (callback) · delegado propio")
                    BulletPoint("inline para lambdas de alto uso; crossinline si escapa el scope")
                    BulletPoint("value class = tipos seguros con cero coste en runtime")
                    BulletPoint("DSL = lambda con receptor A.() -> Unit + apply")
                    BulletPoint("sealed + when sin else = el compilador te cubre las espaldas")
                }
            }
        }
    }
}
