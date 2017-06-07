package com.trafficstats.demo

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SystemActionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            if (!isServiceRunning(context, TrafficStateService::class.java.name)) {
                val startServiceIntent = Intent(context, TrafficStateService::class.java)
                context.startService(startServiceIntent)
                Log.d(TAG, "restart service")
            } else {
                Log.d(TAG, "service is running")
            }
        }
    }

    companion object {
        val TAG = "SystemActionReceiver"

        /**
         * 用来判断服务是否运行.

         * @param className 判断的服务名字
         * *
         * @return true 在运行 false 不在运行
         */
        fun isServiceRunning(mContext: Context, className: String): Boolean {
            var isRunning = false
            val activityManager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val serviceList = activityManager.getRunningServices(30)
            if (serviceList.size <= 0) {
                return false
            }
            for (i in serviceList.indices) {
                if (serviceList[i].service.className == className == true) {
                    isRunning = true
                    break
                }
            }
            return isRunning
        }
    }
}
