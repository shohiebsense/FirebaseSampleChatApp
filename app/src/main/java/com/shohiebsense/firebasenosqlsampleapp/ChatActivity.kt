package com.shohiebsense.firebasenosqlsampleapp


import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_chat.*


class ChatActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var firebaseUser: FirebaseUser? = null

    lateinit var firebaseeDatabaseReference : DatabaseReference
    lateinit var firebaseAdapter : FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder>

    private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"

    companion object{
        const val REQUEST_INVITE = 1
        const val REQUEST_IMAGE = 2
        const val MESSAGES_CHILD = "messages"
        const val ANONYMOUS = "anonymous"
        const val MESSAGE_SENT_EVENT = "message_sent"
    }

    var userName = ""
    var photoUrl = ""
    lateinit var sharedPreferences: SharedPreferences
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        if(!isUserLoggedIn()) return
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        rv_chat.layoutManager = linearLayoutManager

        firebaseeDatabaseReference = FirebaseDatabase.getInstance().reference
        var parser = object: SnapshotParser<ChatMessage>{
            override fun parseSnapshot(snapshot: DataSnapshot): ChatMessage {
                val message = snapshot.getValue(ChatMessage::class.java)
                if(message != null){
                    message.id = snapshot.key!!
                }
                return message!!
            }
        }

        var messagesRef = firebaseeDatabaseReference.child(MESSAGES_CHILD)
        var options = FirebaseRecyclerOptions.Builder<ChatMessage>().setQuery(messagesRef, parser).build()

        firebaseAdapter =object : FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder>(
            options
        ){
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ChatMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return ChatMessageViewHolder(
                    layoutInflater.inflate(
                        R.layout.item_message,
                        parent,
                        false
                    ) as LinearLayout
                )
            }

            override fun onBindViewHolder(
                holder: ChatMessageViewHolder,
                position: Int,
                chatmessage: ChatMessage
            ) {
                holder.bind(chatmessage)
            }
        }


        firebaseAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount: Int = firebaseAdapter.getItemCount()
                val lastVisiblePosition: Int =
                    linearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    rv_chat.scrollToPosition(positionStart)
                }
            }
        })


        rv_chat.adapter = firebaseAdapter


        b_send.setOnClickListener {
            var message = ChatMessage(et_chat.text.toString(), userName, photoUrl, "")
            firebaseeDatabaseReference.child(MESSAGES_CHILD).push().setValue(message)
            et_chat.text.clear()
        }

        iv_add.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("image/*")
            startActivityForResult(intent, REQUEST_IMAGE)
        }
    }

    fun isUserLoggedIn() : Boolean{
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser
        if(firebaseUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return false
        }
        else{
            userName = firebaseUser!!.displayName!!
            if(firebaseUser!!.photoUrl != null){
                photoUrl = firebaseUser!!.photoUrl.toString()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_logout -> {
                firebaseAuth.signOut()
                googleSignInClient.signOut()

                userName = ANONYMOUS
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != RESULT_OK) return
        if(requestCode == REQUEST_IMAGE){
            if(data == null) return
            val uri = data.data
            val tempMessage = ChatMessage("", userName, photoUrl, LOADING_IMAGE_URL)
            firebaseeDatabaseReference.child(MESSAGES_CHILD).push().setValue(tempMessage, DatabaseReference.CompletionListener { error, ref ->
                if(error != null){
                    error.toException().printStackTrace()
                    return@CompletionListener
                }
                val key = ref.key
                val storageReference = FirebaseStorage.getInstance().getReference(firebaseUser!!.uid)
                    .child(key!!).child(uri!!.lastPathSegment!!)
                putImageInStorage(storageReference, uri, key)
            })
        }
    }

    fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String){
        storageReference.putFile(uri).addOnCompleteListener(this, object : OnCompleteListener<UploadTask.TaskSnapshot>{
            override fun onComplete(task: Task<UploadTask.TaskSnapshot>) {
                if(!task.isSuccessful){
                    task.exception?.printStackTrace()
                    return
                }
                task.result?.metadata?.reference?.downloadUrl?.addOnCompleteListener {
                    if(!it.isSuccessful){
                        it.exception?.printStackTrace()
                        return@addOnCompleteListener
                    }
                    val message = ChatMessage("", userName, photoUrl, it.result.toString())
                    firebaseeDatabaseReference.child(MESSAGES_CHILD).child(key).setValue(message)
                }
            }

        })
    }

    override fun onPause() {
        firebaseAdapter.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter.startListening()
    }
}