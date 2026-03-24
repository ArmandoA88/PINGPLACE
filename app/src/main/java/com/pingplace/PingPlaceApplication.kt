package com.pingplace

import android.app.Application
import com.pingplace.BuildConfig
import androidx.work.Configuration
import com.pingplace.data.AppContainer
import org.osmdroid.config.Configuration as OsmConfiguration
import java.io.File

class PingPlaceApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        configureMaps()
        container = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun configureMaps() {
        val osmBasePath = File(cacheDir, "osmdroid").apply { mkdirs() }
        val osmTileCache = File(osmBasePath, "tiles").apply { mkdirs() }
        OsmConfiguration.getInstance().apply {
            load(this@PingPlaceApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            osmdroidBasePath = osmBasePath
            osmdroidTileCache = osmTileCache
        }
    }
}
