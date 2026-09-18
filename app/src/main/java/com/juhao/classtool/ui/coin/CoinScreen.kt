package com.juhao.classtool.ui.coin

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.juhao.classtool.R
import com.juhao.classtool.theme.AppCardDefaults
import kotlin.random.Random

@Composable
fun CoinScreen(modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    
    var showDialog by remember { mutableStateOf(false) }
    var coinIsInPositive by remember { mutableStateOf(false) }

    ScreenScaffold(
        scrollState = listState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "扔硬币") }
            }
            item {
                Button(
                    label = {
                        Text(
                            text = "开扔！",
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = { 
                        coinIsInPositive = Random.nextBoolean()
                        showDialog = true
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
    
    AlertDialog(
        visible = showDialog,
        onDismissRequest = {
            showDialog = false
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null
            )
        },
        title = { Text(text = "结果") },
        text = { Text(text = "硬币在 ${if (coinIsInPositive) "正面" else "背面"}") },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    showDialog = false
                }
            )
        }
    )
}