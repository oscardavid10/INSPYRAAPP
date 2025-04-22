package com.inspyra.inspyraapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class WebAppInterface(private val context: Context) {

    @android.webkit.JavascriptInterface
    fun registrarUsuario(usuario: String) {
        Log.d("WebViewLogin", "Usuario recibido: $usuario")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("WebViewLogin", "Token FCM para $usuario: $token")

                // ✅ Enviar al servidor
                val url = "https://desarrollos.synology.me:8003/funciones/guardar_token.php" // Cámbialo a tu URL real
                val queue = Volley.newRequestQueue(context)

                val request = object : StringRequest(Method.POST, url,
                    { response ->
                        Log.d("WebViewLogin", "Token guardado en servidor: $response")
                    },
                    { error ->
                        Log.e("WebViewLogin", "Error al guardar token: ${error.message}")
                    }) {

                    override fun getParams(): Map<String, String> {
                        return mapOf("usuario" to usuario, "token" to token)
                    }
                }

                queue.add(request)

                Toast.makeText(context, "Bienvenido $usuario", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("WebViewLogin", "Error al obtener token")
            }
        }
    }
}
