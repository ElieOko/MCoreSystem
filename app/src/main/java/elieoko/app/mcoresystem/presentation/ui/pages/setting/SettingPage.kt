package elieoko.app.mcoresystem.presentation.ui.pages.setting

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import kotlinx.coroutines.launch

@Composable
fun SettingPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val currencies by viewModelGlobal?.room?.currency?.listCurrencies?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val paymentMethods by viewModelGlobal?.room?.paymentMethod?.listPaymentMethod?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }
    val username = viewModelGlobal?.currentUsername?.value ?: "—"

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var rateInput by remember(usdToCdfRate) { mutableStateOf(usdToCdfRate.toLong().toString()) }
    var rateSaved by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(rateInput) { rateSaved = false }
    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.currency?.getAllCurrencies()
        viewModelGlobal?.room?.paymentMethod?.getAllPaymentMethod()
    }

    val addedMsg = stringResource(R.string.item_added)
    val deletedMsg = stringResource(R.string.item_deleted)

    val currencyError by viewModelGlobal?.room?.currency?.error?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val paymentError by viewModelGlobal?.room?.paymentMethod?.error?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val genericError = stringResource(R.string.error_generic)
    LaunchedEffect(currencyError, paymentError) {
        if (currencyError != null || paymentError != null) {
            snackbarHost.showSnackbar(genericError)
            viewModelGlobal?.room?.currency?.consumeError()
            viewModelGlobal?.room?.paymentMethod?.consumeError()
        }
    }

    val syncStatus by viewModelGlobal?.syncStatus?.collectAsState()
        ?: remember { mutableStateOf(elieoko.app.mcoresystem.data.remote.SyncStatus.OFFLINE_ONLY) }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.settings),
                isMain = false,
                onBackEvent = onBackEvent,
                username = username
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
            SectionHeader(title = stringResource(R.string.exchange_rate))
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.exchange_rate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Space(y = 12)
                    MCoreTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = stringResource(R.string.exchange_rate_cdf),
                        keyboardType = KeyboardType.Decimal
                    )
                    Space(y = 8)
                    Text(
                        text = "1 USD = ${CurrencyConverter.formatCDF(rateInput.toDoubleOrNull() ?: usdToCdfRate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Space(y = 12)
                    AnimatedFeedback(visible = rateSaved, message = stringResource(R.string.rate_saved))
                    Space(y = 8)
                    MCoreButton(text = stringResource(R.string.save_rate), onClick = {
                        val rate = rateInput.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            viewModelGlobal?.setUsdToCdfRate(rate)
                            rateSaved = true
                        }
                    })
                }
            }
            Space(y = 20)
            SectionHeader(title = stringResource(R.string.settings_currencies)) {
                TextButton(onClick = { showCurrencyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Space(x = 4)
                    Text(stringResource(R.string.add))
                }
            }
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    if (currencies.isEmpty()) {
                        Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } else {
                        currencies.forEach { currency ->
                            ManageableRow(
                                icon = Icons.Default.CurrencyExchange,
                                title = currency.name,
                                subtitle = "${currency.symbol} (${currency.code})",
                                onDelete = {
                                    viewModelGlobal?.room?.currency?.delete(currency)
                                    scope.launch { snackbarHost.showSnackbar(deletedMsg) }
                                }
                            )
                        }
                    }
                }
            }
            Space(y = 20)
            SectionHeader(title = stringResource(R.string.settings_payment_methods)) {
                TextButton(onClick = { showPaymentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Space(x = 4)
                    Text(stringResource(R.string.add))
                }
            }
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    if (paymentMethods.isEmpty()) {
                        Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } else {
                        paymentMethods.forEach { method ->
                            ManageableRow(
                                icon = Icons.Default.Payment,
                                title = method.name,
                                subtitle = null,
                                onDelete = {
                                    viewModelGlobal?.room?.paymentMethod?.delete(method)
                                    scope.launch { snackbarHost.showSnackbar(deletedMsg) }
                                }
                            )
                        }
                    }
                }
            }
            Space(y = 20)
            SectionHeader(title = stringResource(R.string.settings_general))
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    InfoRow(Icons.Default.Person, stringResource(R.string.user), username)
                    InfoRow(
                        Icons.Default.Business,
                        stringResource(R.string.organism),
                        viewModelGlobal?.currentOrganismName?.value?.ifBlank { "—" } ?: "—"
                    )
                    val syncLabel = when (syncStatus) {
                        elieoko.app.mcoresystem.data.remote.SyncStatus.SYNCING -> stringResource(R.string.sync_status_syncing)
                        elieoko.app.mcoresystem.data.remote.SyncStatus.SUCCESS -> stringResource(R.string.sync_status_success)
                        elieoko.app.mcoresystem.data.remote.SyncStatus.ERROR -> stringResource(R.string.sync_status_error)
                        else -> stringResource(R.string.sync_status_offline)
                    }
                    ManageableRow(
                        icon = Icons.Default.CloudSync,
                        title = stringResource(R.string.sync_now),
                        subtitle = syncLabel,
                        onClick = { viewModelGlobal?.requestSync() }
                    )
                    InfoRow(Icons.Default.Info, "MCoreSystem", stringResource(R.string.app_version))
                }
            }
            Space(y = 24)
        }
    }

    if (showCurrencyDialog) {
        CurrencyDialog(
            onDismiss = { showCurrencyDialog = false },
            onSave = {
                viewModelGlobal?.room?.currency?.insert(it)
                showCurrencyDialog = false
                scope.launch { snackbarHost.showSnackbar(addedMsg) }
            }
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onSave = {
                viewModelGlobal?.room?.paymentMethod?.insert(it)
                showPaymentDialog = false
                scope.launch { snackbarHost.showSnackbar(addedMsg) }
            }
        )
    }
}

@Composable
private fun CurrencyDialog(onDismiss: () -> Unit, onSave: (CurrencyModel) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_currency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.currency_name), isError = showError && name.isBlank())
                MCoreTextField(value = code, onValueChange = { code = it }, label = stringResource(R.string.currency_code), isError = showError && code.isBlank())
                MCoreTextField(value = symbol, onValueChange = { symbol = it }, label = stringResource(R.string.currency_symbol), isError = showError && symbol.isBlank())
                AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || code.isBlank() || symbol.isBlank()) { showError = true; return@TextButton }
                onSave(CurrencyModel(name = name, code = code.uppercase(), symbol = symbol))
            }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PaymentDialog(onDismiss: () -> Unit, onSave: (PaymentMethodModel) -> Unit) {
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_payment_method)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name), isError = showError && name.isBlank())
                AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { showError = true; return@TextButton }
                onSave(PaymentMethodModel(name = name))
            }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    ManageableRow(icon = icon, title = label, subtitle = value)
}
