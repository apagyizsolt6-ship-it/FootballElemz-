package com.footballai.analyzer

import android.app.Application
import com.footballai.analyzer.fcm.FcmRegistrar
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class FociElemzoApp : Application() {

    @Inject lateinit var fcmRegistrar: FcmRegistrar

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Hilt inject mezők: EntryPoint vagy késleltetett regisztráció a MainActivity-ből biztosabb.
        // Itt csak scope – regisztráció MainActivity-ben / Settings mentéskor.
    }
}
