package com.example.androidcompleto

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE FORMULARIOS Y VALIDACIÓN
 * El patrón central es "STATE HOISTING" (elevar el estado):
 * el TextField NO guarda su propio texto — tú se lo das (value) y él te avisa
 * de cambios (onValueChange). Así el estado vive en UN solo lugar y validar
 * es trivial: son solo propiedades derivadas del estado.
 */

@Composable
fun PantallaFormularios() {
    // --- ESTADO DEL FORMULARIO (única fuente de verdad) ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var aceptaTerminos by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf("Gratis") }
    var notificaciones by remember { mutableStateOf(true) }
    var edad by remember { mutableFloatStateOf(25f) }
    var enviado by remember { mutableStateOf(false) }

    // --- VALIDACIONES DERIVADAS (se recalculan solas al cambiar el estado) ---
    val emailValido = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordValida = password.length >= 8
    val formularioValido = emailValido && passwordValida && aceptaTerminos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Formulario de Registro", style = MaterialTheme.typography.headlineSmall)

        // --- 1. CAMPO DE EMAIL con validación en vivo ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it }, // el estado se actualiza en cada tecla
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            // isError pinta el campo de rojo. Solo validamos si ya escribió algo.
            isError = email.isNotEmpty() && !emailValido,
            supportingText = {
                if (email.isNotEmpty() && !emailValido) Text("Formato de correo inválido")
            },
            // El teclado correcto para cada campo mejora mucho la UX
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // --- 2. CAMPO DE CONTRASEÑA con ojo para mostrar/ocultar ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mín. 8 caracteres)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            // PasswordVisualTransformation convierte el texto en puntos ••••
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            isError = password.isNotEmpty() && !passwordValida,
            supportingText = {
                if (password.isNotEmpty() && !passwordValida)
                    Text("Faltan ${8 - password.length} caracteres")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // --- 3. RADIO BUTTONS: selección única ---
        Text("Elige tu plan:", style = MaterialTheme.typography.titleSmall)
        listOf("Gratis", "Pro", "Empresa").forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // selectable hace toda la fila clickeable (accesibilidad)
                    .selectable(selected = plan == opcion, onClick = { plan = opcion })
            ) {
                RadioButton(selected = plan == opcion, onClick = { plan = opcion })
                Text(opcion)
            }
        }

        // --- 4. SLIDER: valores numéricos en un rango ---
        Text("Edad: ${edad.toInt()} años", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = edad,
            onValueChange = { edad = it },
            valueRange = 18f..99f
        )

        // --- 5. SWITCH y CHECKBOX ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = notificaciones, onCheckedChange = { notificaciones = it })
            Spacer(Modifier.width(8.dp))
            Text("Recibir notificaciones")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = aceptaTerminos, onCheckedChange = { aceptaTerminos = it })
            Text("Acepto los términos y condiciones")
        }

        // --- 6. BOTÓN DE ENVÍO: solo habilitado si TODO es válido ---
        Button(
            onClick = { enviado = true },
            enabled = formularioValido, // la validación deshabilita el botón sola
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarme")
        }

        if (enviado) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✔ Registro exitoso", style = MaterialTheme.typography.titleMedium)
                    Text("Email: $email")
                    Text("Plan: $plan | Edad: ${edad.toInt()}")
                    Text("Notificaciones: ${if (notificaciones) "Sí" else "No"}")
                }
            }
        }
    }
}
