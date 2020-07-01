package com.example.text

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    private val perms = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val Textbtn:Button = findViewById(R.id.btntext)
        val Objectbtn: Button = findViewById(R.id.btnobject)

        EasyPermissions.requestPermissions(
            this,
            "Permission to Access Camera and Storage is essential for the Apps Functionality!",
            0,
            *perms
        )

        Textbtn.setOnClickListener {
            val intent1 = Intent(this,TextActivity::class.java)
            startActivity(intent1)
            finish()
        }

        Objectbtn.setOnClickListener {
            val intent2 = Intent(this,ObjectActivity::class.java)
            startActivity(intent2)
            finish()
        }

    }
}
