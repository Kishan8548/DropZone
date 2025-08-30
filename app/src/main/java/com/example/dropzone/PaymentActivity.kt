package com.example.dropzone

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dropzone.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding

    private val upiId = "8905421455@ybl"
    private val payeeName = "DropZone"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonGPay.setOnClickListener {
            payViaUpi(binding.editAmount.text.toString(), "com.google.android.apps.nbu.paisa.user")
        }
        binding.buttonPhonePe.setOnClickListener {
            payViaUpi(binding.editAmount.text.toString(), "com.phonepe.app")
        }
        binding.buttonPaytm.setOnClickListener {
            payViaUpi(binding.editAmount.text.toString(), "net.one97.paytm")
        }
        binding.buttonQR.setOnClickListener {
            payViaUpi(binding.editAmount.text.toString(), null) // generic UPI
        }
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
