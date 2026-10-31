package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import elieoko.app.mcoresystem.stock.StockViewModel

private object StockRoute {
    const val Home = "stock_home"
    const val Products = "stock_products"
    const val Livreurs = "stock_livreurs"
    const val Movements = "stock_movements"
    const val Orders = "stock_orders"
    const val Evolution = "stock_evolution"
    const val Space = "stock_space"
    const val Delivery = "stock_delivery"
    const val Assign = "stock_assign?livreurId={livreurId}"
    const val Livreur = "stock_livreur/{id}"

    fun assign(livreurId: Long = -1L) = "stock_assign?livreurId=$livreurId"
    fun livreur(id: Long) = "stock_livreur/$id"
}

@Composable
fun StockApp(onLeave: () -> Unit) {
    val viewModel: StockViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val ready by viewModel.ready.collectAsState()
    val session by viewModel.session.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.message.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            !ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chargement…")
            }
            session == null -> StockLoginPage(viewModel, onLeave)
            session?.isLivreur == true -> LivreurGraph(
                viewModel = viewModel,
                livreurId = session?.livreurId ?: 0L,
                onLeave = onLeave,
                onLogout = { viewModel.logout() }
            )
            else -> OwnerGraph(
                viewModel = viewModel,
                onLeave = onLeave,
                onLogout = { viewModel.logout() }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun OwnerGraph(viewModel: StockViewModel, onLeave: () -> Unit, onLogout: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = StockRoute.Home, modifier = Modifier.fillMaxSize()) {
        composable(StockRoute.Home) {
            StockHomePage(
                viewModel = viewModel,
                onLeave = onLeave,
                onOpenProducts = { navController.navigate(StockRoute.Products) },
                onOpenLivreurs = { navController.navigate(StockRoute.Livreurs) },
                onGive = { navController.navigate(StockRoute.assign()) },
                onOpenLivreur = { navController.navigate(StockRoute.livreur(it)) },
                onOpenMovements = { navController.navigate(StockRoute.Movements) },
                onOpenOrders = { navController.navigate(StockRoute.Orders) },
                onOpenEvolution = { navController.navigate(StockRoute.Evolution) },
                onLogout = onLogout
            )
        }
        composable(StockRoute.Products) {
            StockProductsPage(viewModel) { navController.popBackStack() }
        }
        composable(StockRoute.Livreurs) {
            StockLivreursPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenLivreur = { navController.navigate(StockRoute.livreur(it)) }
            )
        }
        composable(StockRoute.Movements) {
            StockMovementsPage(viewModel) { navController.popBackStack() }
        }
        composable(StockRoute.Orders) {
            StockOrdersPage(viewModel) { navController.popBackStack() }
        }
        composable(StockRoute.Evolution) {
            StockEvolutionPage(viewModel) { navController.popBackStack() }
        }
        composable(
            route = StockRoute.Assign,
            arguments = listOf(navArgument("livreurId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { entry ->
            val livreurId = entry.arguments?.getLong("livreurId") ?: -1L
            StockAssignPage(viewModel, livreurId) { navController.popBackStack() }
        }
        composable(
            route = StockRoute.Livreur,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            StockLivreurDetailPage(
                viewModel = viewModel,
                livreurId = id,
                onBack = { navController.popBackStack() },
                onGiveMore = { navController.navigate(StockRoute.assign(id)) }
            )
        }
    }
}

@Composable
private fun LivreurGraph(
    viewModel: StockViewModel,
    livreurId: Long,
    onLeave: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = StockRoute.Space, modifier = Modifier.fillMaxSize()) {
        composable(StockRoute.Space) {
            StockLivreurSpacePage(
                viewModel = viewModel,
                onLeave = onLeave,
                onLogout = onLogout,
                onDeliver = { navController.navigate(StockRoute.Delivery) },
                onHistory = { navController.navigate(StockRoute.Movements) }
            )
        }
        composable(StockRoute.Delivery) {
            StockDeliveryPage(viewModel, livreurId) { navController.popBackStack() }
        }
        composable(StockRoute.Movements) {
            StockMovementsPage(viewModel, onlyCurrentLivreur = true) { navController.popBackStack() }
        }
    }
}
