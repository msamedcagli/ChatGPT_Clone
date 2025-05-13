package com.msamedcagli.deneme

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class ChatFragment : Fragment() {
    private val client = OkHttpClient()
    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                messageList.add(Message("user", "Resmi incele", uri))
                messageAdapter.notifyItemInserted(messageList.size - 1)
                requireView().findViewById<RecyclerView>(R.id.messagesRecyclerView).scrollToPosition(messageList.size - 1)
                
                // Resmi analiz et
                analyzeImage(uri)
            }
        }
    }

    private fun analyzeImage(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val apiKey = "" //Api key yazılacak
            val url = "https://api.openai.com/v1/chat/completions"

            val json = """
            {
                "model": "gpt-4o-2024-05-13",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "Bu resmi detaylı analiz et."
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "data:image/jpeg;base64,$base64Image"
                                }
                            }
                        ]
                    }
                ],
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

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        messageList.add(Message("ai", "Resim analizi başarısız: ${e.message}"))
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonResponse = JSONObject(body)
                            
                            if (jsonResponse.has("error")) {
                                val error = jsonResponse.getJSONObject("error")
                                requireActivity().runOnUiThread {
                                    messageList.add(Message("ai", "API Hatası: ${error.getString("message")}"))
                                    messageAdapter.notifyItemInserted(messageList.size - 1)
                                }
                                return
                            }
                            
                            val responseMessage = jsonResponse
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                            requireActivity().runOnUiThread {
                                messageList.add(Message("ai", responseMessage))
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                            }
                        } catch (e: Exception) {
                            requireActivity().runOnUiThread {
                                messageList.add(Message("ai", "Cevap işlenemedi: ${e.message}"))
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            messageList.add(Message("ai", "Boş cevap alındı"))
                            messageAdapter.notifyItemInserted(messageList.size - 1)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Resim işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
        val btnImage = view.findViewById<ImageButton>(R.id.GorselButton)

        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_anaSayfaFragment)
        }

        btnImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
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
        val apiKey = "" //Api key yazılacak
        val url = "https://api.openai.com/v1/chat/completions"

        val json = """
        {
            "model": "gpt-4o-2024-05-13",
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
