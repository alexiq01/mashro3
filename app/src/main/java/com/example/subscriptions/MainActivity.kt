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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Logout
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
    var networkState by mutableStateOf<ConnectionState>(ConnectionState.OffNetwork); var screen by mutableStateOf("home"); private var watcher: NetworkWatcher? = null
    fun init(storage: SecurePrefs, context: android.content.Context) { if (prefs != null) return; prefs=storage; repository=SubscriptionRepository(storage); token=storage.token; watcher=NetworkWatcher(context,storage,repository!!); watcher?.start(); viewModelScope.launch { watcher?.state?.collect { networkState=it } }; if(token!=null) load() }
    fun login(username:String,password:String) { loading=true; error=null; viewModelScope.launch { repository?.login(username,password)?.fold({ token=it; load() },{ error=if(it is java.io.IOException) "تعذر الاتصال بالخادم" else it.message ?: "اسم المستخدم أو كلمة المرور غير صحيحة"; loading=false }) } }
    private fun load() { loading=true; viewModelScope.launch { repository?.refreshOrRelogin()?.fold({ subscription=it; token=prefs?.token; loading=false },{ error="تعذر تحميل بيانات الاشتراك"; loading=false }) } }
    fun refresh(){if(token!=null)load()}; fun navigate(value:String){screen=value}; fun logout(){watcher?.stop();prefs?.clear();token=null;subscription=null;screen="home"}
}
@Composable fun AppScreen(vm:AppVm=viewModel()){val c=LocalContext.current;LaunchedEffect(Unit){vm.init(SecurePrefs(c),c)};if(vm.token==null)LoginScreen(vm)else MainShell(vm)}
@Composable private fun LoginScreen(vm:AppVm){var u by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF062B63),Color(0xFF0A78D1))))){Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Image(painterResource(com.example.subscriptions.R.drawable.skyline_logo),"شعار Skyline",modifier=Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)));Text("اشتراكاتي",style=MaterialTheme.typography.headlineLarge,color=Color.White);Text("S K Y L I N E",color=Color(0xFF8DD7FF));Text("إدارة اشتراكك بكل سهولة",color=Color.White,modifier=Modifier.padding(top=8.dp,bottom=28.dp));OutlinedTextField(u,{u=it},label={Text("اسم المستخدم أو رقم الهاتف")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(p,{p=it},label={Text("كلمة المرور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth().padding(top=12.dp));vm.error?.let{Text(it,color=Color.White,modifier=Modifier.padding(10.dp))};Button({vm.login(u,p)},enabled=!vm.loading&&u.isNotBlank()&&p.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF0758A5))){if(vm.loading)CircularProgressIndicator(Modifier.size(20.dp))else Text("تسجيل الدخول")}}}}
@OptIn(ExperimentalMaterialApi::class)
@Composable private fun MainShell(vm:AppVm){val refresh=rememberPullRefreshState(vm.loading,{vm.refresh()});Scaffold(bottomBar={BottomNav(vm)}){pad->Box(Modifier.fillMaxSize().padding(pad).pullRefresh(refresh)){when(vm.screen){"customer"->CustomerScreen(vm);"services"->ServicesScreen(vm);"alerts"->AlertsScreen(vm);"settings"->SettingsScreen(vm);else->HomeScreen(vm)};if(vm.screen=="home")PullRefreshIndicator(vm.loading,refresh,Modifier.align(Alignment.TopCenter))}}}
@Composable private fun BottomNav(vm:AppVm){NavigationBar(containerColor=Color.White){NavigationBarItem(vm.screen=="home",{vm.navigate("home")},{Icon(Icons.Default.Home,null);Text("الرئيسية")});NavigationBarItem(vm.screen=="services",{vm.navigate("services")},{Icon(Icons.Default.Build,null);Text("الخدمات")});NavigationBarItem(vm.screen=="alerts",{vm.navigate("alerts")},{Icon(Icons.Default.Notifications,null);Text("الإشعارات")});NavigationBarItem(vm.screen=="settings",{vm.navigate("settings")},{Icon(Icons.Default.Settings,null);Text("المزيد")})}}
@Composable private fun TopBar(title:String,vm:AppVm){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){IconButton({vm.navigate("home")}){Icon(Icons.Default.ArrowBack,"رجوع")};Text(title,style=MaterialTheme.typography.titleLarge,color=Color(0xFF073B7A),modifier=Modifier.weight(1f));Icon(Icons.Default.Notifications,"الإشعارات",tint=Color(0xFF073B7A))}}
@Composable private fun HomeScreen(vm:AppVm){val i=vm.subscription;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End){Header(vm);i?.let{HomeCards(it,vm)};if(i==null&&!vm.loading)Text(vm.error?:"لا توجد بيانات",modifier=Modifier.padding(32.dp));Button({vm.refresh()},modifier=Modifier.fillMaxWidth().padding(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0875D1))){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("تحديث البيانات")}}}
@Composable private fun Header(vm:AppVm){val on=vm.networkState is ConnectionState.OnSkylineNetwork||vm.networkState is ConnectionState.AutoLoginSuccess;Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart=28.dp,bottomEnd=28.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF062B63),Color(0xFF0875D1)))).padding(18.dp)){Column(horizontalAlignment=Alignment.End,modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){IconButton({vm.navigate("settings")}){Icon(Icons.Default.Menu,"القائمة",tint=Color.White)};Column(horizontalAlignment=Alignment.End){Text("اشتراكاتي",color=Color.White,style=MaterialTheme.typography.titleLarge);Text("S K Y L I N E",color=Color(0xFF9BDFFF),style=MaterialTheme.typography.labelSmall)};IconButton({vm.navigate("alerts")}){Icon(Icons.Default.Notifications,"الإشعارات",tint=Color.White)}};Spacer(Modifier.height(16.dp));Surface(color=Color(0xFFE0F8E8),shape=RoundedCornerShape(20.dp)){Text(if(on)"  Wi‑Fi  متصل بشبكة Skyline  " else "  غير متصل بالشبكة  ",color=Color(0xFF147A42),modifier=Modifier.padding(7.dp))}}}}
@Composable private fun HomeCards(i:Subscription,vm:AppVm){Column(Modifier.padding(horizontal=16.dp),horizontalAlignment=Alignment.End){Text("مرحباً، ${i.name}",style=MaterialTheme.typography.titleLarge,modifier=Modifier.fillMaxWidth().padding(top=18.dp));Text("اضغط لعرض معلومات العميل",color=Color.Gray,modifier=Modifier.fillMaxWidth().clickable{vm.navigate("customer")});Card(Modifier.fillMaxWidth().padding(top=12.dp),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("الباقة الحالية",color=Color.Gray);Text(i.profile,style=MaterialTheme.typography.titleLarge,color=Color(0xFF0758A5));Text("🟢 ${i.status}")};CircularProgressIndicator(progress={((i.expiryMillis-System.currentTimeMillis()).toFloat()/i.expiryMillis.coerceAtLeast(1L)).coerceIn(0f,1f)},color=Color(0xFF20C77A),modifier=Modifier.size(76.dp))};Spacer(Modifier.height(12.dp));Detail("تاريخ البداية",i.start);Detail("تاريخ الانتهاء",i.expiry)}};Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){InfoBox("الرصيد",i.balance);InfoBox("الديون",i.debt)};Card(Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text("معلومات الخدمة",style=MaterialTheme.typography.titleMedium);Detail("السعر",i.price);Detail("الحالة",i.status);Detail("التجديد التلقائي",i.autoRenew)}}}}
@Composable private fun CustomerScreen(vm:AppVm){val i=vm.subscription;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End){TopBar("معلومات العميل",vm);Card(Modifier.fillMaxWidth().padding(16.dp),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(20.dp)){Detail("الاسم",i?.name?:"غير متوفر");Detail("اسم المستخدم","");Detail("الرصيد",i?.balance?:"غير متوفر");Detail("الديون",i?.debt?:"غير متوفر");Detail("البريد الإلكتروني","غير متوفر");Detail("رقم الهاتف","غير متوفر");Detail("العنوان","غير متوفر")}}}}
@Composable private fun ServicesScreen(vm:AppVm){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End){TopBar("الخدمات",vm);ServiceCard("Redeem Code","تفعيل كود الشحن",Color(0xFF8E4DE8));ServiceCard("Activate Account","تفعيل الحساب",Color(0xFF13A875));ServiceCard("Extend Service","تمديد الاشتراك",Color(0xFF148DDB));ServiceCard("Change Service","تغيير الباقة",Color(0xFFF2A21A))}}
@Composable private fun ServiceCard(title:String,sub:String,color:Color){Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Build,null,tint=color,modifier=Modifier.size(30.dp));Column(Modifier.padding(start=14.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(sub,color=Color.Gray)}}}}
@Composable private fun AlertsScreen(vm:AppVm){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End){TopBar("الإشعارات",vm);AlertCard("اشتراكك ينتهي غداً","سيتم تنبيهك قبل انتهاء الاشتراك بـ 24 ساعة",Color(0xFFFFA726));AlertCard("اشتراكك ينتهي بعد ساعة","افتح التطبيق للتجديد",Color(0xFFE53935));AlertCard("تم تحديث بيانات الحساب","تمت مزامنة بيانات اشتراكك بنجاح",Color(0xFF20B26B))}}
@Composable private fun AlertCard(title:String,text:String,color:Color){Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Notifications,null,tint=color,modifier=Modifier.size(30.dp));Column(Modifier.padding(start=14.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(text,color=Color.Gray)}}}}
@Composable private fun SettingsScreen(vm:AppVm){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.End){TopBar("الإعدادات",vm);SettingRow("المظهر","الوضع الفاتح",Icons.Default.Settings);SettingRow("اللغة","العربية",Icons.Default.Info);SettingRow("الإشعارات","تنبيهات انتهاء الاشتراك",Icons.Default.Notifications);SettingRow("التحديث التلقائي","مفعّل",Icons.Default.Refresh);Button({vm.logout()},modifier=Modifier.fillMaxWidth().padding(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE53935))){Icon(Icons.Default.Logout,null);Spacer(Modifier.width(8.dp));Text("تسجيل الخروج")}}}
@Composable private fun SettingRow(title:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector){Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=5.dp),shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Color(0xFF0875D1));Column(Modifier.padding(start=14.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(value,color=Color.Gray)}}}}
@Composable private fun InfoBox(label:String,value:String){Card(Modifier.width(170.dp),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text(label,color=Color.Gray);Text(value,color=Color(0xFF0758A5),style=MaterialTheme.typography.titleMedium)}}}
@Composable private fun Detail(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(value);Text(label,color=Color.Gray)}}
