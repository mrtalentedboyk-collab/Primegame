package com.primegame.aimguide

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var waiting = false
    private var notifAsked = false
    private var captureAsked = false
    private var capCode = 0
    private var capData: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#090711"))
            setPadding(48, 48, 48, 48)
        }
        val title = TextView(this).apply {
            text = "AIM GUIDE"
            textSize = 28f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "START chaple permission chaibe, tarpor Carrom chalu hobe.\n" +
                "Carrom er upor ekta M logo bhashbe. Logo te click korle menu khulbe."
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 48)
        }
        val start = Button(this).apply {
            text = "START"
            setOnClickListener {
                waiting = true
                captureAsked = false
                capData = null
                step()
            }
        }
        val stop = Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }
        root.addView(title)
        root.addView(hint)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (waiting) step()
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (waiting) step()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK && data != null) {
                capCode = resultCode
                capData = data
            } else {
                Toast.makeText(this, "Screen capture deoa hoy ni, Aim Assist kaj korbe na", Toast.LENGTH_LONG).show()
            }
            if (waiting) step()
        }
    }

    @Suppress("DEPRECATION")
    private fun step() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Display over other apps ON koro", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !notifAsked &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifAsked = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        if (!captureAsked) {
            captureAsked = true
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpm.createScreenCaptureIntent(), 100)
            return
        }
        waiting = false
        val svc = Intent(this, OverlayService::class.java)
        capData?.let {
            svc.putExtra("code", capCode)
            svc.putExtra("data", it)
        }
        startForegroundService(svc)
        openCarrom()
    }

    private fun openCarrom() {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(main, 0)
        val match = apps.firstOrNull {
            it.activityInfo.packageName != packageName &&
                (it.loadLabel(packageManager).toString().contains("carrom", ignoreCase = true) ||
                    it.activityInfo.packageName.contains("carrom", ignoreCase = true))
        }
        val launch = match?.let { packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (launch != null) {
            startActivity(launch)
        } else {
            Toast.makeText(this, "Carrom app pai ni, manually open koro", Toast.LENGTH_LONG).show()
        }
    }
}
