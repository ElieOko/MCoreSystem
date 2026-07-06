package elieoko.app.mcoresystem.presentation.ui.pages.operation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreCard
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.components.element.StatusBadge
import elieoko.app.mcoresystem.presentation.components.element.TopBarSimple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationDetailPage(
    operationId: Int?,
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val operationDetail by viewModelGlobal?.room?.operation?.operationDetail?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(operationId) {
        operationId?.let { viewModelGlobal?.room?.operation?.getDetailOperation(it) }
    }

    val amount = operationDetail?.operation?.amount ?: 0.0
    val currencyCode = operationDetail?.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE
    val conversion = CurrencyConverter.conversionLabel(amount, currencyCode, usdToCdfRate)
    val status = OperationStatus.from(operationDetail?.operation?.status)
    val statusMsg = stringResource(R.string.status_updated)

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.operation_detail),
                isMain = false,
                onBackEvent = onBackEvent,
                username = viewModelGlobal?.currentUsername?.value
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            MCoreCard {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = operationDetail?.operation?.taskName ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(status)
                    }
                    Space(y = 16)
                    DetailRow(stringResource(R.string.amount), "${operationDetail?.currency?.symbol ?: ""} $amount")
                    if (conversion.isNotBlank()) DetailRow(stringResource(R.string.conversion_preview), conversion)
                    DetailRow(stringResource(R.string.description), operationDetail?.operation?.description ?: "—")
                    DetailRow(stringResource(R.string.category), operationDetail?.category?.name ?: "—")
                    DetailRow(stringResource(R.string.payment_method), operationDetail?.paymentMethod?.name ?: "—")
                    DetailRow(stringResource(R.string.currency), "${operationDetail?.currency?.name ?: ""} (${operationDetail?.currency?.code ?: ""})")
                    DetailRow(stringResource(R.string.user), operationDetail?.user?.username ?: "—")
                    DetailRow(stringResource(R.string.organism), operationDetail?.organism?.name ?: "—")
                    DetailRow(stringResource(R.string.date), operationDetail?.operation?.createdOn ?: "—")
                }
            }
            Space(y = 16)
            Text(
                text = stringResource(R.string.change_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Space(y = 8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationStatus.entries.forEach { s ->
                    FilterChip(
                        selected = status == s,
                        onClick = {
                            val id = operationDetail?.operation?.id
                            val ownerId = operationDetail?.operation?.userId ?: 1
                            val room = viewModelGlobal?.room?.operation
                            if (id != null && room != null) {
                                room.updateStatus(id, s.name)
                                scope.launch {
                                    room.getDetailOperation(id)
                                    room.getAllOperation(ownerId)
                                    snackbarHost.showSnackbar(statusMsg)
                                }
                            }
                        },
                        label = { Text(s.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
}
