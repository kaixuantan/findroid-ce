package dev.jdtech.jellyfin.work

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.WorkManager
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.NetworkConnectivity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class OfflineToOnlineSyncObserver
@Inject
constructor(
    private val application: Application,
    private val appPreferences: AppPreferences,
    private val networkConnectivity: NetworkConnectivity,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope): Job {
        val onlineFlow =
            callbackFlow {
                trySend(networkConnectivity.isOnline())
                val manager = application.getSystemService(ConnectivityManager::class.java)
                if (manager == null) {
                    close()
                    return@callbackFlow
                }
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(networkConnectivity.isOnline())
                        }

                        override fun onLost(network: Network) {
                            trySend(networkConnectivity.isOnline())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: android.net.NetworkCapabilities,
                        ) {
                            trySend(networkConnectivity.isOnline())
                        }
                    }
                manager.registerDefaultNetworkCallback(callback)
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }

        return scope.launch {
            combine(
                appPreferences.observe(appPreferences.offlineMode),
                onlineFlow,
            ) { prefOffline, online -> prefOffline || !online }
                .debounce(2000.milliseconds)
                .collect { effectiveOffline ->
                    val previous = lastEffectiveOffline
                    lastEffectiveOffline = effectiveOffline
                    if (previous != null &&
                        UserDataSync.shouldSyncOnTransition(previous, effectiveOffline)
                    ) {
                        UserDataSync.schedule(WorkManager.getInstance(application))
                    }
                }
        }
    }

    private var lastEffectiveOffline: Boolean? = null
}
