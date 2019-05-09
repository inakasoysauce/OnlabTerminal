package com.example.terminal

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.session.MediaSession
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.protobuf.InvalidProtocolBufferException
import com.onlab.gymapp.TokenClass
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var nfcAdapter: NfcAdapter
    lateinit var pendingIntent: PendingIntent
    lateinit var intentFilters: Array<IntentFilter>
    lateinit var token: TokenClass.Token
    lateinit var functions: FirebaseFunctions
    lateinit var string: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        functions = FirebaseFunctions.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val nfcIntent = Intent(this,javaClass)
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this,0,nfcIntent,0)
        val ndefIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try{
            ndefIntentFilter.addDataType("text/plain")
            intentFilters = arrayOf(ndefIntentFilter)
        }catch (e: IntentFilter.MalformedMimeTypeException){
            Log.e(this.toString(),e.message)
        }
        string = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjE1NTc0MjE2NzIxODAiLCJ1aWQiOiJjZDB1S2QxdGJWWjJ1bktqN2Y0MmJacEpDdzUyIiwidXNhZ2VzIjo1LCJpYXQiOjE1NTc0MjE2NzJ9.8zSQIsCX_MPkoSOk200TEvqPoCpUYjRB3AmVB_RI3c0"
        useToken()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent!!.action)){
            val receivedArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (receivedArray != null){
                val message = receivedArray[0] as NdefMessage
                val records = message.records
                try {
                    token = TokenClass.Token.parseFrom(records[0].payload)
                    useToken()
                }catch (e: InvalidProtocolBufferException){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun useToken() {
        sendTokenToServer().addOnCompleteListener { task ->
            if(task.isSuccessful){
                val result = task.result
                if (result.equals("OK")){
                    text.text = getString(R.string.OK)
                    layout.setBackgroundColor(Color.GREEN)
                    layout.setOnClickListener {
                        layout.setBackgroundColor(Color.WHITE)
                        text.text = getString(R.string.rintse_ide_a_k_sz_l_ket)
                    }

                }
            }
            else{
                text.text = getString(R.string.not_ok)
                layout.setBackgroundColor(Color.RED)
             /*   Thread.sleep(3000)
                layout.setBackgroundColor(Color.WHITE)
                text.text = getString(R.string.rintse_ide_a_k_sz_l_ket)*/
            }
        }
    }

    private fun sendTokenToServer() : Task<String> {
        return functions.getHttpsCallable("useTicket")
            .call(string).continueWith { task ->
                val result = task.result!!.data as String
                result
            }
    }



    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,intentFilters,null)
    }
}
