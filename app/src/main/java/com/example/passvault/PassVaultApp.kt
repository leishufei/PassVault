package com.example.passvault

import android.app.Application
import com.example.passvault.data.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PassVaultApp : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeDefaultCategories()
    }

    private fun initializeDefaultCategories() {
        applicationScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
    }
}
