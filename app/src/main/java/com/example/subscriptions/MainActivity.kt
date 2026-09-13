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
    private var prefs: SecurePrefs? = null; private var repository: SubscriptionRepository? = null
    var networkState by mutableStateOf<ConnectionState>(ConnectionState.OffNetwork); private var watcher: NetworkWatcher? = null
    fun init(storage: SecurePrefs, context: android.content.Context) { if (prefs != null) return; prefs=storage; repository=SubscriptionRepository(storage); token=storage.token; watcher=NetworkWatcher(context,storage,repository!!); watcher?.start(); viewModelScope.launch { watcher?.state?.collect { networkState=it } }; if(token!=null) load() }
    fun login(username:String,password:String) { loading=true; error=null; viewModelScope.launch { repository?.login(username,password)?.fold({ token=it; load() },{ error=if(it is java.io.IOException) "تعذر الاتصال بالخادم" else it.message ?: "اسم المستخدم أو كلمة المرور غير صحيحة"; loading=false }) } }
    private fun load() { loading=true; viewModelScope.launch { repository?.refreshOrRelogin()?.fold({ subscription=it; token=prefs?.token; loading=false },{ error="تعذر تحميل بيانات الاشتراك"; loading=false }) } }
    fun refresh(){if(token!=null)load()}; fun logout(){watcher?.stop();prefs?.clear();token=null;subscription=null}
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
    val data=vm.subscription; val refreshing=vm.loading; val pullState=rememberPullRefreshState(refreshing,{vm.refresh()})
    Box(Modifier.fillMaxSize().pullRefresh(pullState)) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.End) {
        Text("اشتراكاتي",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF1565C0),modifier=Modifier.fillMaxWidth()); Text("Skyline Internet",color=Color.Gray,modifier=Modifier.fillMaxWidth())
        val online=vm.networkState is ConnectionState.OnSkylineNetwork || vm.networkState is ConnectionState.AutoLoginSuccess
        Text(if(online) "🟢 ONLINE" else "⚪ غير متصل",color=if(online)Color(0xFF2E7D32) else Color.Gray,modifier=Modifier.fillMaxWidth().padding(top=14.dp))
        if(vm.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(data==null&&!vm.loading)Text(vm.error?:"لا توجد بيانات",modifier=Modifier.padding(32.dp))
        data?.let { item ->
            Text("مرحباً بك",style=MaterialTheme.typography.titleLarge,modifier=Modifier.fillMaxWidth().padding(top=24.dp))
            Card(Modifier.fillMaxWidth().padding(top=12.dp)){Column(Modifier.padding(18.dp)){Text("الاشتراك",style=MaterialTheme.typography.titleMedium); Text(item.days+" يوم متبقي",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(vertical=12.dp)); Text(item.profile,style=MaterialTheme.typography.titleMedium,color=Color(0xFF1565C0)); Text("🟢 "+item.status); Detail("ينتهي",item.expiry)}}
            Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("الرصيد",color=Color.Gray);Text(item.balance)};Column{Text("الديون",color=Color.Gray);Text(item.debt)}}
            Card(Modifier.fillMaxWidth().padding(top=16.dp)){Column(Modifier.padding(18.dp)){Text("معلومات الخدمة",style=MaterialTheme.typography.titleMedium);Detail("السعر",item.price);Detail("الحالة",if(online)"ONLINE" else item.status);Detail("التجديد التلقائي",item.autoRenew)}}
        }
        Button({vm.refresh()},modifier=Modifier.fillMaxWidth().padding(top=20.dp)){Text("🔄 تحديث البيانات")}
    }; PullRefreshIndicator(refreshing,pullState,Modifier.align(Alignment.TopCenter)) }
}
@Composable private fun Detail(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(value);Text(label,color=Color.Gray)}}
