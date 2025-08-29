package com.example.dropzone

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PaymentActivity : AppCompatActivity() {

    private lateinit var editAmount: EditText
    private lateinit var buttonGPay: Button
    private lateinit var buttonPhonePe: Button
    private lateinit var buttonPaytm: Button
    private lateinit var buttonQR: Button

    private val upiId = "8905421455@ybl"
    private val payeeName = "DropZone"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        editAmount = findViewById(R.id.editAmount)
        buttonGPay = findViewById(R.id.buttonGPay)
        buttonPhonePe = findViewById(R.id.buttonPhonePe)
        buttonPaytm = findViewById(R.id.buttonPaytm)
        buttonQR = findViewById(R.id.buttonQR)

        buttonGPay.setOnClickListener { payViaUpi(editAmount.text.toString(), "com.google.android.apps.nbu.paisa.user") }
        buttonPhonePe.setOnClickListener { payViaUpi(editAmount.text.toString(), "com.phonepe.app") }
        buttonPaytm.setOnClickListener { payViaUpi(editAmount.text.toString(), "net.one97.paytm") }
        buttonQR.setOnClickListener { payViaUpi(editAmount.text.toString(), null) } // generic
    }

    private fun payViaUpi(amount: String, packageName: String?) {
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(
            "upi://pay?pa=$upiId&pn=$payeeName&tn=Donation for DropZone&am=$amount&cu=INR"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (packageName != null) intent.setPackage(packageName)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "App not found. Please install it first.", Toast.LENGTH_LONG).show()
        }
    }
}
