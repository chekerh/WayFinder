package tn.esprit.wayfinder.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

/**
 * WebView screen to handle Flouci payment
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlouciPaymentScreen(
    navController: NavController,
    paymentLink: String,
    onPaymentSuccess: (String) -> Unit,
    onPaymentFailed: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Paiement Flouci",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        },
        containerColor = Color(0xFFEAF2FF)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                
                                // Check for success/failure in URL
                                url?.let {
                                    when {
                                        it.contains("success", ignoreCase = true) || 
                                        it.contains("payment_success", ignoreCase = true) -> {
                                            // Extract payment ID from URL if possible
                                            val paymentId = extractPaymentIdFromUrl(it)
                                            onPaymentSuccess(paymentId ?: "")
                                        }
                                        it.contains("fail", ignoreCase = true) || 
                                        it.contains("payment_failed", ignoreCase = true) -> {
                                            onPaymentFailed()
                                        }
                                    }
                                }
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                            
                            override fun onReceivedError(
                                view: WebView,
                                request: android.webkit.WebResourceRequest,
                                error: android.webkit.WebResourceError
                            ) {
                                super.onReceivedError(view, request, error)
                                errorMessage = error.description.toString()
                                isLoading = false
                            }
                        }
                        
                        loadUrl(paymentLink)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Erreur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = error,
                            color = Color.Red
                        )
                        Button(
                            onClick = { 
                                errorMessage = null
                                navController.popBackStack()
                            }
                        ) {
                            Text("Retour")
                        }
                    }
                }
            }
        }
    }
}

private fun extractPaymentIdFromUrl(url: String): String? {
    // Try to extract payment ID from URL parameters
    return try {
        val uri = android.net.Uri.parse(url)
        uri.getQueryParameter("payment_id") 
            ?: uri.getQueryParameter("id")
            ?: uri.lastPathSegment
    } catch (e: Exception) {
        null
    }
}

