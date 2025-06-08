package com.example.revisit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel // Este import es correcto
import androidx.navigation.compose.rememberNavController
import com.example.revisit.RevisitApplication
import com.example.revisit.ui.theme.RevisitTheme
import com.example.revisit.ui.screens.AppNavigation
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            RevisitTheme(
                darkTheme = isSystemInDarkTheme()
            ) {

                val app = application as RevisitApplication

                val contactViewModel: ContactViewModel = viewModel(
                    factory = ContactViewModel.Companion.ContactViewModelFactory(
                        applicationContext = app, // <-- Pasando el ApplicationContext
                        repository = app.repository
                    )
                )

                val navController = rememberNavController()
                AppNavigation(navController = navController, viewModel = contactViewModel)
            }
        }
    }
}