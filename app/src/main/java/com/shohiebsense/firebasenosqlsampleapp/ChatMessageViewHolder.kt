package com.shohiebsense.firebasenosqlsampleapp

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_message.view.*

class ChatMessageViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout) {

    fun bind(chatMessage: ChatMessage){
        if(chatMessage.text.isNotEmpty()) {
            layout.tv_message.text = chatMessage.text
            layout.tv_message.visibility = View.VISIBLE
            layout.iv_message_image.visibility = View.GONE
        } else if(chatMessage.imageUrl.isNotEmpty()){
            val imageUrl = chatMessage.imageUrl
            if(imageUrl.startsWith("gs://")){
                var storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                storageReference.downloadUrl.addOnCompleteListener {
                    if(it.isSuccessful){
                        var downloadUrl = it.result.toString()
                        Glide.with(layout.context).load(downloadUrl).into(layout.iv_message_image)
                    }
                    else{
                        it.exception?.printStackTrace()
                    }
                }
            }
            else{
                Glide.with(layout.context).load(chatMessage.imageUrl).into(layout.iv_message_image)

            }
            layout.iv_message_image.visibility = View.VISIBLE
            layout.tv_message.visibility = View.GONE
        }

        layout.tv_messenger.text = chatMessage.name
        if(chatMessage.photoUrl.isEmpty()){
            layout.iv_message_sender_image.setImageDrawable(ContextCompat.getDrawable(layout.context, R.drawable.ic_account_circle_black_36dp))
        }
        else{
            Glide.with(layout.context).load(chatMessage.photoUrl).into(layout.iv_message_sender_image)
        }

    }


}