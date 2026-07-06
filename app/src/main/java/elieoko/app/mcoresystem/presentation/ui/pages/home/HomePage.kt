package elieoko.app.mcoresystem.presentation.ui.pages.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.model.MenuItem
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuickAccessItem(
    val title: String,
    val icon: ImageVector,
    val route: ScreenRoute
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
@Preview(showBackground = true)
fun HomePage(navC: NavHostController? = null, viewModelGlobal: ApplicationViewModel? = null) {
    val username = viewModelGlobal?.currentUsername?.value ?: "Utilisateur"
    val userId = viewModelGlobal?.currentUserId?.intValue ?: 1
    val operationsToday by viewModelGlobal?.room?.operation?.listOperationToday?.collectAsState()
        ?: remember { mutableIntStateOf(0) }
    val operations by viewModelGlobal?.room?.operation?.listOperation?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }
    val dateToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val totalCdf = operations.sumOf { op ->
        CurrencyConverter.toCDF(
            op.operation?.amount ?: 0.0,
            op.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE,
            usdToCdfRate
        )
    }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.operation?.getAllOperationToDay(dateToday, ExchangeRateRepository.CURRENCY_CDF_ID, userId)
    }

    val menuItems = listOf(
        MenuItem(1, stringResource(R.string.operations)) { navC?.navigate(ScreenRoute.Operation.name) },
        MenuItem(2, stringResource(R.string.categories)) { navC?.navigate(ScreenRoute.Category.name) },
        MenuItem(3, stringResource(R.string.type_categories)) { navC?.navigate(ScreenRoute.TypeCategory.name) },
        MenuItem(4, stringResource(R.string.reports)) { navC?.navigate(ScreenRoute.Report.name) },
        MenuItem(5, stringResource(R.string.settings)) { navC?.navigate(ScreenRoute.Setting.name) },
    )

    val quickAccessItems = listOf(
        QuickAccessItem(stringResource(R.string.operations), Icons.Default.SwapHoriz, ScreenRoute.Operation),
        QuickAccessItem(stringResource(R.string.categories), Icons.Default.Category, ScreenRoute.Category),
        QuickAccessItem(stringResource(R.string.type_categories), Icons.Default.Label, ScreenRoute.TypeCategory),
        QuickAccessItem(stringResource(R.string.reports), Icons.Default.Assessment, ScreenRoute.Report),
        QuickAccessItem(stringResource(R.string.settings), Icons.Default.Settings, ScreenRoute.Setting),
    )

    Scaffold(
        topBar = {
            TopBarSimple(
                onclick = {},
                onclickLogOut = {
                    navC?.navigate(ScreenRoute.Login.name) {
                        popUpTo(ScreenRoute.Home.name) { inclusive = true }
                    }
                },
                onclickSync = {},
                menuItem = menuItems,
                username = username
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.home_welcome)}, $username",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Space(y = 16)
                ImageIconButton(onclick = {
                    navC?.navigate(ScreenRoute.Operation.name)
                })
                Space(y = 10)
                Text(
                    text = stringResource(R.string.home_operations_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = stringResource(R.string.operations_today),
                            value = "${operationsToday ?: 0}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.total_amount),
                            value = CurrencyConverter.formatCDF(totalCdf),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Space(y = 20)
                    Text(
                        text = stringResource(R.string.quick_access),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Space(y = 12)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(quickAccessItems) { item ->
                            QuickAccessCard(
                                title = item.title,
                                icon = item.icon,
                                onClick = { navC?.navigate(item.route.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Space(y = 4)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun QuickAccessCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(BluePrimary, BluePrimaryDark)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            }
            Space(y = 8)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}
