package com.water1014.miowatery

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import java.io.ByteArrayInputStream

/**
 * Hosts the original MioWatery frontend without recreating its UI in Compose.
 *
 * The fixed base URL is intentional: localStorage and IndexedDB stay under the
 * same web origin across app upgrades. Never clear WebView data during startup.
 */
class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermission: PermissionRequest? = null
    private var pendingGeo: Pair<String, GeolocationPermissions.Callback>? = null
    private var pendingDownloadBytes: ByteArray? = null
    private var pendingDownloadMime: String = "application/octet-stream"
    private var pendingDownloadName: String = "Mio-export"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.geolocationEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            CookieManager.getInstance().setAcceptCookie(true)
            addJavascriptInterface(MioAndroidBridge(), "MioAndroid")
            webViewClient = MioWebViewClient()
            webChromeClient = MioWebChromeClient()
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                startDownload(url, userAgent, contentDisposition, mimeType)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        val html = assets.open("index.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val origin = "https://water1014.github.io/MioWatery/"
        webView.loadDataWithBaseURL(origin, html, "text/html", "UTF-8", origin + "index.html")
        setContentView(webView)
    }

    private fun isAppUrl(uri: Uri): Boolean =
        uri.scheme == "https" &&
            uri.host == "water1014.github.io" &&
            (uri.path == "/MioWatery" || uri.path?.startsWith("/MioWatery/") == true)

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "没有可打开此链接的应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun assetResponse(path: String): WebResourceResponse? {
        val asset = when (path) {
            "/MioWatery/", "/MioWatery/index.html" -> Triple("index.html", "text/html", "UTF-8")
            "/MioWatery/icon-192.png" -> Triple("icon-192.png", "image/png", null)
            "/MioWatery/manifest.json" -> Triple("manifest.json", "application/manifest+json", "UTF-8")
            "/MioWatery/sw.js" -> Triple("sw.js", "application/javascript", "UTF-8")
            else -> null
        } ?: return null

        return try {
            WebResourceResponse(asset.second, asset.third, assets.open(asset.first))
        } catch (_: Exception) {
            null
        }
    }

    private inner class MioWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
            val uri = request.url
            if (uri.host == "water1014.github.io") {
                assetResponse(uri.path ?: "")?.let { return it }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (isAppUrl(uri)) return false
            if (uri.scheme == "http" || uri.scheme == "https" ||
                uri.scheme == "mailto" || uri.scheme == "tel") {
                openExternal(uri)
                return true
            }
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.evaluateJavascript(
                """
                (function () {
                  if (window.__mioNativeDownloadHook) return;
                  window.__mioNativeDownloadHook = true;
                  document.addEventListener('click', function (event) {
                    var link = event.target && event.target.closest ? event.target.closest('a[download]') : null;
                    if (!link || !link.href || !/^(blob:|data:)/i.test(link.href)) return;
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    fetch(link.href).then(function (response) { return response.blob(); }).then(function (blob) {
                      var reader = new FileReader();
                      reader.onloadend = function () {
                        var encoded = String(reader.result || '').split(',')[1] || '';
                        window.MioAndroid.saveFile(link.download || 'Mio-export', blob.type || 'application/octet-stream', encoded);
                      };
                      reader.readAsDataURL(blob);
                    }).catch(function () {});
                  }, true);
                })();
                """.trimIndent(),
                null
            )
        }
    }

    private inner class MioWebChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            this@MainActivity.fileChooserCallback?.onReceiveValue(null)
            this@MainActivity.fileChooserCallback = filePathCallback
            val accepted = fileChooserParams?.acceptTypes
                ?.flatMap { it.split(",") }
                ?.map { it.trim() }
                ?.filter { it.contains("/") }
                ?.distinct()
                .orEmpty()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (accepted.size == 1) accepted.first() else "*/*"
                if (accepted.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, accepted.toTypedArray())
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
            }
            return try {
                startActivityForResult(Intent.createChooser(intent, "选择文件"), REQUEST_FILE)
                true
            } catch (_: Exception) {
                this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                this@MainActivity.fileChooserCallback = null
                false
            }
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            val required = request.resources.mapNotNull { resource ->
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                    else -> null
                }
            }.distinct()
            if (required.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
                runOnUiThread { request.grant(request.resources) }
            } else {
                pendingWebPermission = request
                requestPermissions(required.toTypedArray(), REQUEST_WEB_PERMISSION)
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            if (pendingWebPermission == request) pendingWebPermission = null
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            val safeOrigin = origin ?: return callback?.invoke(origin, false, false) ?: Unit
            val permissions = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (permissions.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
                callback?.invoke(safeOrigin, true, false)
            } else {
                pendingGeo = safeOrigin to (callback ?: return)
                requestPermissions(permissions, REQUEST_GEO_PERMISSION)
            }
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            val popup = WebView(this@MainActivity)
            popup.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (isAppUrl(uri)) webView.loadUrl(uri.toString()) else openExternal(uri)
                    popup.destroy()
                    return true
                }
            }
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }
    }

    private inner class MioAndroidBridge {
        @JavascriptInterface
        fun saveFile(fileName: String, mimeType: String, base64: String) {
            runOnUiThread {
                try {
                    pendingDownloadBytes = Base64.decode(base64, Base64.DEFAULT)
                    pendingDownloadMime = mimeType.takeIf { it.contains("/") } ?: "application/octet-stream"
                    pendingDownloadName = fileName
                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        .take(120)
                        .ifBlank { "Mio-export" }
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = pendingDownloadMime
                        putExtra(Intent.EXTRA_TITLE, pendingDownloadName)
                    }
                    startActivityForResult(intent, REQUEST_SAVE_FILE)
                } catch (_: Exception) {
                    pendingDownloadBytes = null
                    Toast.makeText(this@MainActivity, "导出文件失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
            }
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "已开始下载", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            openExternal(Uri.parse(url))
        }
    }

    @Deprecated("Handled for WebView file selection and exported files")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE) {
            val uris = if (resultCode == RESULT_OK && data != null) {
                val clip = data.clipData
                when {
                    clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                    data.data != null -> arrayOf(data.data!!)
                    else -> emptyArray()
                }
            } else emptyArray()
            fileChooserCallback?.onReceiveValue(uris)
            fileChooserCallback = null
        } else if (requestCode == REQUEST_SAVE_FILE) {
            val bytes = pendingDownloadBytes
            pendingDownloadBytes = null
            if (resultCode == RESULT_OK && data?.data != null && bytes != null) {
                try {
                    contentResolver.openOutputStream(data.data!!)?.use { it.write(bytes) }
                    Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "保存文件失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Required for WebView runtime permissions")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WEB_PERMISSION) {
            val request = pendingWebPermission
            pendingWebPermission = null
            if (request != null) {
                val requiredGranted = permissions.indices.all {
                    grantResults.getOrNull(it) == PackageManager.PERMISSION_GRANTED
                }
                if (requiredGranted) request.grant(request.resources) else request.deny()
            }
        } else if (requestCode == REQUEST_GEO_PERMISSION) {
            val pending = pendingGeo
            pendingGeo = null
            if (pending != null) {
                val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                pending.second.invoke(pending.first, granted, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("MioAndroid")
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_FILE = 4101
        private const val REQUEST_SAVE_FILE = 4102
        private const val REQUEST_WEB_PERMISSION = 4103
        private const val REQUEST_GEO_PERMISSION = 4104
    }
}
