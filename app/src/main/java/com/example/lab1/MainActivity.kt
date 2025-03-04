package com.example.lab1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.lab1.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Определяем верхний уровень навигации (все фрагменты на одном уровне)
        val topLevelDestinations = setOf(
            R.id.mainFragment,
            R.id.musicFragment,
            R.id.calendarFragment,
            R.id.instagramFragment
        )

        appBarConfiguration = AppBarConfiguration(topLevelDestinations)

        // Настраиваем ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Настраиваем нижнюю навигацию
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemSelectedListener { item ->
                // Обрабатываем выбор пункта меню
                when (item.itemId) {
                    R.id.mainFragment,
                    R.id.musicFragment,
                    R.id.calendarFragment,
                    R.id.instagramFragment -> {
                        navController.navigate(item.itemId)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
} 