package com.inspyra.inspyraapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.pullrefresh.PullRefreshIndicator
import androidx.compose.material3.pullrefresh.pullRefresh
import androidx.compose.material3.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.inspyra.inspyraapp.BuildConfig
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            val action = intent?.getStringExtra("accion")
            SplashThenWeb(action)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SplashThenWeb(accion: String?) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut()
        ) {
            SplashScreen()
        }

        if (!showSplash) {
            WebViewScreen("https://desarrollos.synology.me:8003/", accion)
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(id = R.drawable.principal),
            contentDescription = "Logo"
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(baseUrl: String, accion: String?) {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(checkInternet(context)) }
    val refreshing = remember { mutableStateOf(false) }

    val refreshState = rememberPullRefreshState(
        refreshing = refreshing.value,
        onRefresh = {
            refreshing.value = true
            isConnected.value = checkInternet(context)
            refreshing.value = false
        }
    )

    if (!isConnected.value) {
        Text("Sin conexión a Internet", color = Color.Red)
        return
    }

    val urlFinal = baseUrl + (accion ?: "")

    Box(Modifier.fillMaxSize().pullRefresh(refreshState)) {
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    addJavascriptInterface(WebAppInterface(it), "AndroidApp")
                    loadUrl(urlFinal)
                }
            }
        )
        PullRefreshIndicator(refreshing.value, refreshState, Modifier.align(Alignment.TopCenter))
    }

    checkForUpdate(context)
}

fun checkInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun checkForUpdate(context: Context) {
    val versionActual = BuildConfig.VERSION_NAME
    Thread {
        try {
            val url = URL("https://desarrollos.synology.me/version/inspyra.json")
            val connection = url.openConnection() as HttpURLConnection
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val ultima = json.getString("ultima")

            if (versionActual != ultima) {
                (context as ComponentActivity).runOnUiThread {
                    AlertDialog.Builder(context)
                        .setTitle("Actualización disponible")
                        .setMessage("Hay una nueva versión disponible. Por favor, actualiza la app.")
                        .setCancelable(false)
                        .setPositiveButton("Actualizar") { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                            context.startActivity(intent)
                        }
                        .show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}
