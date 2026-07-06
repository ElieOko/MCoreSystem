package elieoko.app.mcoresystem.presentation.ui.pages.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.constraintlayout.compose.*
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
@Preview(showBackground = true)
fun HomePage(navC: NavHostController? = null, viewModelGlobal: ApplicationViewModel? = null) {
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val size = ((screenHeightDp.value.toInt() / 2))
    val sizeWidth = ((screenWidthDp.value.toInt() / 2))
    val isShow = remember { mutableStateOf(false) }
    var titleMsg = ""
    var msg = ""
    Scaffold(
      /*  topBar = {
           // if (user.isNotEmpty()){
                TopBarSimple(
                    onclick ={
//                        navC?.navigate(route = ScreenRoute.Profil.name)
                    },
                    onclickLogOut = {
//                        textPositive = "Oui"
//                        textNegative = "Non"
//                        msg = "Voulez-vous vraiment vous déconnectez ?"
//                        titleMsg = "Information"
//                        isShow.value = true
//                        onclick = onLogOutEvent
                    },
                    onclickSync = {},
                    menuItem = emptyList(),
                    username = ""
                )
           // }
        }*/
    ) {
        Column(Modifier.padding(it)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(Modifier.padding(5.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                    ImageIconButton(onclick = {
//                        navC?.navigate(route = ScreenRoute.Payment.name)
                    })
                    Space(y = 10)
                    Text("Appuyez pour effectuer des operations sur +", color = Color.Black, fontSize = 18.sp)
                }
                Space(y = 95)
                ConstraintLayout {
                    // val (card) = createRefs()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth().height((size / 2).dp),
                        shape = RectangleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF25262C)
                        )) {

                    }
                }
            }
        }
    }
}