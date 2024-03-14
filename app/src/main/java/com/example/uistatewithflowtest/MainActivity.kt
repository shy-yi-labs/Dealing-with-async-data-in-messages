package com.example.uistatewithflowtest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uistatewithflowtest.ui.theme.UiStateWithFlowTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UiStateWithFlowTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsState()

                        LazyColumn(
                            reverseLayout = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            items(
                                uiState.messages.reversed(),
                                key = { it.id }
                            ) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                        .padding(4.dp)
                                ) {
                                    val reaction by item.reaction.collectAsState(initial = null)
                                    val individualEmitValue by item.scrap.collectAsState(
                                        initial = null
                                    )

                                    val context = LocalContext.current

                                    RowText(text = item.channelId.toString())
                                    RowText(text = item.staticValue)
                                    RowText(
                                        text = reaction.toString(),
                                        modifier = remember(context, viewModel) {
                                            Modifier
                                                .clickable {
                                                    val event = ReactionEvent.random(item.id)
                                                    Toast.makeText(
                                                        context,
                                                        "${event::class.simpleName}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    viewModel.triggerNewReactionEvent(event)
                                                }
                                        }
                                    )
                                    RowText(text = individualEmitValue.toString())
                                }
                            }
                        }

                        Column {
                            val context = LocalContext.current

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color.Black)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                RowText(text = "Channel")
                                RowText(text = "Text")
                                RowText(text = "Reaction")
                                RowText(text = "Scrap")
                            }
                            Row {
                                for (i in 0L..3L) {
                                    RowText(
                                        text = "Channel $i",
                                        modifier = Modifier
                                            .clickable {
                                                startActivity(Companion.getIntent(context, i))
                                            }
                                    )
                                }
                            }
                            Row {
                                RowText(
                                    text = "Init",
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.initMessages()
                                        }
                                )
                                RowText(
                                    text = "Clear",
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.clearMessages()
                                        }
                                )
                                RowText(
                                    text = "Drop",
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.dropMessages()
                                        }
                                )
                            }
                            Row {
                                val messagesState = remember { viewModel.messagesState }
                                var isPushOn by rememberSaveable { mutableStateOf(messagesState.allowPush) }

                                RowText(
                                    text = "Push: $isPushOn",
                                    modifier = Modifier
                                        .clickable {
                                            isPushOn = !isPushOn
                                            viewModel.messagesState.allowPush = isPushOn
                                        }
                                )

                                var isAsyncOn by rememberSaveable { mutableStateOf(!messagesState.awaitInitialization) }
                                RowText(
                                    text = "Async: $isAsyncOn",
                                    modifier = Modifier
                                        .clickable {
                                            isAsyncOn = !isAsyncOn
                                            viewModel.messagesState.awaitInitialization = !isAsyncOn
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        fun getIntent(context: Context, channelId: Long): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(MainViewModel.ARG_CHANNEL_ID, channelId)
            }
        }
    }
}

@Composable
fun RowScope.RowText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        modifier = modifier
            .weight(1f)
            .recomposeHighlighter()
    )
}