package com.example.revisit.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.revisit.ui.navigation.NavRoutes
import com.example.revisit.ui.ContactViewModel

    @Composable
    fun AppNavigation(navController: NavHostController, viewModel: ContactViewModel) {
        NavHost(navController = navController, startDestination = NavRoutes.CONTACT_LIST) {
            composable(NavRoutes.CONTACT_LIST) {
                ContactScreen(
                    viewModel = viewModel,
                    onAddContact = {
                        navController.navigate(NavRoutes.ADD_EDIT_CONTACT)
                    },
                    onContactClick = { contactId ->
                        navController.navigate("contactDetail/$contactId")
                    },
                    onShowMapClick = { contactIds ->
                        contactIds.ifEmpty { "ALL" }
                        navController.navigate("itineraryMap/$contactIds")
                    }
                )
            }

            composable(
                route = "itineraryMap/{contactIds}",
                arguments = listOf(navArgument("contactIds") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val contactIdsString = backStackEntry.arguments?.getString("contactIds")
                ItineraryMapScreen(
                    navController = navController,
                    contactIdsString = contactIdsString,
                    viewModel = viewModel
                )
            }

            composable(NavRoutes.ADD_EDIT_CONTACT) {
                AddEditContactScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoutes.ADD_EDIT_CONTACT_WITH_ID,
                arguments = listOf(navArgument("contactId") { type = NavType.IntType })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getInt("contactId")
                AddEditContactScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    contactId = contactId
                )
            }

            composable(
                route = "contactDetail/{contactId}",
                arguments = listOf(
                    navArgument("contactId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getInt("contactId")
                if (contactId != null && contactId != -1) {
                    ContactDetailScreen(
                        navController = navController,
                        contactId = contactId,
                        viewModel = viewModel
                    )
                } else {
                    Text("Error: Invalid Contact ID")
                }
            }
        }
    }

