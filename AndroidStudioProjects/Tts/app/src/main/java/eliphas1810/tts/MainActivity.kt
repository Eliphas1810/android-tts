package eliphas1810.tts

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {


    var textToSpeech: TextToSpeech? = null


    var editText: EditText? = null

    var speakButton: Button? = null
    var cancelButton: Button? = null


    var isStopping: Boolean = false
    var isStarting: Boolean = false
    var isCompleted: Boolean = true


    var lineList: List<String> = mutableListOf<String>()
    var lineIndex = 0
    var maxLineIndex = 0


    var scheduledExecutorService: ScheduledExecutorService? = null


    //メモリー上に作成される時にのみ呼ばれます。
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)


            //戻るボタン、戻るジェスチャーを無効化
            onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })


            textToSpeech = TextToSpeech(this, this)


            editText = findViewById(R.id.editText)


            speakButton = findViewById(R.id.speak)
            cancelButton = findViewById(R.id.cancel)


            speakButton?.isClickable = true
            cancelButton?.isClickable = false


            speakButton?.setOnClickListener { view ->
                try {

                    if(textToSpeech?.isSpeaking ?: true) {
                        Toast.makeText(applicationContext, getString(R.string.text_to_speech_is_busy), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    if (isStarting) {
                        return@setOnClickListener
                    }

                    speakButton?.isClickable = false
                    cancelButton?.isClickable = true
                    isStarting = true
                    isStopping = false

                    var text = editText?.text.toString() ?: ""

                    if (text.length == 0) {
                        isStarting = false
                        isStopping = false
                        speakButton?.isClickable = true
                        cancelButton?.isClickable = false
                        return@setOnClickListener
                    }

                    text = text.replace("\r\n", "\n")
                    text = text.replace("\r", "\n")
                    lineList = text.split("\n")
                    lineIndex = 0
                    maxLineIndex = lineList.size - 1

                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                    scheduledExecutorService?.scheduleAtFixedRate(
                        {
                            try {
                                if (isCompleted) {

                                    if (maxLineIndex < lineIndex || isStopping) {
                                        isStarting = false
                                        isStopping = false
                                        speakButton?.isClickable = true
                                        cancelButton?.isClickable = false

                                        lineIndex = 0

                                        scheduledExecutorService?.shutdown()

                                        return@scheduleAtFixedRate
                                    }

                                    var line = lineList[lineIndex]

                                    isCompleted = false
                                    if (1 <= line.length) {
                                        textToSpeech?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "line" + (lineIndex + 1))
                                    }
                                    lineIndex += 1
                                    isCompleted = true
                                }
                            } catch (exception: Exception) {
                                Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                                throw exception
                            }
                        },
                        1, //1回目までの時間間隔の時間数
                        1, //1回目以降の時間間隔の時間数
                        TimeUnit.SECONDS //時間の単位。秒。
                    )

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            cancelButton?.setOnClickListener { view ->
                try {

                    if (isStarting == false) {
                        return@setOnClickListener
                    }

                    speakButton?.isClickable = false
                    cancelButton?.isClickable = false

                    isStopping = true

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }



        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            //textToSpeech?.setSpeechRate(1.0f) //読み上げ速度
        }
    }


    //メモリーから破棄される時にのみ呼ばれます。
    override fun onDestroy() {
        try {


            textToSpeech?.shutdown()
            textToSpeech = null


            scheduledExecutorService?.shutdownNow()
            scheduledExecutorService = null


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.onDestroy()
        }
    }
}
