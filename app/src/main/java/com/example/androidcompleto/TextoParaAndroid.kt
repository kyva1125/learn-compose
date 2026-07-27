package com.example.androidcompleto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TEXTO Y ENTRADA AVANZADOS — PISTA COMPOSE (Lección C16)
 *
 * Los formularios de la lección C7 eran TextFields sencillos. En una app
 * real necesitas mucho más: texto con estilos mezclados, máscaras de
 * entrada (teléfono, tarjeta), el teclado correcto en cada campo, y
 * mover el foco de un campo al siguiente.
 *
 * LAS 4 PIEZAS:
 *
 * 1. AnnotatedString: un String con TRAMOS de estilo distinto. Es lo que
 *    permite negritas, colores y enlaces dentro de un mismo Text.
 *
 * 2. VisualTransformation: separa lo que el usuario VE de lo que tú
 *    GUARDAS. El estado es "987654321"; la pantalla muestra "987 654 321".
 *    Clave: hay que mapear también las posiciones del cursor (OffsetMapping),
 *    o el cursor saltará a sitios absurdos.
 *
 * 3. KeyboardOptions / KeyboardActions: qué teclado sale (números, email)
 *    y qué hace la tecla de acción (Siguiente, Buscar, Listo).
 *
 * 4. FocusRequester y FocusManager: dar el foco a un campo, saltar al
 *    siguiente, y cerrar el teclado al terminar.
 */

