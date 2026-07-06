package elieoko.app.mcoresystem.presentation.ui.pages.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.model.OperationStatus
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark
import elieoko.app.mcoresystem.presentation.ui.theme.StatusClosed
import elieoko.app.mcoresystem.presentation.ui.theme.StatusOpen
import elieoko.app.mcoresystem.presentation.ui.theme.StatusPending

@Composable
fun ReportPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val userId = viewModelGlobal?.currentUserId?.intValue ?: 1
    val operations by viewModelGlobal?.room?.operation?.listOperation?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.category?.getAll()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.report_cdf), stringResource(R.string.report_usd))
    val currencyCode = if (selectedTab == 0) ExchangeRateRepository.CURRENCY_CDF_CODE else ExchangeRateRepository.CURRENCY_USD_CODE

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.reports),
                isMain = false,
                onBackEvent = onBackEvent,
                username = viewModelGlobal?.currentUsername?.value
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            AnimatedContent(
                targetState = currencyCode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "report-tab"
            ) { code ->
                CurrencyReport(
                    operations = operations.filter { it.currency?.code == code },
                    currencyCode = code,
                    usdToCdfRate = usdToCdfRate
                )
            }
        }
    }
}

@Composable
private fun CurrencyReport(
    operations: List<OperationRelation>,
    currencyCode: String,
    usdToCdfRate: Double
) {
    val total = operations.sumOf { it.operation?.amount ?: 0.0 }
    val openCount = operations.count { OperationStatus.from(it.operation?.status) == OperationStatus.OUVERT }
    val pendingCount = operations.count { OperationStatus.from(it.operation?.status) == OperationStatus.EN_ATTENTE }
    val closedCount = operations.count { OperationStatus.from(it.operation?.status) == OperationStatus.CLOTURE }
    val formatter: (Double) -> String = if (currencyCode == ExchangeRateRepository.CURRENCY_CDF_CODE)
        CurrencyConverter::formatCDF else CurrencyConverter::formatUSD
    val gradient = if (currencyCode == ExchangeRateRepository.CURRENCY_CDF_CODE)
        listOf(BluePrimary, BluePrimaryDark) else listOf(AccentTeal, BluePrimaryDark)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        BalanceCard(
            label = if (currencyCode == ExchangeRateRepository.CURRENCY_CDF_CODE)
                stringResource(R.string.report_cdf) else stringResource(R.string.report_usd),
            amount = formatter(total),
            subtitle = "${operations.size} ${stringResource(R.string.total_operations).lowercase()}",
            gradient = gradient,
            icon = Icons.Default.Assessment
        )
        Space(y = 12)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.open_operations), "$openCount", Icons.Default.LockOpen, StatusOpen, Modifier.weight(1f))
            StatTile(stringResource(R.string.pending_operations), "$pendingCount", Icons.Default.HourglassBottom, StatusPending, Modifier.weight(1f))
            StatTile(stringResource(R.string.closed_operations), "$closedCount", Icons.Default.CheckCircle, StatusClosed, Modifier.weight(1f))
        }
        Space(y = 20)
        SectionHeader(title = stringResource(R.string.categories))
        Space(y = 8)
        if (operations.isEmpty()) {
            EmptyState(message = stringResource(R.string.no_data))
        } else {
            MCoreCard {
                Column(Modifier.padding(16.dp)) {
                    val byCategory = operations.groupBy { it.category?.name ?: "—" }
                    byCategory.forEach { (name, ops) ->
                        val catTotal = ops.sumOf { it.operation?.amount ?: 0.0 }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("${ops.size} op.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                formatter(catTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
        Space(y = 24)
    }
}
