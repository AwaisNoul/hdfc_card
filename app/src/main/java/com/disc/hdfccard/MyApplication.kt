package com.disc.hdfccard

import android.app.Application
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Telephony
import com.disc.hdfccard.databinding.ActivityMainBinding

class MyApplication : Application(){
    companion object{
        var old=""
    }
    override fun onCreate() {
        super.onCreate()
        var smsReceiver = SmsReceiver()
        val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(smsReceiver, intentFilter)
    }
}