package com.example.aichatbotapplication

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.Objects

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etmessage: EditText
    private lateinit var btnsend: CardView
    private lateinit var btnvoice: CardView
    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatModel>()
    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,bottom)
            insets
        }

        recyclerView=findViewById(R.id.recyclerView)
        etmessage=findViewById(R.id.etmessage)
        btnsend=findViewById(R.id.btnsend)
        btnvoice=findViewById(R.id.btnvoice)

        setUpRecyclerView()

        btnsend.setOnClickListener{
            if(etmessage.text.toString().isEmpty()){
                Toast.makeText(this,"Please Enter Your Question",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else{
                val message=etmessage.text.toString()
                addMessage(ChatModel(message,true))
                etmessage.text.clear()
                apicall(message)
            }
        }

        btnvoice.setOnClickListener {
            // on below line we are calling speech recognizer intent.
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Your AI")

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {

                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                addMessage(ChatModel(Objects.requireNonNull(res)[0],true))
                etmessage.text.clear()
                apicall(Objects.requireNonNull(res)[0])
            }
        }
    }

    private fun apicall(prompt: String){
        val url ="https://api.groq.com/openai/v1/chat/completions"
        val GROQ_API_KEY="gsk_your_secret_key"

        val jsonBody = JSONObject().apply{
            put("messages", JSONArray().apply {
                put( JSONObject().apply {
                    put("role","user")
                    put("content",prompt)
                })
            })
            put("model","qwen/qwen3.8-27b")
            put("temperature",0.6)
            put("max_completion_tokens",2048)
            put("top_p",0.95)
            put("stream",false)
            put("reasoning_effort","default")
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            Response.Listener { response ->
                try{
                    val answer = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    addMessage(ChatModel(answer,false))
                }
                catch(e : Exception){
                    addMessage(ChatModel("Error",false))
                }
            },
            Response.ErrorListener { error ->
                addMessage(ChatModel("Error",false))
            }){

            override fun getHeaders(): MutableMap<String?, String?>? {
                return hashMapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $GROQ_API_KEY"
                )
            }
        }

        Volley.newRequestQueue(this)
              .add(request)

        request.retryPolicy = DefaultRetryPolicy(
            60000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

    }



    fun addMessage(message: ChatModel) {
        chatList.add(message)
        adapter.notifyItemInserted(chatList.size-1)
        recyclerView.scrollToPosition(chatList.size-1)
    }

    private fun setUpRecyclerView(){
        adapter = ChatAdapter(chatList,this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}