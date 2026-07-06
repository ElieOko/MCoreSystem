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
    val dateToday = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.operation?.getAllOperationToDay(dateToday, 1, userId)
        viewModelGlobal?.room?.category?.getAll()
    }

    val totalAmount = operations.sumOf { it.operation?.amount ?: 0.0 }
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
                    title = stringResource(R.string.total_amount),
                    value = String.format(Locale.getDefault(), "%.2f", totalAmount),
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    title = stringResource(R.string.active),
                    value = "$activeOperations",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
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
                            val count = operations.count { it.category?.id == category.id }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$count op.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                        text = stringResource(R.string.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Space(y = 8)
                    Text(
                        text = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(Date()),
                        style = MaterialTheme.typography.bodyLarge
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
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
