package com.juhao.classtool.ui.about

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.R
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class FireworkParticle(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val trail: Boolean
)

private data class FireworkBurst(
    val center: Offset,
    val particles: List<FireworkParticle>,
    val startTime: Long,
    val flash: Boolean
)

@Composable
private fun FireworksOverlay(active: Boolean) {
    if (!active) return

    val ringColor = MaterialTheme.colorScheme.onBackground

    val bursts = remember { mutableStateListOf<FireworkBurst>() }
    val palette = remember {
        listOf(
            Color(0xFFFF6B9D),
            Color(0xFFFFD54F),
            Color(0xFF64B5F6),
            Color(0xFF4DB6AC),
            Color(0xFFBA68C8),
            Color(0xFFFF8A65),
            Color(0xFFFF5252),
            Color(0xFF69F0AE)
        )
    }

    LaunchedEffect(active) {
        while (active) {
            val center = Offset(
                x = Random.nextFloat() * 0.7f + 0.15f,
                y = Random.nextFloat() * 0.6f + 0.12f
            )
            val count = Random.nextInt(22, 34)
            val baseColor = palette.random()
            val particles = List(count) {
                FireworkParticle(
                    angle = (it.toFloat() / count) * 2f * Math.PI.toFloat() +
                        Random.nextFloat() * 0.15f,
                    speed = Random.nextFloat() * 0.7f + 0.6f,
                    color = if (Random.nextFloat() < 0.7f) baseColor else palette.random(),
                    size = Random.nextFloat() * 4f + 3f,
                    trail = Random.nextFloat() < 0.5f
                )
            }
            bursts.add(
                FireworkBurst(
                    center = center,
                    particles = particles,
                    startTime = System.currentTimeMillis(),
                    flash = Random.nextFloat() < 0.4f
                )
            )
            delay(Random.nextLong(280, 550))
        }
    }

    val frame by produceState(0L, active) {
        while (active) {
            value = System.currentTimeMillis()
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = frame
        bursts.removeAll { now - it.startTime > 1800 }
        for (burst in bursts) {
            val progress = ((now - burst.startTime) / 1800f).coerceIn(0f, 1f)
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val cx = burst.center.x * size.width
            val cy = burst.center.y * size.height
            val radius = progress * size.minDimension * 0.62f

            if (burst.flash && progress < 0.18f) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - progress / 0.18f) * 0.35f),
                    radius = size.minDimension * 0.18f,
                    center = Offset(cx, cy)
                )
            }

            for (p in burst.particles) {
                val dx = cos(p.angle) * radius * p.speed
                val dy = sin(p.angle) * radius * p.speed
                val pos = Offset(cx + dx, cy + dy)

                if (p.trail) {
                    val trailDx = cos(p.angle) * radius * p.speed * 0.85f
                    val trailDy = sin(p.angle) * radius * p.speed * 0.85f
                    drawLine(
                        color = p.color.copy(alpha = alpha * 0.5f),
                        start = Offset(cx + trailDx, cy + trailDy),
                        end = pos,
                        strokeWidth = p.size * 0.8f
                    )
                }

                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * (1f - progress * 0.4f),
                    center = pos
                )

                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    radius = p.size * 0.30f,
                    center = pos
                )
            }

            drawCircle(
                color = ringColor.copy(alpha = alpha * 0.45f),
                radius = radius * 0.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = ringColor.copy(alpha = alpha * 0.25f),
                radius = radius * 0.85f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionInfo = remember { context.getAppVersionInfo() }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var secretTaps by remember { mutableIntStateOf(0) }
    var secretActive by remember { mutableStateOf(false) }
    var secretPulse by remember { mutableStateOf(false) }

    LaunchedEffect(secretTaps) {
        if (secretTaps == 0) return@LaunchedEffect
        delay(1200)
        secretTaps = 0
    }

    LaunchedEffect(secretActive) {
        if (!secretActive) return@LaunchedEffect
        while (true) {
            secretPulse = true
            delay(420)
            secretPulse = false
            delay(420)
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = when {
            secretActive && secretPulse -> 1.18f
            secretActive -> 1.08f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "secretIconScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = "应用图标",
                        modifier = Modifier
                            .size(64.dp)
                            .scale(iconScale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                secretTaps++
                                if (secretTaps >= 7) {
                                    secretTaps = 0
                                    secretActive = !secretActive
                                }
                            }
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            }
                    )
                }
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        textAlign = TextAlign.Center,
                        text = if (secretActive) "ClassTool · 特别版" else "ClassTool",
                        color = if (secretActive) Color(0xFFFFD54F) else Color.Unspecified
                    )
                }
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        textAlign = TextAlign.Center,
                        text = if (secretActive) "今天也要好好上课哦" else "课间娱乐/小工具"
                    )
                }

                item {
                    ListSubHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text("应用信息") }
                }

                item {
                    FilledTonalButton(
                        onClick = { },
                        label = { Text("版本号") },
                        secondaryLabel = { Text(versionInfo.versionName) },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { },
                        label = { Text("包名") },
                        secondaryLabel = { Text(context.packageName) },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }

                item {
                    ListSubHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text("团队") }
                }

                item {
                    FilledTonalButton(
                        onClick = { },
                        label = { Text("开发者") },
                        secondaryLabel = { Text("JuHao") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "mailto:juhaoluoye@163.com".toUri()
                                )
                            )
                        },
                        label = { Text("联系我") },
                        secondaryLabel = { Text("反馈与建议") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Mail,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }

                item {
                    ListSubHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text("开源") }
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/shijuhao/classtool".toUri()
                                )
                            )
                        },
                        label = { Text("源代码") },
                        secondaryLabel = { Text("github.com/shijuhao/classtool") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Code,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/shijuhao/classtool/issues".toUri()
                                )
                            )
                        },
                        label = { Text("问题反馈") },
                        secondaryLabel = { Text("提交 Issue") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Bug_report,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.gnu.org/licenses/gpl-3.0.html".toUri()
                                )
                            )
                        },
                        label = { Text("开源协议") },
                        secondaryLabel = { Text("GPL 3.0") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
            }
        }

        FireworksOverlay(active = secretActive)
    }
}