package elieoko.app.mcoresystem.presentation.ui.pages.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreCard
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.components.element.TopBarSimple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val userId = viewModelGlobal?.currentUserId?.intValue ?: 1
    val operations by viewModelGlobal?.room?.operation?.listOperation?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val operationsToday by viewModelGlobal?.room?.operation?.listOperationToday?.collectAsState()
        ?: remember { mutableIntStateOf(0) }
    val categories by viewModelGlobal?.room?.category?.listCategories?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }
    val dateToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.operation?.getAllOperationToDay(dateToday, ExchangeRateRepository.CURRENCY_CDF_ID, userId)
        viewModelGlobal?.room?.category?.getAll()
    }

    val totalCdf = operations.sumOf { op ->
        CurrencyConverter.toCDF(
            op.operation?.amount ?: 0.0,
            op.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE,
            usdToCdfRate
        )
    }
    val totalUsd = CurrencyConverter.toUSD(totalCdf, ExchangeRateRepository.CURRENCY_CDF_CODE, usdToCdfRate)
    val activeOperations = operations.count { it.operation?.isActive == true }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.report_summary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Space(y = 16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReportStatCard(
                    title = stringResource(R.string.operations_today),
                    value = "${operationsToday ?: 0}",
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    title = stringResource(R.string.operations),
                    value = "${operations.size}",
                    icon = Icons.Default.SwapHoriz,
                    modifier = Modifier.weight(1f)
                )
            }
            Space(y = 12)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReportStatCard(
                    title = stringResource(R.string.currency_cdf),
                    value = CurrencyConverter.formatCDF(totalCdf),
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    title = stringResource(R.string.currency_usd),
                    value = CurrencyConverter.formatUSD(totalUsd),
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
            }
            Space(y = 12)
            ReportStatCard(
                title = stringResource(R.string.active),
                value = "$activeOperations",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.fillMaxWidth()
            )
            Space(y = 20)
            MCoreCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.categories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Space(y = 12)
                    if (categories.isEmpty()) {
                        Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        categories.forEach { category ->
                            val categoryOps = operations.filter { it.category?.id == category.id }
                            val categoryTotal = categoryOps.sumOf { op ->
                                CurrencyConverter.toCDF(
                                    op.operation?.amount ?: 0.0,
                                    op.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE,
                                    usdToCdfRate
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category.name, style = MaterialTheme.typography.bodyMedium)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${categoryOps.size} op.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        CurrencyConverter.formatCDF(categoryTotal),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
            Space(y = 16)
            MCoreCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.exchange_rate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Space(y = 8)
                    Text(
                        text = "1 USD = ${CurrencyConverter.formatCDF(usdToCdfRate)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Space(y = 8)
                    Text(
                        text = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Space(y = 8)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
