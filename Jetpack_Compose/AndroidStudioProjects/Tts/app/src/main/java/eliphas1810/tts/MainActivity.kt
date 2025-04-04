package eliphas1810.tts


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import eliphas1810.tts.ui.theme.TtsTheme

import android.speech.tts.TextToSpeech

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview


class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {


    var textToSpeech: TextToSpeech? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            textToSpeech = TextToSpeech(this, this)

            enableEdgeToEdge()
            setContent {
                TtsTheme {
                    BuildView(textToSpeech)
                }
            }
        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    override fun onInit(status: Int) {
        //if (status == TextToSpeech.SUCCESS) {
            //textToSpeech?.setSpeechRate(1.0f) //読み上げ速度
        //}
    }


    override fun onDestroy() {
        try {
            textToSpeech?.shutdown()
            textToSpeech = null

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.onDestroy()
        }
    }
}


@Composable
fun BuildView(textToSpeech: TextToSpeech?) {
    val context = LocalContext.current

    val speakLabel = context.getString(R.string.speak)
    val cancelLabel = context.getString(R.string.cancel)
    val textLabel = context.getString(R.string.text_label)
    val textToSpeechIsBusyMessage = context.getString(R.string.text_to_speech_is_busy)
    val license = context.getString(R.string.license)

    var isStopping = false
    var isStarting = false
    var isCompleted = true

    var scheduledExecutorService: ScheduledExecutorService? = null

    var isSpeakButtonEnabled by rememberSaveable { mutableStateOf(true) }
    var isCancelButtonEnabled by rememberSaveable { mutableStateOf(false) }

    var text by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column (
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(20.dp))
            Row {
                Button(
                    onClick = {

                        if (textToSpeech?.isSpeaking ?: true) {
                            Toast.makeText(context, textToSpeechIsBusyMessage, Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (isStarting) {
                            return@Button
                        }

                        isSpeakButtonEnabled = false
                        isCancelButtonEnabled = true

                        isStarting = true
                        isStopping = false

                        var string = text

                        if (text.length == 0) {
                            isStarting = false
                            isStopping = false
                            isSpeakButtonEnabled = true
                            isCancelButtonEnabled = false
                            return@Button
                        }

                        string = string.replace("\r\n", "\n")
                        string = string.replace("\r", "\n")
                        val lineList = string.split("\n")
                        var lineIndex = 0
                        val maxLineIndex = lineList.size - 1

                        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                        scheduledExecutorService?.scheduleAtFixedRate(
                            {
                                if (isCompleted) {

                                    if (maxLineIndex < lineIndex || isStopping) {
                                        isStarting = false
                                        isStopping = false
                                        isSpeakButtonEnabled = true
                                        isCancelButtonEnabled = false

                                        scheduledExecutorService?.shutdown()

                                        return@scheduleAtFixedRate
                                    }

                                    val line = lineList[lineIndex]

                                    isCompleted = false
                                    if (1 <= line.length) {
                                        textToSpeech?.speak(line, TextToSpeech.QUEUE_FLUSH, null, "line" + (lineIndex + 1))
                                    }
                                    lineIndex += 1
                                    isCompleted = true
                                }
                            },
                            1, //1回目までの時間間隔の時間数
                            1, //1回目以降の時間間隔の時間数
                            TimeUnit.SECONDS //時間の単位。秒。
                        )
                    },
                    enabled = isSpeakButtonEnabled
                ) {
                    Text(speakLabel)
                }
                Spacer(Modifier.size(50.dp))
                Button(
                    onClick = {

                        if (isStarting == false) {
                            return@Button
                        }

                        isSpeakButtonEnabled = false
                        isCancelButtonEnabled = false

                        isStopping = true
                    },
                    enabled = isCancelButtonEnabled
                ) {
                    Text(cancelLabel)
                }
            }
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(textLabel)
                }
            )
            Spacer(Modifier.size(10.dp))
            Text(text = license)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBuildView() {
    TtsTheme {
        BuildView(null)
    }
}
