package elieoko.app.mcoresystem.presentation.ui.pages.operation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationPage(
    navC: NavHostController? = null,
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val userId = viewModelGlobal?.currentUserId?.intValue ?: 1
    val organismId = viewModelGlobal?.currentOrganismId?.intValue ?: 1
    val operations by viewModelGlobal?.room?.operation?.listOperation?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val categories by viewModelGlobal?.room?.category?.listCategories?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val currencies by viewModelGlobal?.room?.currency?.listCurrencies?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val paymentMethods by viewModelGlobal?.room?.paymentMethod?.listPaymentMethod?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }

    var showDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableIntStateOf(0) }
    var selectedCurrencyId by remember { mutableIntStateOf(ExchangeRateRepository.CURRENCY_CDF_ID) }
    var selectedPaymentId by remember { mutableIntStateOf(1) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    val selectedCurrency = currencies.find { it.id == selectedCurrencyId }
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val conversionPreview = if (amountValue > 0 && selectedCurrency != null) {
        CurrencyConverter.conversionLabel(amountValue, selectedCurrency.code, usdToCdfRate)
    } else ""

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.category?.getAll()
        viewModelGlobal?.room?.currency?.getAllCurrencies()
        viewModelGlobal?.room?.paymentMethod?.getAllPaymentMethod()
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == 0) {
            selectedCategoryId = categories.first().id
        }
    }

    LaunchedEffect(currencies) {
        if (currencies.isNotEmpty() && currencies.none { it.id == selectedCurrencyId }) {
            selectedCurrencyId = currencies.first().id
        }
    }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.operations),
                isMain = false,
                onBackEvent = onBackEvent,
                username = viewModelGlobal?.currentUsername?.value
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (operations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(operations, key = { it.operation?.id ?: 0 }) { operation ->
                    OperationListItem(
                        operation = operation,
                        usdToCdfRate = usdToCdfRate,
                        onClick = {
                            navC?.navigate("${ScreenRoute.OperationDetail.name}/${operation.operation?.id}")
                        },
                        onDelete = {
                            operation.operation?.let { viewModelGlobal?.room?.operation?.delete(it) }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("${stringResource(R.string.add)} ${stringResource(R.string.operations)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MCoreTextField(value = taskName, onValueChange = { taskName = it }, label = stringResource(R.string.task_name))
                    MCoreTextField(value = description, onValueChange = { description = it }, label = stringResource(R.string.description))
                    MCoreTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = stringResource(R.string.amount),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                    if (conversionPreview.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.conversion_preview)} : $conversionPreview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    CurrencyDropdown(categories, selectedCategoryId, categoryExpanded, { categoryExpanded = it }, { selectedCategoryId = it }, stringResource(R.string.category), { it.name })
                    CurrencyDropdown(currencies, selectedCurrencyId, currencyExpanded, { currencyExpanded = it }, { selectedCurrencyId = it }, stringResource(R.string.currency), { "${it.symbol} (${it.code})" })
                    CurrencyDropdown(paymentMethods, selectedPaymentId, paymentExpanded, { paymentExpanded = it }, { selectedPaymentId = it }, stringResource(R.string.payment_method), { it.name })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (taskName.isNotBlank() && amount.isNotBlank()) {
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        viewModelGlobal?.room?.operation?.insert(
                            OperationModel(
                                organismId = organismId,
                                categoryId = selectedCategoryId,
                                userId = userId,
                                paymentMethodId = selectedPaymentId,
                                currencyId = selectedCurrencyId,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                taskName = taskName,
                                description = description,
                                createdOn = date
                            )
                        )
                        taskName = ""
                        description = ""
                        amount = ""
                        showDialog = false
                    }
                }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CurrencyDropdown(
    items: List<T>,
    selectedId: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    label: String,
    displayText: (T) -> String
) {
    val getId: (T) -> Int = { item ->
        when (item) {
            is elieoko.app.mcoresystem.domain.model.room.CategoryModel -> item.id
            is elieoko.app.mcoresystem.domain.model.room.CurrencyModel -> item.id
            is elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel -> item.id
            else -> 0
        }
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = items.find { getId(it) == selectedId }?.let(displayText) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(displayText(item)) },
                    onClick = { onSelect(getId(item)); onExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
private fun OperationListItem(
    operation: OperationRelation,
    usdToCdfRate: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyCode = operation.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE
    val amount = operation.operation?.amount ?: 0.0
    val conversion = CurrencyConverter.conversionLabel(amount, currencyCode, usdToCdfRate)

    DeleteableListItem(onClick = onClick, onDelete = onDelete) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(operation.operation?.taskName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(operation.category?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(operation.operation?.createdOn ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (conversion.isNotBlank()) {
                Text(conversion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = "${operation.currency?.symbol ?: ""} $amount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}
