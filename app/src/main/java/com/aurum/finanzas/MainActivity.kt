package com.aurum.finanzas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aurum.finanzas.data.Prefs
import com.aurum.finanzas.data.Repo
import com.aurum.finanzas.notif.Notifier
import com.aurum.finanzas.ui.AppRoot
import com.aurum.finanzas.ui.SettingsState
import com.aurum.finanzas.ui.theme.AurumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(applicationContext)
        Notifier.ensureChannel(applicationContext)
        val settings = SettingsState(Prefs(applicationContext))
        setContent {
            AurumTheme(themeMode = settings.themeMode) {
                AppRoot(settings)
            }
        }
    }
}
