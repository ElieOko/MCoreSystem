package elieoko.app.mcoresystem.presentation.ui.pages.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.model.MenuItem
import elieoko.app.mcoresystem.domain.model.OperationStatus
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.presentation.ui.theme.AccentViolet
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark
import elieoko.app.mcoresystem.presentation.ui.theme.StatusClosed
import elieoko.app.mcoresystem.presentation.ui.theme.StatusPending
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuickAccessItem(
    val title: String,
    val icon: ImageVector,
    val route: ScreenRoute,
    val accent: Color
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomePage(navC: NavHostController? = null, viewModelGlobal: ApplicationViewModel? = null) {
    val username = viewModelGlobal?.currentUsername?.value ?: "Utilisateur"
    val userId = viewModelGlobal?.currentUserId?.intValue ?: 1
    val operations by viewModelGlobal?.room?.operation?.listOperation?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }
    val dateToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
    }

    val totalCdf = operations.filter { it.currency?.code == ExchangeRateRepository.CURRENCY_CDF_CODE }
        .sumOf { it.operation?.amount ?: 0.0 }
    val totalUsd = operations.filter { it.currency?.code == ExchangeRateRepository.CURRENCY_USD_CODE }
        .sumOf { it.operation?.amount ?: 0.0 }
    val globalCdf = operations.sumOf {
        CurrencyConverter.toCDF(it.operation?.amount ?: 0.0, it.currency?.code ?: "CDF", usdToCdfRate)
    }
    val pendingCount = operations.count { OperationStatus.from(it.operation?.status) != OperationStatus.CLOTURE }
    val closedCount = operations.count { OperationStatus.from(it.operation?.status) == OperationStatus.CLOTURE }
    val todayCount = operations.count { it.operation?.createdOn?.startsWith(dateToday) == true }

    val menuItems = listOf(
        MenuItem(1, stringResource(R.string.operations)) { navC?.navigate(ScreenRoute.Operation.name) },
        MenuItem(2, stringResource(R.string.categories)) { navC?.navigate(ScreenRoute.Category.name) },
        MenuItem(3, stringResource(R.string.type_categories)) { navC?.navigate(ScreenRoute.TypeCategory.name) },
        MenuItem(4, stringResource(R.string.reports)) { navC?.navigate(ScreenRoute.Report.name) },
        MenuItem(5, stringResource(R.string.settings)) { navC?.navigate(ScreenRoute.Setting.name) },
    )

    val quickAccessItems = listOf(
        QuickAccessItem(stringResource(R.string.operations), Icons.Default.SwapHoriz, ScreenRoute.Operation, BluePrimary),
        QuickAccessItem(stringResource(R.string.reports), Icons.Default.Assessment, ScreenRoute.Report, AccentTeal),
        QuickAccessItem(stringResource(R.string.categories), Icons.Default.Category, ScreenRoute.Category, AccentViolet),
        QuickAccessItem(stringResource(R.string.type_categories), Icons.Default.Label, ScreenRoute.TypeCategory, StatusPending),
        QuickAccessItem(stringResource(R.string.settings), Icons.Default.Settings, ScreenRoute.Setting, StatusClosed),
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopBarSimple(
                onclick = { navC?.navigate(ScreenRoute.Setting.name) },
                onclickLogOut = {
                    navC?.navigate(ScreenRoute.Login.name) {
                        popUpTo(ScreenRoute.Home.name) { inclusive = true }
                    }
                },
                onclickSync = { viewModelGlobal?.room?.operation?.getAllOperation(userId) },
                menuItem = menuItems,
                username = username
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navC?.navigate(ScreenRoute.Operation.name) },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.operations)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })
        ) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Space(y = 8)
                Text(
                    text = "${stringResource(R.string.home_welcome)},",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Space(y = 16)
                BalanceCard(
                    label = stringResource(R.string.net_balance),
                    amount = CurrencyConverter.formatCDF(globalCdf),
                    subtitle = "${CurrencyConverter.formatUSD(totalUsd)} • ${CurrencyConverter.formatCDF(totalCdf)}",
                    icon = Icons.Default.AccountBalanceWallet
                )
                Space(y = 12)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        title = stringResource(R.string.operations_today),
                        value = "$todayCount",
                        icon = Icons.Default.Today,
                        accent = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = stringResource(R.string.pending_operations),
                        value = "$pendingCount",
                        icon = Icons.Default.HourglassBottom,
                        accent = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = stringResource(R.string.closed_operations),
                        value = "$closedCount",
                        icon = Icons.Default.CheckCircle,
                        accent = StatusClosed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Space(y = 20)
                SectionHeader(title = stringResource(R.string.quick_access))
                Space(y = 12)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(quickAccessItems) { item ->
                        QuickAccessCard(item.title, item.icon, item.accent) { navC?.navigate(item.route.name) }
                    }
                }
                Space(y = 90)
            }
        }
    }
}

@Composable
private fun QuickAccessCard(title: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f))),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Space(y = 8)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}
