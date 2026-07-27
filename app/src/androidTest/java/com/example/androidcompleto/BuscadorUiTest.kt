package com.example.androidcompleto

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TESTS DE UI EN COMPOSE (Lección K5)
 *
 * Estos corren en un emulador o dispositivo real:
 *      ./gradlew connectedDebugAndroidTest
 *
 * CÓMO ENCUENTRA COMPOSE LOS ELEMENTOS: no hay findViewById. Se busca en
 * el árbol de SEMANTICS — el mismo que lee TalkBack. Por eso una app
 * accesible es una app fácil de testear (lección C8).
 *
 * LOS 3 PASOS DE TODO TEST DE UI:
 *   1. FINDER    → onNodeWithText / onNodeWithTag / onNodeWithContentDescription
 *   2. ACCIÓN    → performClick / performTextInput / performScrollTo
 *   3. ASERCIÓN  → assertIsDisplayed / assertTextEquals / assertIsEnabled
 */
@RunWith(AndroidJUnit4::class)
class BuscadorUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun al_escribir_muestra_los_resultados_que_coinciden() {
        compose.setContent { PantallaTesting() }

        // 1. FINDER + 2. ACCIÓN
        compose.onNodeWithTag("campo_busqueda").performTextInput("kot")

        // El ViewModel tiene 300ms de debounce: esperamos a que aparezca.
        // waitUntil es la forma correcta — nunca uses Thread.sleep.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("lista_resultados")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 3. ASERCIÓN
        compose.onNodeWithText("• Kotlin").assertIsDisplayed()
        compose.onNodeWithText("• Kotlin Multiplatform").assertIsDisplayed()
    }

    @Test
    fun una_busqueda_sin_coincidencias_muestra_el_mensaje_vacio() {
        compose.setContent { PantallaTesting() }

        compose.onNodeWithTag("campo_busqueda").performTextInput("xyz-no-existe")

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Sin resultados 🤷")
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Sin resultados 🤷").assertIsDisplayed()
    }

    @Test
    fun al_borrar_el_texto_vuelve_al_estado_inicial() {
        compose.setContent { PantallaTesting() }

        compose.onNodeWithTag("campo_busqueda").performTextInput("compose")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("lista_resultados")
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("campo_busqueda").performTextClearance()

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Escribe algo para buscar")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Escribe algo para buscar").assertIsDisplayed()
    }

    @Test
    fun la_pantalla_muestra_su_titulo_al_abrirse() {
        compose.setContent { PantallaTesting() }

        compose.onNodeWithText("Testing asíncrono").assertIsDisplayed()
    }
}
