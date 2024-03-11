package com.example.uistatewithflowtest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uistatewithflowtest.ui.theme.UiStateWithFlowTestTheme

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
                    val uiState by viewModel.uiState.collectAsState()

                    LazyColumn(reverseLayout = true) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(64.dp)
                            ) {
                                RowText(text = "Static")
                                RowText(text = "Reaction")
                                RowText(text = "Individual")
                            }
                        }

                        items(
                            uiState.messages.reversed(),
                            key = { it.id}
                        ) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(64.dp)
                            ) {
                                val reaction by item.reaction.collectAsState(initial = null)
                                val individualEmitValue by item.scrap.collectAsState(
                                    initial = null
                                )

                                val context = LocalContext.current

                                RowText(text = item.staticValue)
                                RowText(
                                    text = reaction.toString(),
                                    modifier = remember(context, viewModel) {
                                        Modifier
                                            .clickable {
                                                val event = ReactionEvent.random(item.id)
                                                Toast.makeText(context, "${event::class.simpleName}", Toast.LENGTH_SHORT).show()
                                                viewModel.triggerNewReactionEvent(event)
                                            }
                                    }
                                )
                                RowText(text = individualEmitValue.toString())
                            }
                        }
                    }
                }
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