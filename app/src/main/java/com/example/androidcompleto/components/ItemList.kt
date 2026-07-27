package com.example.androidcompleto.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

data class Amigo(
    val nombre: String,
    val profesion: String,
    val avatarEmoji: String,
    val estaConectado: Boolean
)

@Composable
fun ItemListBasic(modifier: Modifier){

    val listaAmigos = listOf(
        Amigo("Carlos", "Desarrollador Android", "👨‍💻", true),
        Amigo("Lucía", "Diseñadora UI/UX", "🎨", false),
        Amigo("Marcos", "Ingeniero de Datos", "📊", true),
        Amigo("Sofía", "Project Manager", "📋", true),
        Amigo("Andrés", "Especialista en QA", "🔍", false),
        Amigo("Elena", "Backend Developer", "☁️", true),
        Amigo("Miguel", "Product Owner", "💼", false)
    )

    Column(modifier
        .background(Color.Cyan)
        .verticalScroll(rememberScrollState())) {

        listaAmigos.forEach {
            Column {
                Text(it.nombre)
                HorizontalDivider()
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun ItemListPreview(){
    ItemListBasic(modifier = Modifier)
}