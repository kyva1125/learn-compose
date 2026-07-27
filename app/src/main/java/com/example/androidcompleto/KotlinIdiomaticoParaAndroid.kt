package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * KOTLIN IDIOMÁTICO — PISTA KOTLIN (Lección K7)
 *
 * Los detalles que separan el Kotlin "que funciona" del Kotlin que un
 * revisor senior aprueba sin comentarios. Nada exótico: cosas que usarás
 * cada semana y que salen en entrevistas.
 *
 * 1. SEQUENCES: evaluación perezosa. Con listas, cada operador crea una
 *    lista intermedia completa. Con sequences no se crea nada y los
 *    elementos pasan de uno en uno por toda la cadena.
 * 2. CHANNELS: comunicación entre corrutinas (Flow es "frío", Channel es
 *    una cola caliente entre productor y consumidor).
 * 3. Operadores de colección que casi nadie conoce: fold, zip, windowed,
 *    partition, groupingBy, chunked.
 * 4. Azúcar sintáctico: infix, destructuring, companion object.
 * 5. Result / runCatching: errores como VALOR, sin try/catch por todas partes.
 */

// ============================================================
// --- 1. INFIX: funciones que se leen como lenguaje natural ---
// ============================================================
/**
 * `infix` permite llamar sin punto ni paréntesis: `5 sumadoA 3`.
 * Requisitos: función miembro o extensión, UN solo parámetro, sin varargs.
 * Ya lo usas: `to` (para Pairs), `until`, `downTo`, `step` son infix.
 */
infix fun Int.elevadoA(exponente: Int): Long {
    var r = 1L
    repeat(exponente) { r *= this }
    return r
}

data class Medida(val valor: Double, val unidad: String)

infix fun Double.en(unidad: String) = Medida(this, unidad)

// ============================================================
// --- 2. COMPANION OBJECT: "estáticos" y fábricas ---
// ============================================================
/**
 * Kotlin no tiene `static`. El companion object es el equivalente, y
 * además es un OBJETO real: puede implementar interfaces y tener estado.
 * Uso más común: constructores con nombre (fábricas) y constantes.
 */
class Temperatura private constructor(val celsius: Double) {

    val fahrenheit: Double get() = celsius * 9 / 5 + 32

    // El companion se usa para VALIDAR antes de construir:
    // el constructor es privado, así que la única entrada es la fábrica.
    companion object {
        const val CERO_ABSOLUTO = -273.15

        fun desdeCelsius(c: Double): Result<Temperatura> =
            if (c < CERO_ABSOLUTO) Result.failure(IllegalArgumentException("Bajo el cero absoluto"))
            else Result.success(Temperatura(c))

        fun desdeFahrenheit(f: Double) = desdeCelsius((f - 32) * 5 / 9)
    }
}

// ============================================================
// --- 3. DESTRUCTURING: repartir un objeto en variables ---
// ============================================================
/**
 * Toda data class genera component1(), component2()... y eso habilita
 * `val (a, b) = objeto`. También puedes dárselo a una clase normal
 * declarando los operator fun componentN() a mano.
 */
