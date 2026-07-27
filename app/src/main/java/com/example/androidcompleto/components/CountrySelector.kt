package com.example.androidcompleto.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Modelo de datos para el país
 */
data class Pais(
    val nombre: String,
    val bandera: String, // Emoji de la bandera
    val codigo: String
)

/**
 * Componente Selector de Países usando Material 3
 * Equivalente a un DropdownButtonFormField en Flutter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDePaises(
    onPaisSeleccionado: (Pais) -> Unit,
    modifier: Modifier = Modifier
) {
    // Lista de países extendida
    val listaPaises = listOf(
        Pais("Argentina", "🇦🇷", "+54"),
        Pais("Bolivia", "🇧🇴", "+591"),
        Pais("Brasil", "🇧🇷", "+55"),
        Pais("Chile", "🇨🇱", "+56"),
        Pais("Colombia", "🇨🇴", "+57"),
        Pais("Costa Rica", "🇨🇷", "+506"),
        Pais("Cuba", "🇨🇺", "+53"),
        Pais("Ecuador", "🇪🇨", "+593"),
        Pais("El Salvador", "🇸🇻", "+503"),
        Pais("España", "🇪🇸", "+34"),
        Pais("Estados Unidos", "🇺🇸", "+1"),
        Pais("Guatemala", "🇬🇹", "+502"),
        Pais("Honduras", "🇭🇳", "+504"),
        Pais("México", "🇲🇽", "+52"),
        Pais("Nicaragua", "🇳🇮", "+505"),
        Pais("Panamá", "🇵🇦", "+507"),
        Pais("Paraguay", "🇵🇾", "+595"),
        Pais("Perú", "🇵🇪", "+51"),
        Pais("Puerto Rico", "🇵🇷", "+1-787"),
        Pais("República Dominicana", "🇩🇴", "+1-809"),
        Pais("Uruguay", "🇺🇾", "+598"),
        Pais("Venezuela", "🇻🇪", "+58")
    ).sortedBy { it.nombre } // Los ordenamos alfabéticamente para que sea fácil buscarlos

    // ESTADO: ¿El menú está abierto?
    var expandido by remember { mutableStateOf(false) }
    
    // ESTADO: País seleccionado actualmente
    var paisSeleccionado by remember { mutableStateOf(listaPaises[0]) }

    // El contenedor oficial de Material 3 para menús desplegables
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido },
        modifier = modifier.fillMaxWidth().padding(16.dp)
    ) {
        // El campo de texto que sirve de "botón"
        OutlinedTextField(
            value = "${paisSeleccionado.bandera} ${paisSeleccionado.nombre} (${paisSeleccionado.codigo})",
            onValueChange = {},
            readOnly = true, // No queremos que el usuario escriba, solo que seleccione
            label = { Text("Selecciona tu país") },
            trailingIcon = {
                // El icono de la flecha que gira sola
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        // El menú que se despliega
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            listaPaises.forEach { pais ->
                DropdownMenuItem(
                    text = {
                        Text(text = "${pais.bandera} ${pais.nombre} (${pais.codigo})")
                    },
                    onClick = {
                        paisSeleccionado = pais
                        expandido = false
                        onPaisSeleccionado(pais) // Notificamos al padre (como un callback en Flutter)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectorDePaises() {
    SelectorDePaises(onPaisSeleccionado = {})
}
