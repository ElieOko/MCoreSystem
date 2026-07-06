package elieoko.app.mcoresystem.presentation.components.element

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.presentation.ui.theme.MCoreSystemTheme
import elieoko.app.mcoresystem.presentation.ui.theme.OrangePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.OrangePrimaryDark
import elieoko.app.mcoresystem.presentation.ui.theme.bagdeColor

@Composable
@Preview(showBackground = true)
fun ImageIconButton(onclick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primaryContainer,
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "color"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(OrangePrimary, OrangePrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AttachMoney,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        ) {
            IconButton(
                onClick = onclick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = bagdeColor),
                modifier = Modifier
                    .size(40.dp)
                    .border(width = 3.dp, color = animatedColor, shape = CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    }
}
