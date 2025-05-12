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