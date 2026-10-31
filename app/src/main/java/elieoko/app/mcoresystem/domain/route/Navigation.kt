package elieoko.app.mcoresystem.domain.route

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.ui.pages.auth.LoginPage
import elieoko.app.mcoresystem.presentation.ui.pages.auth.RegisterPage
import elieoko.app.mcoresystem.presentation.ui.pages.category.CategoryPage
import elieoko.app.mcoresystem.presentation.ui.pages.category.type.TypeCategoryPage
import elieoko.app.mcoresystem.presentation.ui.pages.home.HomePage
import elieoko.app.mcoresystem.presentation.ui.pages.launcher.ModuleLauncherPage
import elieoko.app.mcoresystem.stock.StockAccess
import elieoko.app.mcoresystem.stock.ui.StockApp
import elieoko.app.mcoresystem.presentation.ui.pages.operation.OperationPage
import elieoko.app.mcoresystem.presentation.ui.pages.operation.detail.OperationDetailPage
import elieoko.app.mcoresystem.presentation.ui.pages.report.ReportPage
import elieoko.app.mcoresystem.presentation.ui.pages.setting.SettingPage

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Navigation(
    navC: NavHostController,
    viewModelGlobal: ApplicationViewModel? = viewModel(),
    startDestination: String = ScreenRoute.Login.name
) {
    NavHost(navController = navC, startDestination = startDestination, route = "root") {
        composable(ScreenRoute.Login.name) {
            LoginPage(navC, viewModelGlobal)
        }
        composable(ScreenRoute.Register.name) {
            RegisterPage(navC, viewModelGlobal)
        }
        composable(ScreenRoute.Portal.name) {
            val role = viewModelGlobal?.currentRole?.value.orEmpty()
            ModuleLauncherPage(
                organismName = viewModelGlobal?.currentOrganismName?.value.orEmpty(),
                username = viewModelGlobal?.currentUsername?.value.orEmpty(),
                showFinance = !role.equals("LIVREUR", ignoreCase = true),
                onOpenFinance = { navC.navigate(ScreenRoute.Home.name) },
                onOpenStock = { navC.navigate(ScreenRoute.Stock.name) },
                onLogout = {
                    viewModelGlobal?.logout {
                        navC.navigate(ScreenRoute.Login.name) {
                            popUpTo(navC.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(ScreenRoute.Stock.name) {
            val access = StockAccess(
                organismUuid = viewModelGlobal?.currentOrganismUuid?.value.orEmpty(),
                organismName = viewModelGlobal?.currentOrganismName?.value.orEmpty(),
                financeOrganismId = viewModelGlobal?.currentOrganismId?.intValue ?: 0,
                userUuid = viewModelGlobal?.currentUserUuid?.value.orEmpty(),
                username = viewModelGlobal?.currentUsername?.value.orEmpty(),
                role = viewModelGlobal?.currentRole?.value.orEmpty()
            )
            StockApp(
                access = access,
                onLeave = { navC.popBackStack() },
                onLogout = {
                    viewModelGlobal?.logout {
                        navC.navigate(ScreenRoute.Login.name) {
                            popUpTo(navC.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(ScreenRoute.Home.name) {
            HomePage(navC, viewModelGlobal)
        }
        composable(ScreenRoute.Operation.name) {
            OperationPage(
                navC = navC,
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
        composable(
            route = "${ScreenRoute.OperationDetail.name}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { navBackStack ->
            val operationId = navBackStack.arguments?.getInt("id")
            OperationDetailPage(
                operationId = operationId,
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
        composable(ScreenRoute.Category.name) {
            CategoryPage(
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
        composable(ScreenRoute.TypeCategory.name) {
            TypeCategoryPage(
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
        composable(ScreenRoute.Report.name) {
            ReportPage(
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
        composable(ScreenRoute.Setting.name) {
            SettingPage(
                viewModelGlobal = viewModelGlobal,
                onBackEvent = { navC.popBackStack() }
            )
        }
    }
}
