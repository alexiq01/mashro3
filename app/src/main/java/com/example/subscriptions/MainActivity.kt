package com.example.subscriptions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
@Composable fun AppScreen(vm: AppVm = viewModel()) { val context=LocalContext.current; LaunchedEffect(Unit){vm.init(SecurePrefs(context),context)}; if(vm.token==null) LoginScreen(vm) else DashboardScreen(vm) }
@Composable private fun LoginScreen(vm:AppVm){ var username by remember{mutableStateOf("")}; var password by remember{mutableStateOf("")}; Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF062B63),Color(0xFF0A78D1))))){Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Image(painterResource(com.example.subscriptions.R.drawable.skyline_logo),"شعار Skyline",modifier=Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)));Text("اشتراكاتي",style=MaterialTheme.typography.headlineLarge,color=Color.White);Text("S K Y L I N E",color=Color(0xFF8DD7FF));Spacer(Modifier.height(36.dp));OutlinedTextField(username,{username=it},label={Text("اسم المستخدم")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth().padding(top=12.dp));vm.error?.let{Text(it,color=Color.White,modifier=Modifier.padding(10.dp))};Button({vm.login(username,password)},enabled=!vm.loading&&username.isNotBlank()&&password.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF0758A5))){if(vm.loading)CircularProgressIndicator(Modifier.size(20.dp))else Text("دخول")}}}}
@OptIn(ExperimentalMaterialApi::class)
@Composable private fun DashboardScreen(vm:AppVm){
 val item=vm.subscription; val refresh=rememberPullRefreshState(vm.loading,{vm.refresh()})
 Scaffold(bottomBar={NavigationBar(containerColor=Color.White){NavigationBarItem(true,{},{Icon(Icons.Default.Home,null);Text("الرئيسية")});NavigationBarItem(false,{},{Icon(Icons.Default.Menu,null);Text("الخدمات")});NavigationBarItem(false,{},{Icon(Icons.Default.Notifications,null);Text("الإشعارات")})}}){pad->
  Box(Modifier.fillMaxSize().padding(pad).pullRefresh(refresh)){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalAlignment=Alignment.End){Header(vm);item?.let{ModernCards(it)};if(item==null&&!vm.loading)Text(vm.error?:"لا توجد بيانات",modifier=Modifier.padding(32.dp));Button({vm.refresh()},modifier=Modifier.fillMaxWidth().padding(vertical=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0875D1))){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("تحديث البيانات")}};PullRefreshIndicator(vm.loading,refresh,Modifier.align(Alignment.TopCenter))}
 }
}
@Composable private fun Header(vm:AppVm){val online=vm.networkState is ConnectionState.OnSkylineNetwork||vm.networkState is ConnectionState.AutoLoginSuccess;Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart=28.dp,bottomEnd=28.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF062B63),Color(0xFF0875D1)))).padding(18.dp)){Column(horizontalAlignment=Alignment.End,modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Icon(Icons.Default.Menu,"القائمة",tint=Color.White);Column(horizontalAlignment=Alignment.End){Text("اشتراكاتي",color=Color.White,style=MaterialTheme.typography.titleLarge);Text("S K Y L I N E",color=Color(0xFF9BDFFF),style=MaterialTheme.typography.labelSmall)};Icon(Icons.Default.Notifications,"الإشعارات",tint=Color.White)};Spacer(Modifier.height(16.dp));Surface(color=Color(0xFFE0F8E8),shape=RoundedCornerShape(20.dp)){Text(if(online)"  Wi‑Fi  متصل بشبكة Skyline  " else "  غير متصل بالشبكة  ",color=Color(0xFF147A42),modifier=Modifier.padding(7.dp))}}}}
@Composable private fun ModernCards(i:Subscription){Text("مرحباً، ${i.name}",style=MaterialTheme.typography.titleLarge,modifier=Modifier.fillMaxWidth().padding(top=18.dp));Card(Modifier.fillMaxWidth().padding(top=12.dp),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("الباقة الحالية",color=Color.Gray);Text(i.profile,style=MaterialTheme.typography.titleLarge,color=Color(0xFF0758A5));Text("🟢 ${i.status}")};CircularProgressIndicator(progress={((i.expiryMillis-System.currentTimeMillis()).toFloat()/i.expiryMillis.coerceAtLeast(1L)).coerceIn(0f,1f)},color=Color(0xFF20C77A),modifier=Modifier.size(76.dp))};Spacer(Modifier.height(12.dp));Detail("تاريخ البداية",i.start);Detail("تاريخ الانتهاء",i.expiry)}};Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){InfoBox("الرصيد",i.balance);InfoBox("الديون",i.debt)};Card(Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text("معلومات الخدمة",style=MaterialTheme.typography.titleMedium);Detail("السعر",i.price);Detail("الحالة",i.status);Detail("التجديد التلقائي",i.autoRenew)}}}
@Composable private fun InfoBox(label:String,value:String){Card(Modifier.width(170.dp),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text(label,color=Color.Gray);Text(value,color=Color(0xFF0758A5),style=MaterialTheme.typography.titleMedium)}}}
@Composable private fun Detail(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(value);Text(label,color=Color.Gray)}}
