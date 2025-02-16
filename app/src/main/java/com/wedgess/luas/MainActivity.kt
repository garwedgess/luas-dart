package com.wedgess.luas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wedgess.luas.presentation.navigation.BottomNavigationBar
import com.wedgess.luas.presentation.navigation.MainNavigationGraph
import com.wedgess.luas.ui.theme.LuasTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navHostController = rememberNavController()
            val backStackEntry = navHostController.currentBackStackEntryAsState()


            LuasTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomAppBar {
                            BottomNavigationBar(
                                selectedItemRoute = backStackEntry.value?.destination?.route,
                                onNavigateTo = { route ->
                                    if (route != backStackEntry.value?.destination) {
                                        navHostController.navigate(route)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    MainNavigationGraph(
                        navController = navHostController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
