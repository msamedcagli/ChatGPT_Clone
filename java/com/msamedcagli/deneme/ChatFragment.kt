package com.msamedcagli.deneme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChatFragment : Fragment() {
    private val client = OkHttpClient()
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val question = view.findViewById<EditText>(R.id.questionBar)
        val btnSubmit = view.findViewById<ImageButton>(R.id.GonderButton)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_anaSayfaFragment)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = messageAdapter

        question.setOnEditorActionListener { _, _, _ ->
            btnSubmit.performClick()
            true
        }

        btnSubmit.setOnClickListener {
            val userQuestion = question.text.toString()
            if (userQuestion.isNotEmpty()) {
                messageList.add(Message("user", userQuestion))
                messageAdapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)

                getResponse(userQuestion) { response ->
                    messageList.add(Message("ai", response))
                    requireActivity().runOnUiThread {
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
                question.text.clear()
            } else {
                Toast.makeText(requireContext(), "Lütfen bir soru yazın!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getResponse(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val json = """
        {
            "model": "openai/gpt-3.5-turbo",
            "messages": [
                {"role": "user", "content": "$question"}
            ],
            "temperature": 0.7,
            "max_tokens": 1000
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://example.com")
            .addHeader("X-Title", "Deneme App")
            .post(requestBody)
            .build()

        println("API İsteği: $json") // Debug için isteği yazdır

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("İstek başarısız: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        println("API Yanıtı: $body") // Debug için yanıtı yazdır
                        println("Response Code: ${response.code}") // Response kodunu yazdır
                        
                        val jsonResponse = JSONObject(body)
                        
                        if (jsonResponse.has("error")) {
                            val error = jsonResponse.getJSONObject("error")
                            callback("API Hatası: ${error.getString("message")}")
                            return
                        }
                        
                        val responseMessage = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        callback(responseMessage)
                    } catch (e: Exception) {
                        callback("Cevap işlenemedi: ${e.message}\nYanıt: $body")
                    }
                } else {
                    callback("Boş cevap alındı")
                }
            }
        })
    }
}
