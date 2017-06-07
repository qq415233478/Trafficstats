package com.trafficstats.demo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View

class MainActivity : Activity() {

    companion object {
        val ACTION_KILL_SERVICE = "com.trafficstats.kill";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onStart(v: View) {
        if (!Settings.canDrawOverlays(this)) {
            var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            startActivity(intent)
        } else {
            startService(Intent(this@MainActivity, TrafficStateService::class.java))
        }
    }

    fun onStop(v: View) {
        val intent = Intent()
        intent.action = ACTION_KILL_SERVICE
        sendBroadcast(intent)
    }
}
