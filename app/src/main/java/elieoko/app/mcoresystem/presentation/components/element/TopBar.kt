package elieoko.app.mcoresystem.presentation.components.element

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.domain.model.MenuItem
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.presentation.ui.theme.bagdeColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun TopBarSimple(
    username: String? = "elieoko",
    title : String = "MATOS_GROUP",
    onclickLogOut : ()-> Unit = {},
    onBackEvent : ()-> Unit = {},
    onclickSync : ()-> Unit = {},
    onclick :()-> Unit = {},
    menuItem :List<MenuItem> = emptyList(),
    isMain : Boolean = true
){

    var expanded by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xF788F18A),
        targetValue = Color(0xFFFFFFFF),
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "color"
    )
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(0.8F)),
        navigationIcon = {
            if (!isMain){
                IconButton(onClick = {onBackEvent()}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(24.dp))
                }
            }
        },
        title = { Text(title) },
        actions = {
            if (isMain){
                Space(x = 5)
                IconButton(
                    onClick = onclick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = bagdeColor),
                    modifier = Modifier.size(28.dp).border(width = 10.dp,
                        color = animatedColor,
                        shape = CircleShape
                    )
                ) {
                    Text(username?.get(0)?.uppercaseChar().toString(), color = Color.White)
                }
                Space(x = 13)
                IconButton(onClick = {onclickSync()}) {
                    Icon(painterResource(R.drawable.sync), null, modifier = Modifier.size(24.dp))
                }

                IconButton(onClick = {onclickLogOut()}) {
                    Icon(painterResource(R.drawable.logout), null, modifier = Modifier.size(24.dp))
                }

                Box {
                    IconButton({
                        expanded = !expanded
                    }) {
                        Icon(painterResource(R.drawable.menu), null, modifier = Modifier.size(24.dp))
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