package com.example.androidcompleto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GUÍA MAESTRA DE JETPACK COMPOSE
 * Aquí entenderás cómo se construye la interfaz en Android moderno.
 *
 * LOS 3 PILARES:
 * 1. Column / Row / Box  -> los contenedores (vertical, horizontal, superpuesto)
 * 2. Modifier            -> tamaño, color, clics, padding... EN ORDEN de cadena
 * 3. Lazy*               -> listas eficientes que solo dibujan lo visible
 */

// --- 1. CORE: @Composable ---
// Las funciones con esta anotación son los ladrillos de tu app.
// Reemplazan a los antiguos XML y a los Widgets de otros frameworks.
@Composable
fun MiComponente(texto: String) {
    Text(text = "Soy un componente: $texto")
}

// --- 2. LAYOUTS (Los Contenedores) ---
@Composable
fun EjemploLayouts() {
    // COLUMN: Apila elementos verticalmente
    Column {
        MiComponente("Uno")
        MiComponente("Dos")

        // ROW: Alinea elementos horizontalmente
        Row {
            Text("Izquierda")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Derecha")
        }

        // BOX: Superpone elementos (como un Stack). Útil para poner texto sobre imágenes.
        Box(modifier = Modifier.size(100.dp).background(Color.LightGray)) {
            Text("Fondo", modifier = Modifier.align(Alignment.Center))
            Text("Arriba", modifier = Modifier.align(Alignment.TopStart))
        }
    }
}

// --- 3. WEIGHT: repartir el espacio (el 'flex' de Compose) ---
@Composable
fun EjemploWeight() {
    // weight reparte el espacio PROPORCIONALMENTE: 1+2+1 = 4 partes
    Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF9A9A)),
            contentAlignment = Alignment.Center) { Text("1f") }
        Box(Modifier.weight(2f).fillMaxHeight().background(Color(0xFFA5D6A7)),
            contentAlignment = Alignment.Center) { Text("2f (el doble)") }
        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF90CAF9)),
            contentAlignment = Alignment.Center) { Text("1f") }
    }
}

// --- 4. ARRANGEMENT Y ALIGNMENT: distribuir y alinear ---
@Composable
fun EjemploArrangement() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SpaceBetween:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween // extremos separados
        ) { Text("A"); Text("B"); Text("C") }

        Text("Center:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(4.dp),
            horizontalArrangement = Arrangement.Center // todos al centro
        ) { Text("A  "); Text("B  "); Text("C") }
    }
}

// --- 5. LISTAS: LazyColumn, LazyRow y LazyVerticalGrid ---
// "Lazy" significa que solo dibuja lo que se ve en pantalla (muy eficiente).
@Composable
fun MiListaDeTareas(tareas: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "items" es como el builder: crea un elemento por cada dato en la lista
        items(tareas) { tarea ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = tarea,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// LazyRow: carrusel horizontal (categorías, stories, chips)
@Composable
fun EjemploLazyRow() {
    val categorias = listOf("Todos", "Kotlin", "Compose", "Estado", "Red", "Room", "Testing")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categorias) { categoria ->
            Text(
                categoria,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// LazyVerticalGrid: cuadrícula (galerías, catálogos de productos)
@Composable
fun EjemploGrid() {
    val colores = listOf(
        0xFFEF9A9A, 0xFFA5D6A7, 0xFF90CAF9, 0xFFFFF59D,
        0xFFCE93D8, 0xFFFFCC80, 0xFF80DEEA, 0xFFB0BEC5
    )
    // OJO: dentro de una pantalla con scroll propio, el grid necesita
    // altura FIJA (dos scrolls verticales anidados no se llevan bien).
    LazyVerticalGrid(
        columns = GridCells.Fixed(4), // también existe GridCells.Adaptive(100.dp)
        modifier = Modifier.height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colores) { color ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(color))
            )
        }
    }
}

// --- 6. MODIFIERS (El motor de diseño) ---
// EL ORDEN IMPORTA: cada modifier envuelve al anterior.
@Composable
fun EjemploModifiers() {
    Text(
        text = "¡Mírame!",
        modifier = Modifier
            .padding(16.dp)           // Margen externo (ANTES del fondo)
            .background(Color.Cyan)   // Color de fondo
            .clickable { /* Acción */ } // Detecta clics
            .fillMaxWidth()           // Ocupa todo el ancho
            .height(50.dp)            // Altura fija
            .padding(8.dp)            // Margen interno (DESPUÉS del fondo)
    )
}

@Composable
fun PantallaCompose() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Jetpack Compose", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Layouts (Column, Row, Box):", style = MaterialTheme.typography.titleMedium)
        EjemploLayouts()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Weight (repartir espacio):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        EjemploWeight()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Arrangement (distribución):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        EjemploArrangement()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("LazyRow (carrusel horizontal):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        EjemploLazyRow()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("LazyVerticalGrid (cuadrícula):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        EjemploGrid()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Modifiers (el orden importa):", style = MaterialTheme.typography.titleMedium)
        EjemploModifiers()
    }
}
