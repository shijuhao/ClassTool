package com.juhao.classtool.ui.about

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.juhao.classtool.utils.getAppVersionInfo

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    
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
                ) { Text(text = "关于") }
            }
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "ClassTool"
                )
            }
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "课间娱乐/小工具"
                )
            }
            item {
                FilledTonalButton(
                    onClick = { },
                    label = { Text("版本号") },
                    secondaryLabel = { Text(context.getAppVersionInfo().versionName) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
            item {
                FilledTonalButton(
                    onClick = { },
                    label = { Text("开发者") },
                    secondaryLabel = { Text("JuHao") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
            item {
                FilledTonalButton(
                    onClick = { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/shijuhao/classtool".toUri()))
                    },
                    label = { Text("源代码") },
                    secondaryLabel = { Text("https://github.com/shijuhao/classtool") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.code),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}