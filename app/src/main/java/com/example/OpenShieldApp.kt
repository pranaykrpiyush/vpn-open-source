package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.VpnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OpenShieldApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var database: AppDatabase
        private set
    lateinit var repository: VpnRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = VpnRepository(database)

        applicationScope.launch {
            repository.initializeDefaultServersIfEmpty()
        }
    }

    companion object {
        lateinit var instance: OpenShieldApp
            private set
    }
}
