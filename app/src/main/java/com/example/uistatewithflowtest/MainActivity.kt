package com.example.uistatewithflowtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uistatewithflowtest.ui.theme.UiStateWithFlowTestTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UiStateWithFlowTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val uiState by viewModel.uiState.collectAsState()

                    LazyColumn {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(64.dp)
                            ) {
                                Text(text = "Static")
                                Text(text = "Batch")
                                Text(text = "Individual")
                            }
                        }

                        items(uiState.items) { item ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(64.dp)
                            ) {
                                val batchEmitValue by item.batchEmitValue.collectAsState(initial = null)
                                val individualEmitValue by item.individualEmitValue.collectAsState(initial = null)
                                Text(text = item.staticValue.toString())
                                Text(text = batchEmitValue.toString())
                                Text(text = individualEmitValue.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}