// ============================================================
// --- 1. AnnotatedString: estilos dentro de un mismo texto ---
// ============================================================
@Composable
fun DemoAnnotatedString() {
    val texto = buildAnnotatedString {
        append("Compose permite ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("negrita") }
        append(", ")
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("color") }
        append(", ")
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("cursiva") }
        append(" y ")
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append("tachado") }
        append(" en UN SOLO Text.")
    }

    val precio = buildAnnotatedString {
        withStyle(SpanStyle(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 14.sp
        )) { append("S/ 199.00") }
        append("  ")
        withStyle(SpanStyle(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            fontSize = 20.sp
        )) { append("S/ 149.00") }
    }

    val resaltado = remember {
        val fuente = "El buscador resalta la palabra Kotlin dentro del resultado"
        val termino = "Kotlin"
        buildAnnotatedString {
            val i = fuente.indexOf(termino)
            append(fuente.substring(0, i))
            withStyle(SpanStyle(
                background = Color(0xFFFFF176),
                fontWeight = FontWeight.Bold
            )) { append(termino) }
            append(fuente.substring(i + termino.length))
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1. AnnotatedString", style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Text("Caso real: precio rebajado", style = MaterialTheme.typography.labelLarge)
            Text(precio)
            HorizontalDivider()
            Text("Caso real: resaltar la búsqueda", style = MaterialTheme.typography.labelLarge)
            Text(resaltado, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Sin AnnotatedString tendrías que partir el texto en varios Text dentro " +
                        "de un Row — y se rompería al hacer salto de línea.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 2. VisualTransformation: máscaras de entrada ---
// ============================================================
/**
 * Muestra 987654321 como "987 654 321".
 * Lo difícil NO es insertar los espacios: es el OffsetMapping, que traduce
 * posiciones entre el texto original y el mostrado. Sin él, el cursor y la
 * selección se vuelven locos.
 */
class TransformacionTelefono : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text.take(9)
        val formateado = digitos.chunked(3).joinToString(" ")

        val mapeo = object : OffsetMapping {
            // original → mostrado: se suma 1 por cada espacio insertado antes
            override fun originalToTransformed(offset: Int): Int {
                val espacios = (offset - 1).coerceAtLeast(0) / 3
                return (offset + espacios).coerceAtMost(formateado.length)
            }
            // mostrado → original: se resta el número de espacios
            override fun transformedToOriginal(offset: Int): Int {
                val espacios = offset / 4
                return (offset - espacios).coerceIn(0, digitos.length)
            }
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}

/** Muestra 4111111111111111 como "4111 1111 1111 1111". */
class TransformacionTarjeta : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text.take(16)
        val formateado = digitos.chunked(4).joinToString(" ")

        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val espacios = (offset - 1).coerceAtLeast(0) / 4
                return (offset + espacios).coerceAtMost(formateado.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val espacios = offset / 5
                return (offset - espacios).coerceIn(0, digitos.length)
            }
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}

@Composable
fun DemoMascaras() {
    var telefono by remember { mutableStateOf("") }
    var tarjeta by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. Máscaras (VisualTransformation)",
                style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = telefono,
                // Filtramos en la ENTRADA: el estado solo guarda dígitos
                onValueChange = { nuevo -> telefono = nuevo.filter { it.isDigit() }.take(9) },
                label = { Text("Teléfono") },
                visualTransformation = TransformacionTelefono(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Estado guardado: \"$telefono\"  ·  mostrado con espacios",
                style = MaterialTheme.typography.labelSmall)

            OutlinedTextField(
                value = tarjeta,
                onValueChange = { nuevo -> tarjeta = nuevo.filter { it.isDigit() }.take(16) },
                label = { Text("Tarjeta") },
                visualTransformation = TransformacionTarjeta(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña") },
                // La transformación de contraseña ya viene hecha
                visualTransformation = if (verClave) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { verClave = !verClave }) {
                        Text(if (verClave) "Ocultar" else "Ver")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Regla de oro: el ESTADO guarda el dato limpio (solo dígitos). El formato " +
                        "es solo presentación. Así no tienes que limpiar el String antes de enviarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ============================================================
// --- 3. Teclado y foco: un formulario que se recorre bien ---
// ============================================================
@Composable
fun DemoTecladoYFoco() {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf<String?>(null) }

    val focoNombre = remember { FocusRequester() }
    val gestorFoco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. Teclado y foco", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pulsa 'Siguiente' en el teclado: el foco salta solo. En el último campo " +
                        "la tecla dice 'Listo' y cierra el teclado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next        // la tecla dirá "Siguiente"
                ),
                keyboardActions = KeyboardActions(
                    onNext = { gestorFoco.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focoNombre)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,   // teclado con @ y .com
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { gestorFoco.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = edad,
                onValueChange = { edad = it.filter { c -> c.isDigit() } },
                label = { Text("Edad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done           // la tecla dirá "Listo"
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        gestorFoco.clearFocus()   // quita el foco
                        teclado?.hide()           // y cierra el teclado
                        enviado = "$nombre · $email · $edad años"
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // Dar el foco a un campo concreto (ej: tras un error de validación)
                    focoNombre.requestFocus()
                }) { Text("Foco al primero") }
                OutlinedButton(onClick = {
                    gestorFoco.clearFocus(); teclado?.hide()
                }) { Text("Cerrar teclado") }
            }

            enviado?.let {
                Text("Enviado: $it", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

// ============================================================
// --- 4. Otros ajustes de Text que se preguntan ---
// ============================================================
@Composable
fun DemoAjustesTexto() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("4. Recorte, líneas y escalado", style = MaterialTheme.typography.titleMedium)

            Text(
                "Este texto es muy largo y se corta con puntos suspensivos al llegar al " +
                        "límite de una sola línea porque usa maxLines con overflow Ellipsis",
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "Con minLines reservas altura desde el principio y la tarjeta no 'salta' " +
                        "cuando llega el contenido real.",
                minLines = 2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                "Texto que se auto-reduce para caber",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                // autoSize ajusta el tamaño para que quepa (evita el corte)
                modifier = Modifier.fillMaxWidth()
            )

            BloqueCodigo(
                "Lo que debes recordar",
                """
                Text(
                    texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,  // …
                    softWrap = true,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp
                    )
                )

                // Selección de texto por el usuario:
                SelectionContainer { Text("Este texto se puede copiar") }
                """.trimIndent()
            )
        }
    }
}

// ============================================================
// --- PANTALLA ---
// ============================================================
@Composable
fun PantallaTexto() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Texto y entrada avanzados", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Estilos mezclados, máscaras de entrada, teclados y gestión del foco: " +
                        "lo que necesita cualquier formulario real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item { DemoAnnotatedString() }
        item { DemoMascaras() }
        item { DemoTecladoYFoco() }
        item { DemoAjustesTexto() }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("📌 Chuleta de texto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BulletPoint("buildAnnotatedString + withStyle para estilos mezclados")
                    BulletPoint("El estado guarda el dato LIMPIO; la máscara es presentación")
                    BulletPoint("VisualTransformation exige OffsetMapping o el cursor salta")
                    BulletPoint("PasswordVisualTransformation ya viene hecha")
                    BulletPoint("KeyboardType: Email, Phone, Number, Password")
                    BulletPoint("ImeAction.Next + moveFocus = formulario que se recorre solo")
                    BulletPoint("clearFocus() + keyboardController.hide() al terminar")
                    BulletPoint("maxLines + TextOverflow.Ellipsis para no romper el layout")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nota de futuro: Compose está migrando a BasicTextField con " +
                                "TextFieldState (API con estado propio y transformaciones de " +
                                "entrada). Cuando sea estable, sustituirá al patrón " +
                                "value/onValueChange de arriba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