class Coordenada(val x: Int, val y: Int) {
    operator fun component1() = x
    operator fun component2() = y
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaKotlinIdiomatico() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kotlin idiomático", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Sequences, channels, operadores poco conocidos y azúcar sintáctico. " +
                        "Lo que hace que tu código pase una revisión sin comentarios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            TarjetaDemo(
                "1. Sequences: evaluación perezosa",
                "Con listas, cada operador recorre TODO y crea una lista nueva. Con sequence, no."
            ) {
                val numeros = (1..10).toList()
                val pasosLista = mutableListOf<String>()
                val pasosSecuencia = mutableListOf<String>()

                // --- LISTA: eager (ansiosa). Hace map de los 10, luego filter de los 10 ---
                val resultadoLista = numeros
                    .map { pasosLista += "map($it)"; it * 2 }
                    .filter { pasosLista += "filter($it)"; it > 10 }
                    .take(2)

                // --- SEQUENCE: lazy. Cada elemento cruza toda la cadena y para al llegar a 2 ---
                val resultadoSecuencia = numeros.asSequence()
                    .map { pasosSecuencia += "map($it)"; it * 2 }
                    .filter { pasosSecuencia += "filter($it)"; it > 10 }
                    .take(2)
                    .toList()   // ← nada se ejecuta hasta esta operación TERMINAL

                """
                Resultado (idéntico): $resultadoLista

                Lista     → ${pasosLista.size} operaciones
                Sequence  → ${pasosSecuencia.size} operaciones

                Traza de la sequence:
                ${pasosSecuencia.joinToString(" → ")}

                Se paró en cuanto tuvo 2 resultados. La lista procesó los 10
                dos veces y creó 2 listas intermedias.

                ⚠ Regla real: usa Sequence con colecciones GRANDES o cadenas
                largas de operadores. Para 10 elementos, la lista es más rápida
                (crear la sequence tiene su propio coste).
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "2. Channels: cola entre corrutinas",
                "Flow es frío (se ejecuta al colectar). Channel es caliente: una cola real."
            ) {
                runBlocking {
                    // Un Channel es una cola: alguien envía, alguien recibe
                    val canal = Channel<Int>(capacity = 3)
                    val recibidos = mutableListOf<Int>()

                    // produce = atajo que crea corrutina + channel de salida
                    val productor = produce {
                        repeat(5) { send(it * 10) }
                    }
                    recibidos += productor.consumeAsFlow().toList()

                    canal.close()

                    """
                    Productor envió 5 valores → recibidos: $recibidos

                    CHANNEL vs FLOW (pregunta clásica de entrevista):
                      Flow    = FRÍO. Cada colector reejecuta el bloque desde cero.
                      Channel = CALIENTE. Los valores existen aunque nadie escuche;
                                cada valor lo consume UN solo receptor.

                    Cuándo usar Channel: repartir trabajo entre workers, o eventos
                    de-una-sola-vez. Para estado de UI, usa StateFlow.
                    Nota: hoy casi siempre prefieres SharedFlow antes que Channel.
                    """.trimIndent()
                }
            }
        }

        item {
            TarjetaDemo(
                "3. Operadores de colección que casi nadie usa",
                "fold, zip, windowed, partition, chunked, groupingBy: menos bucles a mano."
            ) {
                val ventas = listOf(120, 340, 90, 500, 210, 75, 400)
                val meses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul")

                // fold: reduce con valor inicial (reduce falla en listas vacías)
                val total = ventas.fold(0) { acumulado, v -> acumulado + v }

                // zip: empareja dos listas
                val porMes = (meses zip ventas).joinToString { "${it.first}:${it.second}" }

                // partition: divide en dos según un predicado (una sola pasada)
                val (buenas, malas) = ventas.partition { it >= 200 }

                // windowed: ventanas deslizantes — ideal para medias móviles
                val mediaMovil = ventas.windowed(size = 3).map { it.average().toInt() }

                // chunked: trocea en bloques
                val trimestres = ventas.chunked(3)

                // groupingBy + eachCount: contar por categoría sin crear listas
                val porTamano = ventas.groupingBy {
                    when { it < 100 -> "baja"; it < 400 -> "media"; else -> "alta" }
                }.eachCount()

                // runningFold: como fold pero devolviendo cada paso (acumulado)
                val acumulado = ventas.runningFold(0) { a, v -> a + v }.drop(1)

                """
                Ventas: $ventas

                fold (total)        → $total
                zip (mes:venta)     → $porMes
                partition           → buenas=$buenas / malas=$malas
                windowed(3) media   → $mediaMovil
                chunked(3)          → $trimestres
                groupingBy+eachCount→ $porTamano
                runningFold         → $acumulado

                Cada uno de estos sustituye un bucle con variable mutable.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "4. infix: código que se lee como una frase",
                "Sin punto ni paréntesis. Ya lo usas con 'to', 'until' y 'downTo'."
            ) {
                val potencia = 2 elevadoA 10
                val distancia = 5.5 en "km"
                val rango = (1 until 5).toList()
                val par = "clave" to "valor"

                """
                2 elevadoA 10   → $potencia
                5.5 en "km"     → $distancia
                1 until 5       → $rango
                "clave" to "valor" → $par

                Requisitos de infix: un solo parámetro, sin valor por defecto
                y sin vararg. Úsalo con moderación: solo cuando de verdad
                se lea mejor que una llamada normal.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "5. companion object: fábricas con validación",
                "Constructor privado + fábrica = objetos imposibles de crear en estado inválido."
            ) {
                val valida = Temperatura.desdeCelsius(25.0)
                val invalida = Temperatura.desdeCelsius(-300.0)
                val desdeF = Temperatura.desdeFahrenheit(98.6)

                """
                desdeCelsius(25.0)     → ${valida.getOrNull()?.celsius}°C = ${
                    "%.1f".format(valida.getOrNull()?.fahrenheit)
                }°F
                desdeCelsius(-300.0)   → ✖ ${invalida.exceptionOrNull()?.message}
                desdeFahrenheit(98.6)  → ${"%.1f".format(desdeF.getOrNull()?.celsius)}°C
                Constante del companion: ${Temperatura.CERO_ABSOLUTO}

                El constructor es PRIVADO: la única forma de crear una
                Temperatura pasa por la validación. Estado inválido: imposible.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "6. Destructuring: repartir en variables",
                "Toda data class lo trae; en una clase normal lo declaras con componentN()."
            ) {
                val punto = Coordenada(3, 7)
                val (x, y) = punto                        // clase normal con componentN()

                val usuario = Medida(72.5, "kg")
                val (peso, unidad) = usuario              // data class: gratis

                val mapa = mapOf("a" to 1, "b" to 2)
                val entradas = mapa.map { (clave, valor) -> "$clave=$valor" }  // en lambdas

                val (primero, segundo) = listOf("uno", "dos")  // también en listas

                """
                val (x, y) = Coordenada(3, 7)     → x=$x, y=$y
                val (peso, unidad) = Medida(...)  → $peso $unidad
                map { (clave, valor) -> ... }     → $entradas
                val (primero, segundo) = listOf() → $primero, $segundo

                Ojo: el destructuring va POR POSICIÓN, no por nombre. Si
                reordenas los campos de la data class, todo el código que
                desestructura cambia de significado SIN avisar. Es la razón
                de que se recomiende para clases pequeñas y estables.
                """.trimIndent()
            }
        }

        item {
            TarjetaDemo(
                "7. Result y runCatching: errores como valor",
                "En vez de try/catch esparcido, el error viaja como dato y se compone."
            ) {
                fun dividir(a: Int, b: Int): Result<Int> = runCatching { a / b }

                val ok = dividir(10, 2)
                val fallo = dividir(10, 0)

                // Lo potente: encadenar transformaciones sin try/catch
                val encadenado = dividir(100, 5)
                    .map { it * 2 }
                    .mapCatching { require(it < 100) { "demasiado grande" }; it }
                    .getOrElse { -1 }

                val recuperado = fallo.recover { 0 }.getOrNull()

                """
                dividir(10, 2)  → ${ok.getOrNull()}
                dividir(10, 0)  → ✖ ${fallo.exceptionOrNull()?.let { it::class.simpleName }}
                encadenado      → $encadenado
                fallo.recover{0}→ $recuperado

                fold para tratar ambos casos de una vez:
                ${ok.fold(onSuccess = { "✔ $it" }, onFailure = { "✖ ${it.message}" })}

                ⚠ NO uses runCatching alrededor de código con corrutinas sin
                cuidado: atrapa también CancellationException y rompe la
                cancelación. En ese caso, captura excepciones concretas.
                """.trimIndent()
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta idiomática", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("Sequence para colecciones grandes o cadenas largas; lista para pocas")
                    BulletPoint("Sequence necesita una operación TERMINAL (toList, first, sum)")
                    BulletPoint("Flow = frío y reejecutable · Channel = caliente, un solo receptor")
                    BulletPoint("fold > reduce (reduce revienta con lista vacía)")
                    BulletPoint("partition, windowed, chunked y groupingBy sustituyen bucles")
                    BulletPoint("Constructor privado + companion = imposible crear objeto inválido")
                    BulletPoint("Destructuring es POR POSICIÓN: cuidado al reordenar campos")
                    BulletPoint("runCatching atrapa CancellationException: úsalo con cabeza")
                }
            }
        }
    }
}
