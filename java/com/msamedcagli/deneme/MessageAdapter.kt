package com.msamedcagli.deneme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].role == "user") VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.user_message, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.gpt_message, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is UserViewHolder) {
            holder.userMessage.text = message.content
            if (message.imageUri != null) {
                holder.userImage.visibility = View.VISIBLE
                holder.userImage.setImageURI(message.imageUri)
            } else {
                holder.userImage.visibility = View.GONE
            }
        } else if (holder is AiViewHolder) {
            holder.aiMessage.text = message.content
        }
    }

    override fun getItemCount(): Int = messageList.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessage: TextView = view.findViewById(R.id.messageContent)
        val userImage: ImageView = view.findViewById(R.id.messageImage)
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val aiMessage: TextView = view.findViewById(R.id.aiMessageContent)
    }
}
