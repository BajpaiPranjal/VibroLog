package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Vibrant glassmorphism palette
val GlassCyan = Color(0xFF00F5D4)
val GlassBlue = Color(0xFF00BBF9)
val GlassPurple = Color(0xFF9D4EDD)
val GlassYellow = Color(0xFFFEE440)
val GlassCoral = Color(0xFFEF476F)
val GlassDarkVoid = Color(0xFF060913)
val GlassNavy = Color(0xFF0D172A)

/**
 * Ambient background drawing glowing nebulas/gradient lights that shine through
 * the translucent frosted glass cards.
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) GlassDarkVoid else Color(0xFFF0F4F9))
    ) {
        // Ambient decorative canvas with glowing orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDark) {
                // Top-right cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassCyan.copy(alpha = 0.16f * pulse), Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.12f),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(width * 0.85f, height * 0.12f)
                )

                // Mid-left purple nebula
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassPurple.copy(alpha = 0.14f * pulse), Color.Transparent),
                        center = Offset(width * 0.1f, height * 0.45f),
                        radius = width * 0.65f
                    ),
                    radius = width * 0.65f,
                    center = Offset(width * 0.1f, height * 0.45f)
                )

                // Bottom-right electric blue orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassBlue.copy(alpha = 0.15f * pulse), Color.Transparent),
                        center = Offset(width * 0.75f, height * 0.85f),
                        radius = width * 0.8f
                    ),
                    radius = width * 0.8f,
                    center = Offset(width * 0.75f, height * 0.85f)
                )
            } else {
                // Light mode subtle tint gradients
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFBAE6FD).copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.1f),
                        radius = width * 0.75f
                    ),
                    radius = width * 0.75f,
                    center = Offset(width * 0.85f, height * 0.1f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE9D5FF).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(width * 0.15f, height * 0.6f),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(width * 0.15f, height * 0.6f)
                )
            }
        }

        content()
    }
}

/**
 * Frosted glass card with translucent specular sheen and gradient border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 0.dp,
    accentGlow: Color? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isDark) {
        Color(0x38121F38)
    } else {
        Color(0xBAFFFFFF)
    }

    val topBorderColor = if (isDark) Color(0x40FFFFFF) else Color(0x90FFFFFF)
    val bottomBorderColor = if (isDark) Color(0x10FFFFFF) else Color(0x30FFFFFF)

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            accentGlow?.copy(alpha = 0.6f) ?: topBorderColor,
            bottomBorderColor
        )
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = accentGlow?.copy(alpha = 0.3f) ?: Color.Black.copy(alpha = 0.2f),
                spotColor = accentGlow?.copy(alpha = 0.4f) ?: Color.Black.copy(alpha = 0.3f)
            ),
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Frosted metric statistic badge showing min, max, avg, rms, etc.
 */
@Composable
fun GlassStatBadge(
    label: String,
    value: String,
    unit: String = "",
    accentColor: Color = GlassCyan,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0x301E2E4A) else Color(0xE0FFFFFF),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.45f), Color(0x15FFFFFF))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = accentColor.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Frosted interactive button with haptic feedback and glowing borders.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    accentColor: Color = GlassCyan,
    enabled: Boolean = true,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val animatedBg by animateColorAsState(
        targetValue = if (enabled) {
            if (isDark) accentColor.copy(alpha = 0.18f) else accentColor.copy(alpha = 0.12f)
        } else {
            Color.Gray.copy(alpha = 0.1f)
        },
        label = "btn_bg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = animatedBg,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = 0.6f), accentColor.copy(alpha = 0.2f))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) (if (isDark) Color.White else Color(0xFF0F172A)) else Color.Gray
            )
        }
    }
}
