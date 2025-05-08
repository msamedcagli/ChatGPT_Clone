package com.msamedcagli.chatgptapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val messageList = mutableListOf<Message>()  // Kullanıcı ve yapay zekanın mesajlarını tutacak liste
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val question = findViewById<EditText>(R.id.questionBar)
        val btnSubmit = findViewById<ImageButton>(R.id.GonderButton)

        val recyclerView = findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter
        question.setOnEditorActionListener { _, _, _ ->
            btnSubmit.performClick()  // Butonun click olayını tetikler
            true
        }

        btnSubmit.setOnClickListener {
            val userQuestion = question.text.toString()
            if (userQuestion.isNotEmpty()) {
                // Kullanıcı sorusunu ekliyoruz
                messageList.add(Message("user", userQuestion))
                messageAdapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)

                getResponse(userQuestion) { response ->
                    // Yapay zekanın cevabını ekliyoruz
                    messageList.add(Message("ai", response))
                    runOnUiThread {
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
                question.text.clear()
            } else {
                Toast.makeText(this, "Lütfen bir soru yazın!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getResponse(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val json = """
        {
            "model": "gpt-3.5-turbo",  
            "messages": [
                {"role": "user", "content": "$question"}
            ]
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://example.com")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("İstek başarısız: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        val jsonResponse = JSONObject(body)
                        val responseMessage = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        callback(responseMessage)
                    } catch (e: Exception) {
                        callback("Cevap işlenemedi: ${e.message}")
                    }
                } else {
                    callback("Boş cevap alındı")
                }
            }
        })
    }
}

