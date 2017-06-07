package com.trafficstats.demo

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.RelativeLayout
import android.widget.TextView
import java.text.DecimalFormat
import java.util.*

class TrafficStateService : Service() {

    private var mRxTotalData: Long = 0
    // private long mTxTotalData;
    private var mTimerTask: TimerTask? = null
    private var mTimer: Timer? = null
    private var mFormat: DecimalFormat? = null

    private var mWindowManager: WindowManager? = null
    private var mFloatLayout: View? = null
    private var mParams: LayoutParams? = null

    private var mHandler: Handler? = null

    private var mNeedRestart = true
    private var mIntentFilter: IntentFilter? = null
    private var mKillReceiver: KillBroadcastReceiver? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createFloatView()
        mFormat = DecimalFormat("###.#")
        mHandler = UpdateHandler()
        registerKillActionReceiver()

        mTimerTask = object : TimerTask() {
            override fun run() {
                if (mHandler != null) {
                    val msg = Message.obtain()
                    msg.obj = speed2String(rxSpeed)
                    mHandler!!.sendMessage(msg)
                }
            }
        }
    }

    private fun registerKillActionReceiver() {
        mKillReceiver = KillBroadcastReceiver()
        mIntentFilter = IntentFilter()
        mIntentFilter!!.addAction(MainActivity.ACTION_KILL_SERVICE)
        registerReceiver(mKillReceiver, mIntentFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mRxTotalData = TrafficStats.getTotalRxBytes()
        // mTxTotalData = TrafficStats.getTotalTxBytes();
        try {
            mTimer = Timer()
            mTimer!!.schedule(mTimerTask, 0, (1000 * PERIOD_SEC).toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return super.onStartCommand(intent, Service.START_STICKY, startId)
    }

    override fun onDestroy() {
        if (mTimerTask != null) {
            mTimerTask!!.cancel()
        }
        if (mTimer != null) {
            mTimer!!.cancel()
        }
        if (mWindowManager != null) {
            mWindowManager!!.removeView(mFloatLayout)
        }
        if (mKillReceiver != null) {
            unregisterReceiver(mKillReceiver)
        }
        if (mNeedRestart) {
            val intent = Intent(this@TrafficStateService, TrafficStateService::class.java)
            startService(intent)
        }
        super.onDestroy()
    }

    private val rxSpeed: Double
        get() {
            val rxSpeed = TrafficStats.getTotalRxBytes() - mRxTotalData
            mRxTotalData = TrafficStats.getTotalRxBytes()
            return rxSpeed.toDouble()
        }

    // private double getTxSpeed() {
    // long txSpeed = TrafficStats.getTotalTxBytes() - mTxTotalData;
    // mTxTotalData = TrafficStats.getTotalTxBytes();
    // return txSpeed;
    // }

    private fun speed2String(speed: Double): String {
        var speed = speed
        if (speed >= 0) {
            speed /= PERIOD_SEC.toDouble()
            val b = (speed % 1024).toInt()
            val kb = speed / 1024.0
            val mb = speed / (1024.0 * 1024.0 * 1.0)
            if (mb >= 1) {
                return mFormat!!.format(mb) + " M/S"
            } else if (kb >= 1) {
                if (kb >= 100)
                    return mFormat!!.format(kb.toInt().toLong()) + " K/S"
                return mFormat!!.format(kb) + " K/S"
            } else {
                return b.toString() + " B/S"
            }
        }
        return ""
    }

    private fun createFloatView() {
        mParams = LayoutParams()
        mWindowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mParams!!.type = LayoutParams.TYPE_PHONE
        mParams!!.format = PixelFormat.RGBA_8888
        mParams!!.flags = LayoutParams.FLAG_NOT_FOCUSABLE
        mParams!!.gravity = Gravity.START or Gravity.TOP
        mParams!!.x = 0
        mParams!!.y = 0
        mParams!!.width = LayoutParams.WRAP_CONTENT
        mParams!!.height = LayoutParams.WRAP_CONTENT
        val inflater = LayoutInflater.from(application)
        mFloatLayout = inflater.inflate(R.layout.layout_pop_traffic_state, null) as RelativeLayout
        mWindowManager!!.addView(mFloatLayout, mParams)
        mSpeedTextView = mFloatLayout!!.findViewById(R.id.tv_traffic_pop_layout) as TextView

        mFloatLayout!!.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        mFloatLayout!!.setOnTouchListener { _, event ->
            mParams!!.x = event.rawX.toInt() - mFloatLayout!!.measuredWidth / 2
            mParams!!.y = event.rawY.toInt() - mFloatLayout!!.measuredHeight / 2 - 25
            mWindowManager!!.updateViewLayout(mFloatLayout, mParams)
            false
        }
    }

    internal class UpdateHandler : Handler() {
        override fun handleMessage(msg: Message) {
            if (mSpeedTextView != null) {
                mSpeedTextView!!.text = msg.obj.toString()
            }
        }
    }

    internal inner class KillBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mNeedRestart = false
            stopSelf()
        }
    }

    companion object {
        private var mSpeedTextView: TextView? = null

        private val PERIOD_SEC = 2

    }
}
