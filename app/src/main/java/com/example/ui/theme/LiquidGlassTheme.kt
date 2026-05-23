@file:Suppress("DEPRECATION")
package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.sin

// --- Color Additions ---
object GlassColors {
    val DeepPurple = Color(0xFF140D26)
    val OceanBlue = Color(0xFF0F1E36)
    val MintAqua = Color(0xFF00F5D4)
    val CoralRose = Color(0xFF7B2CBF)
    val GlassWhite = Color(0x15FFFFFF)
    val GlassDark = Color(0x350A0A14)
    val GlassBorderLight = Color(0x45FFFFFF)
    val GlassBorderAccent = Color(0x8000F5D4)
    val NeonBlue = Color(0xFF0077B6)
    val NeonCyan = Color(0xFF00E5FF)
    val NeonPink = Color(0xFFF72585)
    
    val DarkBackgroundGradient = listOf(
        Color(0xFF0B0914),
        Color(0xFF130E29),
        Color(0xFF0A1128),
        Color(0xFF080612)
    )
}

// --- Dynamic Liquid Canvas Background Component ---
@Composable
fun LiquidFlowBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Liquid Flow Dynamics")
    
    // Wave 1 variables
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave Phase 1"
    )
    
    // Wave 2 variables
    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = Math.PI.toFloat(),
        targetValue = (3 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave Phase 2"
    )

    // Fluid Glowing Bubble variables
    val bubbleY1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Bubble Y 1"
    )
    
    val bubbleY2 by infiniteTransition.animateFloat(
        initialValue = 1100f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "Bubble Y 2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = GlassColors.DarkBackgroundGradient
                )
            )
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw standard glass-ambient glowing fluid blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7B2CBF).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(canvasWidth * 0.25f, bubbleY1),
                        radius = 450.dp.toPx()
                    ),
                    radius = 450.dp.toPx(),
                    center = Offset(canvasWidth * 0.25f, bubbleY1)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00F5D4).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(canvasWidth * 0.8f, bubbleY2),
                        radius = 400.dp.toPx()
                    ),
                    radius = 400.dp.toPx(),
                    center = Offset(canvasWidth * 0.8f, bubbleY2)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF007F).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                        radius = 350.dp.toPx()
                    ),
                    radius = 350.dp.toPx(),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f)
                )

                // Draw realistic flowing organic liquid waves across bottom
                val wavePath = Path()
                wavePath.moveTo(0f, canvasHeight)
                for (x in 0..canvasWidth.toInt() step 8) {
                    val angle = (x / canvasWidth) * 2 * Math.PI + waveOffset1
                    val y = canvasHeight - 120.dp.toPx() + (sin(angle) * 25.dp.toPx()).toFloat()
                    wavePath.lineTo(x.toFloat(), y)
                }
                wavePath.lineTo(canvasWidth, canvasHeight)
                wavePath.close()

                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x307B2CBF),
                            Color(0x70140D26)
                        )
                    )
                )

                val wavePath2 = Path()
                wavePath2.moveTo(0f, canvasHeight)
                for (x in 0..canvasWidth.toInt() step 8) {
                    val angle = (x / canvasWidth) * 2 * Math.PI + waveOffset2
                    val y = canvasHeight - 90.dp.toPx() + (sin(angle) * 18.dp.toPx()).toFloat()
                    wavePath2.lineTo(x.toFloat(), y)
                }
                wavePath2.lineTo(canvasWidth, canvasHeight)
                wavePath2.close()

                drawPath(
                    path = wavePath2,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x2500F5D4),
                            Color(0x500A1128)
                        )
                    )
                )
            }
    ) {
        content()
    }
}

// --- RenderEffect Custom Modifier for Real Android 12+ Blur ---
fun Modifier.liquidGlassBlur(
    radiusX: Float = 24f,
    radiusY: Float = 24f,
    enabled: Boolean = true
): Modifier = this.then(
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(
                radiusX,
                radiusY,
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        Modifier // Fallback to translucent gradient drawing in component shapes
    }
)

// --- Custom Reusable Glass Card Component ---
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    testTag: String? = null,
    borderGradient: Brush = Brush.linearGradient(
        colors = listOf(GlassColors.GlassBorderLight, Color.Transparent, GlassColors.GlassBorderLight)
    ),
    backgroundGradient: Brush = Brush.verticalGradient(
        colors = listOf(Color(0x1CFFFFFF), Color(0x06FFFFFF))
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    val roundedShape = RoundedCornerShape(22.dp)
    
    Box(
        modifier = modifier
            .testTag(testTag ?: "glass_card")
            .clip(roundedShape)
            .background(backgroundGradient)
            .border(borderWidth, borderGradient, roundedShape)
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

// --- Custom Interactive Glass Button with Ripple & Hover Animation ---
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    accentColor: Color = GlassColors.MintAqua,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val roundedShape = RoundedCornerShape(18.dp)
    
    val bgColors = if (enabled) {
        listOf(accentColor.copy(alpha = 0.28f), Color(0x12FFFFFF))
    } else {
        listOf(Color(0x0AFFFFFF), Color(0x05FFFFFF))
    }
    
    val borderColors = if (enabled) {
        listOf(GlassColors.GlassBorderLight, accentColor.copy(alpha = 0.6f))
    } else {
        listOf(Color(0x10FFFFFF), Color(0x0AFFFFFF))
    }

    Box(
        modifier = modifier
            .testTag(testTag ?: "glass_button")
            .clip(roundedShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor)
            )
            .background(
                Brush.horizontalGradient(bgColors)
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(borderColors),
                roundedShape
            )
            .padding(vertical = 14.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

// --- Custom Beautiful Transparent Text Field ---
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "glass_text_field",
    singleLine: Boolean = false,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        color = Color.White
    )
) {
    val roundedShape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(roundedShape)
            .background(Brush.verticalGradient(listOf(Color(0x0FFFFFFF), Color(0x03FFFFFF))))
            .border(1.dp, Brush.horizontalGradient(listOf(GlassColors.GlassBorderLight, Color.Transparent)), roundedShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Custom Glass Bottom Navigation Bar ---
@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(
        modifier = modifier
            .testTag("glass_bottom_bar")
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x28FFFFFF), Color(0x08FFFFFF))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(GlassColors.GlassBorderLight, Color.Transparent)
                ),
                shape
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            content()
        }
    }
}

// --- Interactive Navigation Item styled in Glass ---
@Composable
fun RowScope.GlassBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    accentColor: Color = GlassColors.MintAqua
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_bubble_scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 32.dp, color = accentColor)
            )
            .graphicsLayer(scaleX = scale, scaleY = scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) accentColor else Color.White.copy(alpha = 0.6f)
            ) {
                icon()
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accentColor else Color.White.copy(alpha = 0.6f)
        )
    }
}

// --- Liquid Glass Beautiful Dialog ---
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            val roundedShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(roundedShape)
                    .background(Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x10FFFFFF))))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(listOf(GlassColors.GlassBorderLight, Color.Transparent)),
                        roundedShape
                    )
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

// --- Liquid Glass Material 3 Expressive Bottom Sheet ---
@Composable
fun GlassBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                onClick = onDismiss,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x0FFFFFFF)))
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(GlassColors.GlassBorderLight, Color.Transparent)),
                    shape
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .clickable(enabled = false) {}
                .padding(vertical = 12.dp, horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // drag handle
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(20.dp))
                content()
            }
        }
    }
}
