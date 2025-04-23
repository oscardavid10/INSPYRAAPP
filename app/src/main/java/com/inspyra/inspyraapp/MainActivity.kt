package com.inspyra.inspyraapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.inspyra.inspyraapp.BuildConfig
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            val action = intent?.getStringExtra("accion")
            SplashThenWeb(action)
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    @Composable
    fun SplashThenWeb(accion: String?) {
        var showSplash by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            delay(2000)
            showSplash = false
        }

        Box(Modifier.fillMaxSize()) {
            if (showSplash) {
                SplashScreen()
            } else {
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
            Image(painterResource(id = R.drawable.principal), contentDescription = "Logo")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebViewScreen(baseUrl: String, accion: String?) {
        val context = LocalContext.current
        val isError = remember { mutableStateOf(false) }
        val urlFinal = baseUrl + (accion ?: "")
        val swipeRefresh = remember { mutableStateOf<SwipeRefreshLayout?>(null) }

        // ✅ Mueve esto AQUÍ para que sí se componga
        checkForUpdateCompose(context)

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SwipeRefreshLayout(ctx).apply {
                    swipeRefresh.value = this
                    setOnRefreshListener {
                        isError.value = false
                        webView.reload()
                        isRefreshing = false
                    }

                    webView = WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                isError.value = true
                                loadOfflineHtml(this@apply)
                            }
                        }
                        addJavascriptInterface(WebAppInterface(ctx), "AndroidApp")
                        loadUrl(urlFinal)
                    }

                    addView(webView)
                }
            }
        )
    }


    private fun loadOfflineHtml(webView: WebView) {
        val html = """
            <html>
            <head>
                <style>
                    body {
                        background-color: #ffffff;
                        font-family: sans-serif;
                        text-align: center;
                        padding-top: 80px;
                    }
                    h1 {
                        color: #d32f2f;
                        font-size: 28px;
                    }
                    p {
                        color: #555;
                        font-size: 18px;
                    }
                    img {
                        width: 100px;
                        margin-bottom: 20px;
                    }
                </style>
            </head>
            <body>
                <img src="file:///android_res/drawable/principal.png" />
                <h1>¡Sin conexión!</h1>
                <p>No se pudo cargar la página.<br>Por favor, revisa tu conexión a Internet.</p>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    @Composable
    fun checkForUpdateCompose(context: Context) {
        Log.d("VERSION_CHECK", "Entró a checkForUpdateCompose()")
        val showDialog = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            try {
                val versionActual = BuildConfig.VERSION_NAME
                Log.d("VERSION_CHECK", "Versión actual (BuildConfig): $versionActual")

                val response = withContext(Dispatchers.IO) {
                    val url = URL("https://desarrollos.synology.me:8003/version/inspyra.json")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.inputStream.bufferedReader().readText()
                }

                Log.d("VERSION_CHECK", "JSON recibido: $response")

                val json = JSONObject(response)
                val ultima = json.getString("ultima_version")
                Log.d("VERSION_CHECK", "Versión última: $ultima")

                if (versionActual != ultima) {
                    Log.d("VERSION_CHECK", "¡Hay actualización disponible!")
                    showDialog.value = true
                } else {
                    Log.d("VERSION_CHECK", "App actualizada.")
                }

            } catch (e: Exception) {
                Log.e("VERSION_CHECK", "Error en la validación", e)
            }
        }

        if (showDialog.value) {
            AlertDialog.Builder(context)
                .setTitle("Actualización disponible")
                .setMessage("Hay una nueva versión disponible. Por favor, actualiza la app.")
                .setCancelable(false)
                .setPositiveButton("Actualizar") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                    context.startActivity(intent)

                    // 👇 Forzar cierre completo de la app
                    if (context is ComponentActivity) {
                        context.finishAffinity()
                    }
                }
                .show()
        }
    }


}
