package com.example.subscriptions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subscriptions.crypto.SasCrypto
import com.example.subscriptions.model.Subscription
import com.example.subscriptions.network.ApiFactory
import com.example.subscriptions.network.SasEncryptedRequest
import com.example.subscriptions.storage.SecurePrefs
import com.example.subscriptions.repository.SubscriptionRepository
import com.example.subscriptions.network.NetworkWatcher
import com.example.subscriptions.network.ConnectionState
import com.example.subscriptions.util.JsonUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 5)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1565C0))) { AppScreen() } }
    }
}

class AppVm : ViewModel() {
    var token by mutableStateOf<String?>(null); var loading by mutableStateOf(false); var error by mutableStateOf<String?>(null); var subscription by mutableStateOf<Subscription?>(null)
    private var prefs: SecurePrefs? = null
    var networkState by mutableStateOf<ConnectionState>(ConnectionState.OffNetwork)
    private var watcher: NetworkWatcher? = null
    fun init(storage: SecurePrefs, context: android.content.Context) { if (prefs != null) return; prefs = storage; token = storage.token; watcher = NetworkWatcher(context, storage, SubscriptionRepository(storage)); watcher?.start(); viewModelScope.launch { watcher?.state?.collect { networkState = it } }; if (token != null) load() }
    fun login(username: String, password: String) { loading = true; error = null; viewModelScope.launch { try { val json = Gson().toJson(mapOf("username" to username, "password" to password)); var response = ApiFactory.api.login(ApiFactory.encoded(json)); if (!response.isSuccessful) response = ApiFactory.api.login(ApiFactory.plain(json)); val received = response.body()?.token; if (!response.isSuccessful || received.isNullOrBlank()) throw Exception("اسم المستخدم أو كلمة المرور غير صحيحة"); token = received; prefs?.token = received; prefs?.username = username; prefs?.password = password; load() } catch (e: Exception) { error = if (e is java.io.IOException) "تعذر الاتصال بالخادم" else e.message ?: "حدث خطأ غير متوقع"; loading = false } } }
    private fun load() { viewModelScope.launch { try { val json = Gson().toJson(mapOf("username" to prefs?.username.orEmpty())); var response = ApiFactory.api.getUser("Bearer ${token.orEmpty()}", ApiFactory.encoded(json)); if (!response.isSuccessful) response = ApiFactory.api.getUser("Bearer ${token.orEmpty()}", ApiFactory.plain(json)); if (!response.isSuccessful) throw Exception(); val root = Gson().fromJson(Gson().toJson(response.body()), JsonObject::class.java); subscription = JsonUtils.subscription(root); prefs?.expiry = subscription?.expiryMillis ?: 0L; loading = false } catch (_: Exception) { error = "تعذر تحميل بيانات الاشتراك"; loading = false } } }
    fun refresh() { if (token != null) load() }
    fun logout() { watcher?.stop(); prefs?.clear(); token = null; subscription = null }
}

@Composable fun AppScreen(vm: AppVm = viewModel()) { val context = LocalContext.current; LaunchedEffect(Unit) { vm.init(SecurePrefs(context), context) }; if (vm.token == null) LoginScreen(vm) else DashboardScreen(vm) }

@Composable private fun LoginScreen(vm: AppVm) {
    var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AccessTime, null, tint = Color(0xFF1565C0), modifier = Modifier.size(72.dp)); Text("اشتراكاتي", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF1565C0)); Text("متابعة اشتراك Skyline", modifier = Modifier.padding(bottom = 28.dp))
        OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        Button({ vm.login(username, password) }, enabled = !vm.loading && username.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) { if (vm.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("دخول") }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable private fun DashboardScreen(vm: AppVm) {
    val data = vm.subscription
    val refreshing = vm.loading
    val pullState = rememberPullRefreshState(refreshing, { vm.refresh() })
    Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.End) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اشتراكاتي", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1565C0))
                IconButton({ vm.logout() }) { Icon(Icons.Default.Logout, "تسجيل الخروج") }
            }
            val online = vm.networkState is ConnectionState.OnSkylineNetwork || vm.networkState is ConnectionState.AutoLoginSuccess
            Text(if (online) "🟢 متصل بشبكة Skyline" else "⚪ غير متصل بالشبكة", color = if (online) Color(0xFF2E7D32) else Color.Gray)
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (data == null && !vm.loading) Text(vm.error ?: "لا توجد بيانات", modifier = Modifier.padding(32.dp))
            data?.let { item ->
                Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("مرحباً ${item.name}", style = MaterialTheme.typography.titleLarge)
                        Text(item.profile, color = Color(0xFF1565C0))
                        Spacer(Modifier.height(20.dp))
                        Detail("تاريخ البداية", item.start); Detail("تاريخ الانتهاء", item.expiry)
                        Detail("الوقت المستهلك", item.timeUsed); Detail("الوقت المتبقي", item.timeLeft)
                        Detail("الترافيك المستهلك", item.trafficUsed); Detail("الترافيك المتبقي", item.trafficLeft)
                        val difference = item.expiryMillis - System.currentTimeMillis()
                        val statusColor = when { difference < 0 -> Color(0xFFC62828); difference < 86_400_000L -> Color(0xFFEF6C00); else -> Color(0xFF2E7D32) }
                        val status = when { difference < 0 -> "منتهي"; difference < 86_400_000L -> "ينتهي قريباً"; else -> "نشط" }
                        val progress = if (item.expiryMillis > 0L) (difference.toFloat() / item.expiryMillis.toFloat()).coerceIn(0f, 1f) else 0f
                        CircularProgressIndicator(progress = { progress }, color = statusColor, modifier = Modifier.size(72.dp).padding(top = 12.dp))
                        Text(status, color = statusColor, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
            Button({ vm.refresh() }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("تحديث البيانات") }
        }
        PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(value); Text(label, color = Color.Gray) }
}
