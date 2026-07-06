package elieoko.app.mcoresystem.presentation.components.element

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.domain.model.MenuItem
import elieoko.app.mcoresystem.presentation.ui.theme.MCoreSystemTheme
import elieoko.app.mcoresystem.presentation.ui.theme.bagdeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun TopBarSimple(
    username: String? = "elieoko",
    title: String = "MCoreSystem",
    onclickLogOut: () -> Unit = {},
    onBackEvent: () -> Unit = {},
    onclickSync: () -> Unit = {},
    onclick: () -> Unit = {},
    menuItem: List<MenuItem> = emptyList(),
    isMain: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primaryContainer,
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "color"
    )
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            if (!isMain) {
                IconButton(onClick = { onBackEvent() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = {
            if (isMain) {
                Space(x = 5)
                IconButton(
                    onClick = onclick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = bagdeColor),
                    modifier = Modifier
                        .size(28.dp)
                        .border(width = 2.dp, color = animatedColor, shape = CircleShape)
                ) {
                    Text(username?.get(0)?.uppercaseChar().toString(), color = Color.White)
                }
                Space(x = 8)
                IconButton(onClick = { onclickSync() }) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { onclickLogOut() }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Box {
                    IconButton({ expanded = !expanded }) {
                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        menuItem.forEach { menu ->
                            DropdownMenuItem(
                                text = { Text(menu.name) },
                                onClick = {
                                    expanded = false
                                    menu.eventClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
