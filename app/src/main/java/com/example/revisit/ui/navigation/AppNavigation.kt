package com.example.revisit.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.revisit.ui.contacts.ContactViewModel
import com.example.revisit.ui.contacts.AddEditContactScreen
import com.example.revisit.ui.contacts.ContactDetailScreen
import com.example.revisit.ui.contacts.ContactScreen
import com.example.revisit.ui.itinerary.ItineraryMapScreen
import com.example.revisit.ui.map.MapPickerScreen
import com.example.revisit.ui.photo.PhotoViewScreen // Asegúrate de importar tu nueva pantalla

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
                onShowMapClick = { contactIdsString ->
                    val routeParameter: String = if (contactIdsString.isEmpty()) {
                        "ALL"
                    } else {
                        contactIdsString
                    }
                    navController.navigate("itineraryMap/$routeParameter")
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
                navController = navController,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ADD_EDIT_CONTACT_WITH_ID,
            arguments = listOf(navArgument("contactId") {
                type = NavType.IntType
            })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getInt("contactId")
            AddEditContactScreen(
                navController = navController,
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
                Text("Error: Invalid Contact ID passed to ContactDetail.")
            }
        }

        composable(
            route = "mapPicker?initialLat={initialLat}&initialLng={initialLng}",
            arguments = listOf(
                navArgument("initialLat") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("initialLng") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val initialLatStr = backStackEntry.arguments?.getString("initialLat")
            val initialLngStr = backStackEntry.arguments?.getString("initialLng")

            MapPickerScreen(
                initialLatitude = initialLatStr?.toDoubleOrNull(),
                initialLongitude = initialLngStr?.toDoubleOrNull(),
                onLocationSelected = { lat, lng ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("picked_latitude", lat)
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("picked_longitude", lng)
                    navController.popBackStack()
                },
                onNavigateUp = {
                    navController.popBackStack()
                }
            )
        }

        // --- INICIO: Nueva ruta para PhotoViewScreen ---
        composable(
            route = "photoViewScreen/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            PhotoViewScreen(
                navController = navController,
                imageUriString = imageUri
            )
        }
        // --- FIN: Nueva ruta para PhotoViewScreen ---
    }
}
