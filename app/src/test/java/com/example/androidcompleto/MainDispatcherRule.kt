package com.example.androidcompleto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * LA REGLA QUE TODO TEST DE VIEWMODEL NECESITA
 *
 * Problema: viewModelScope usa Dispatchers.Main, que en un test JVM
 * NO EXISTE (no hay Looper de Android). Sin esto obtienes:
 *   "Module with the Main dispatcher had failed to initialize"
 *
 * Solución: sustituir Main por un TestDispatcher durante el test y
 * devolverlo a su sitio al terminar.
 *
 * Uso:  @get:Rule val mainDispatcherRule = MainDispatcherRule()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
