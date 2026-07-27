package com.example.androidcompleto

import android.graphics.Color as ColorAndroid
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CalendarView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * INTEROPERABILIDAD CON VIEWS — PISTA COMPOSE (Lección C10)
 *
 * Este tema no aparece en los tutoriales bonitos, pero es EL PRIMERO que
 * usarás en un trabajo real: casi ninguna app se reescribe entera. Vas a
 * entrar a una app con XML y tendrás que meter Compose dentro, o meter
 * una View que Compose no tiene (mapas, WebView, un SDK de terceros).
 *
 * LOS DOS SENTIDOS:
 *
 *   Views  →  Compose:  ComposeView dentro de un XML
 *   Compose → Views:    AndroidView { } dentro de un @Composable   ← esta lección
 *
 * ANATOMÍA DE AndroidView (los 3 parámetros que importan):
 *
 *   AndroidView(
 *     factory = { context -> MiView(context) },  // se llama UNA vez: crear
 *     update  = { view -> view.texto = estado }, // en cada recomposición: sincronizar
 *     onRelease = { view -> view.limpiar() }     // al salir: liberar recursos
 *   )
 *
 * REGLA MENTAL: factory = onCreate. update = "vincular estado a la View".
 * El puente es de una dirección: Compose empuja estado a la View, y la
 * View avisa de vuelta mediante callbacks (listeners) que actualizan tu estado.
 *
 * ⚠️ Si creas la View dentro de update, la recrearás en cada recomposición.
 *    Crear SIEMPRE en factory.
 */

// ============================================================
// --- 1. Una View clásica controlada por estado de Compose ---
// ============================================================
@Composable
fun DemoTextViewClasico() {
    var contador by remember { mutableIntStateOf(0) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Un TextView de toda la vida", style = MaterialTheme.typography.titleMedium)
            Text(
                "El botón es Compose; el texto de abajo es un android.widget.TextView " +
                        "real. 'update' lo sincroniza con el estado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    // Se ejecuta UNA sola vez: aquí se construye la View
                    TextView(context).apply {
                        textSize = 18f
                        setTextColor(ColorAndroid.parseColor("#6650a4"))
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                update = { textView ->
                    // Se ejecuta en cada recomposición donde cambie lo que lee
                    textView.text = "Soy un TextView y el contador vale $contador"
                }
            )

            Button(onClick = { contador++ }) { Text("Incrementar desde Compose") }
        }
    }
}

// ============================================================
// --- 2. Comunicación de vuelta: la View avisa a Compose ---
// ============================================================
@Composable
fun DemoRatingBarBidireccional() {
    var estrellas by remember { mutableFloatStateOf(3f) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("2. Comunicación en los dos sentidos",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "El RatingBar (View) avisa a Compose con un listener; el Slider (Compose) " +
                        "empuja el valor de vuelta a la View. Estado único, dos mundos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            AndroidView(
                factory = { context ->
                    RatingBar(context).apply {
                        numStars = 5
                        stepSize = 1f
                        // La View → Compose: el listener actualiza NUESTRO estado
                        setOnRatingBarChangeListener { _, valor, deUsuario ->
                            if (deUsuario) estrellas = valor
                        }
                    }
                },
                update = { barra ->
                    // Compose → la View: solo si de verdad cambió (evita bucles)
                    if (barra.rating != estrellas) barra.rating = estrellas
                }
            )

            Text("Valor compartido: ${estrellas.toInt()} estrellas",
                style = MaterialTheme.typography.labelLarge)
            Slider(
                value = estrellas,
                onValueChange = { estrellas = it },
                valueRange = 0f..5f,
                steps = 4
            )
        }
    }
}

// ============================================================
// --- 3. Una View que Compose NO tiene: WebView ---
// ============================================================
@Composable
fun DemoWebView() {
    val html = """
        <html><body style="font-family:sans-serif;padding:12px;background:#f3edf7">
        <h3>Soy un WebView</h3>
        <p>Compose no tiene componente de navegador. Para HTML, mapas o
        reproductores de vídeo, <b>AndroidView es la respuesta</b>.</p>
        </body></html>
    """.trimIndent()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("3. WebView: lo que Compose no cubre",
                style = MaterialTheme.typography.titleMedium)
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { webView ->
                    // ¡Importante! Liberar recursos al salir de la composición
                    webView.destroy()
                }
            )
            Text(
                "Fíjate en onRelease: un WebView o un MapView sin liberar es una fuga " +
                        "de memoria garantizada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 4. CalendarView: View compleja con estado propio ---
// ============================================================
@Composable
fun DemoCalendarView() {
    var fecha by remember { mutableStateOf("ninguna") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("4. CalendarView", style = MaterialTheme.typography.titleMedium)
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    CalendarView(context).apply {
                        setOnDateChangeListener { _, anio, mes, dia ->
                            fecha = "%02d/%02d/%d".format(dia, mes + 1, anio)
                        }
                    }
                }
            )
            Text("Fecha elegida: $fecha", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaInterop() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Interop con el sistema de Views", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Todo lo de esta pantalla mezcla Compose y Views clásicas de Android " +
                        "funcionando juntas de verdad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoTextViewClasico() }
        item { DemoRatingBarBidireccional() }
        item { DemoWebView() }
        item { DemoCalendarView() }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("El sentido contrario: Compose dentro de XML",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Es la forma REAL de migrar una app grande: pantalla a pantalla, " +
                                "sin parar el desarrollo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "1) Un ComposeView en tu layout XML",
                        """
                        <androidx.compose.ui.platform.ComposeView
                            android:id="@+id/compose_view"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content" />
                        """.trimIndent()
                    )
                    Spacer(Modifier.height(8.dp))
                    BloqueCodigo(
                        "2) Y le pones contenido desde el Fragment/Activity",
                        """
                        binding.composeView.apply {
                            // Sin esto la composición no se libera con la vista
                            setViewCompositionStrategy(
                                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                            )
                            setContent {
                                MiTema { MiPantallaCompose() }
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
                    Text("📌 Chuleta de interop", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("factory = crear (una vez) · update = sincronizar estado")
                    BulletPoint("NUNCA crees la View dentro de update")
                    BulletPoint("onRelease para liberar: WebView, MapView, players")
                    BulletPoint("View → Compose: listeners que escriben en tu estado")
                    BulletPoint("Compose → View: en update, comprobando si cambió (evita bucles)")
                    BulletPoint("XML → Compose: ComposeView + DisposeOnViewTreeLifecycleDestroyed")
                    BulletPoint("AndroidViewBinding si ya usas ViewBinding en ese layout")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Estrategia de migración real: empieza por las hojas (celdas de lista, " +
                                "diálogos), sigue por pantallas completas y deja la navegación " +
                                "para el final.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
