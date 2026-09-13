package com.example.subscriptions.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.example.subscriptions.repository.SubscriptionRepository
import com.example.subscriptions.storage.SecurePrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class ConnectionState { data object OnSkylineNetwork:ConnectionState(); data object OffNetwork:ConnectionState(); data object AutoLoginSuccess:ConnectionState(); data class AutoLoginFailed(val reason:String):ConnectionState() }
class NetworkWatcher(private val context:Context, private val prefs:SecurePrefs, private val repository:SubscriptionRepository) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO); private val manager=context.getSystemService(ConnectivityManager::class.java); private var lastCheck=0L
    private val _state=MutableStateFlow<ConnectionState>(ConnectionState.OffNetwork); val state:StateFlow<ConnectionState> = _state
    private val callback=object:ConnectivityManager.NetworkCallback(){ override fun onAvailable(network:Network){ check() }; override fun onLost(network:Network){ _state.value=ConnectionState.OffNetwork } }
    fun start(){ manager.registerDefaultNetworkCallback(callback); check() }
    fun stop(){ runCatching{manager.unregisterNetworkCallback(callback)}; scope.cancel() }
    private fun check(){ if(System.currentTimeMillis()-lastCheck<60_000L)return; lastCheck=System.currentTimeMillis(); scope.launch { if(SkylineNetworkDetector.isSkylineNetwork() && SkylineNetworkDetector.confirmSkylineGateway()){ _state.value=ConnectionState.OnSkylineNetwork; if(prefs.username!=null){ repository.refreshOrRelogin().onSuccess{_state.value=ConnectionState.AutoLoginSuccess}.onFailure{_state.value=ConnectionState.AutoLoginFailed("تعذر تسجيل الدخول تلقائياً")} } } else _state.value=ConnectionState.OffNetwork } }
}
