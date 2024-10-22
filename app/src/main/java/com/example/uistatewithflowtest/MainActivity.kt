package com.example.uistatewithflowtest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uistatewithflowtest.repository.FetchType
import com.example.uistatewithflowtest.repository.message.Message
import com.example.uistatewithflowtest.ui.theme.UiStateWithFlowTestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
                        val messages by viewModel.messages.collectAsState()

                        MessageList(
                            messages,
                            viewModel.lazyListState,
                            ::startMainActivity,
                            viewModel::triggerNewReactionEvent,
                            viewModel::fetch,
                            modifier = Modifier.weight(1f)
                        )

                        TextField(
                            value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth()
                        )

                        Column {
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
                                                startMainActivity(i, null)
                                            }
                                    )
                                }
                            }
                            Row {
                                RowText(
                                    text = "Init",
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.fetchLatest()
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

    private fun startMainActivity(channelId: Long, messageId: Long?) {
        startActivity(getIntent(this, channelId, messageId))
    }

    companion object {

        fun getIntent(context: Context, channelId: Long, around: Long?): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(MainViewModel.ARG_CHANNEL_ID, channelId)
                putExtra(MainViewModel.ARG_AROUND, around)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageList(
    messages: List<Message>,
    lazyListState: LazyListState,
    onTextClick: (Long, Long) -> Unit,
    onReactionClick: (ReactionEvent) -> Unit,
    onFetch: (Long, FetchType) -> Unit,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val reversedMessages by rememberUpdatedState(messages.reversed())

    val firstVisibleItemIndex by remember {
        derivedStateOf {
            try {
                lazyListState.firstVisibleItemIndex
            } catch (e: Exception) {
                null
            }
        }
    }

    if (firstVisibleItemIndex == 0) {
        LaunchedEffect(messages.lastOrNull()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    // TODO 더 이상 스크롤 할 수 없고 사용자가 추가 스크롤을 시도할 경우로 조건 변경 필요.

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.canScrollForward }
            .collectLatest {
                if (it.not() && reversedMessages.isNotEmpty()) {
                    onFetch(reversedMessages.last().id.messageId, FetchType.Older)
                }
            }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.canScrollBackward }
            .collectLatest {
                if (it.not() && reversedMessages.isNotEmpty()) {
                    onFetch(reversedMessages.first().id.messageId, FetchType.Newer)
                }
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            reverseLayout = true,
        ) {
            items(
                reversedMessages,
                key = { it.id.messageId }
            ) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .padding(4.dp)
                        .animateItemPlacement()
                ) {
                    val reaction by item.reaction.collectAsState(initial = null)
                    val scrap by item.scrap.collectAsState(initial = null)

                    val context = LocalContext.current

                    RowText(
                        text = item.id.channelId.toString(),
                        modifier = Modifier.recomposeHighlighter()
                    )
                    RowText(
                        text = item.text,
                        modifier = remember(onTextClick) {
                            Modifier
                                .clickable {
                                    Toast
                                        .makeText(
                                            context,
                                            "Around: ${item.id.messageId}",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    onTextClick(item.id.channelId, item.id.messageId)
                                }
                                .recomposeHighlighter()
                        }
                    )
                    RowText(
                        text = reaction.toString(),
                        modifier = remember(onReactionClick) {
                            Modifier
                                .clickable {
                                    val event = ReactionEvent.random(item.id)
                                    Toast
                                        .makeText(
                                            context,
                                            "${event::class.simpleName}",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    onReactionClick(event)
                                }
                                .recomposeHighlighter()
                        }
                    )
                    RowText(
                        text = scrap.toString(),
                        modifier = Modifier.recomposeHighlighter()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = lazyListState.canScrollBackward && lazyListState.isScrollInProgress.not(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(color = Color.DarkGray, shape = CircleShape)
                    .padding(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
            ) {
                Text(text = "👇")
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
    )
}