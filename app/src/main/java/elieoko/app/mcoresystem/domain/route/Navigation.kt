package elieoko.app.mcoresystem.domain.route

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.ui.pages.home.HomePage


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Navigation(
    navC: NavHostController,
    viewModelGlobal: ApplicationViewModel? = viewModel()
){
    NavHost(navController = navC, startDestination = ScreenRoute.Home.name, route = "root") {
//        composable(ScreenRoute.Login.name) {
//            AuthLogin(navC,viewModelGlobal)
//        }
        composable(ScreenRoute.Home.name) {
            HomePage(navC,viewModelGlobal)
        }
//        composable(ScreenRoute.History.name) {
//            HistoryRecouvrement(navC,onBackEvent={navC.popBackStack()},viewModelGlobal)
//        }
//        composable(ScreenRoute.Detail.name+ "/{id}") { navBackStack ->
//            val recouvementId = navBackStack.arguments?.getString("id")?.toInt()
//            DetailRecouvrement(navC,onBackEvent={navC.popBackStack()},viewModelGlobal,recouvementId)
//        }
//        composable(ScreenRoute.Payment.name) {
//            Paiement(navC,onBackEvent={navC.popBackStack()},viewModelGlobal)
//        }
//        composable(ScreenRoute.PrinterConfig.name,) {
//            PrinterConfig(onBackEvent ={navC.popBackStack()}, viewModelGlobal)
//        }
//        composable(ScreenRoute.PaymentPrinter.name + "/{id}") { navBackStack ->
//            val recouvementId = navBackStack.arguments?.getString("id")?.toInt()
//            PaimentPrinter(onBackEvent={navC.popBackStack()},viewModelGlobal,recouvementId)
//        }
//        composable(ScreenRoute.Profil.name) { navBackStack ->
////            val userId = navBackStack.arguments?.getString("id")?.toInt()
//            Profil(navC,onBackEvent={navC.popBackStack()},viewModelGlobal)
//        }
    }
}