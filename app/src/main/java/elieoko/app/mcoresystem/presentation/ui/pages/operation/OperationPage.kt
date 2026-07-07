package elieoko.app.mcoresystem.presentation.ui.pages.operation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.model.OperationStatus
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import kotlinx.coroutines.launch
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

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<OperationStatus?>(null) }
    var statusTarget by remember { mutableStateOf<OperationRelation?>(null) }
    var editTarget by remember { mutableStateOf<OperationModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val operationError by viewModelGlobal?.room?.operation?.error?.collectAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(userId) {
        viewModelGlobal?.room?.operation?.getAllOperation(userId)
        viewModelGlobal?.room?.category?.getAll()
        viewModelGlobal?.room?.currency?.getAllCurrencies()
        viewModelGlobal?.room?.paymentMethod?.getAllPaymentMethod()
    }

    // Recherche instantanée : libellé, catégorie, type, montant, notes, date, statut.
    val filtered = remember(operations, statusFilter, searchQuery) {
        val byStatus =
            if (statusFilter == null) operations
            else operations.filter { OperationStatus.from(it.operation?.status) == statusFilter }
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) byStatus
        else byStatus.filter { relation ->
            val op = relation.operation
            (op?.taskName?.lowercase()?.contains(query) == true) ||
                (op?.description?.lowercase()?.contains(query) == true) ||
                (op?.createdOn?.lowercase()?.contains(query) == true) ||
                (op?.amount?.toString()?.contains(query) == true) ||
                (relation.category?.name?.lowercase()?.contains(query) == true) ||
                (relation.paymentMethod?.name?.lowercase()?.contains(query) == true) ||
                (relation.currency?.code?.lowercase()?.contains(query) == true) ||
                OperationStatus.from(op?.status).label.lowercase().contains(query)
        }
    }

    val addedMsg = stringResource(R.string.operation_added)
    val updatedMsg = stringResource(R.string.operation_updated)
    val deletedMsg = stringResource(R.string.item_deleted)
    val statusMsg = stringResource(R.string.status_updated)
    val genericError = stringResource(R.string.error_generic)

    LaunchedEffect(operationError) {
        if (operationError != null) {
            snackbarHost.showSnackbar(genericError)
            viewModelGlobal?.room?.operation?.consumeError()
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
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_operations)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                singleLine = true
            )
            StatusFilterRow(statusFilter) { statusFilter = it }
            if (filtered.isEmpty()) {
                EmptyState(message = stringResource(R.string.no_data))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                ) {
                    items(filtered, key = { it.operation?.id ?: 0 }) { operation ->
                        OperationListItem(
                            operation = operation,
                            usdToCdfRate = usdToCdfRate,
                            onClick = { navC?.navigate("${ScreenRoute.OperationDetail.name}/${operation.operation?.id}") },
                            onStatusClick = { statusTarget = operation },
                            onEdit = { editTarget = operation.operation },
                            onDelete = {
                                operation.operation?.let { viewModelGlobal?.room?.operation?.delete(it) }
                                viewModelGlobal?.room?.operation?.getAllOperation(userId)
                                viewModelGlobal?.requestSync()
                                scope.launch { snackbarHost.showSnackbar(deletedMsg) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        OperationFormSheet(
            categories = categories,
            currencies = currencies,
            paymentMethods = paymentMethods,
            usdToCdfRate = usdToCdfRate,
            onDismiss = { showSheet = false },
            onCreateCurrency = { showSheet = false; showCurrencySheet = true },
            onSave = { model ->
                viewModelGlobal?.room?.operation?.insert(model)
                showSheet = false
                viewModelGlobal?.room?.operation?.getAllOperation(userId)
                viewModelGlobal?.requestSync()
                scope.launch { snackbarHost.showSnackbar(addedMsg) }
            },
            organismId = organismId,
            userId = userId
        )
    }

    editTarget?.let { existing ->
        OperationFormSheet(
            categories = categories,
            currencies = currencies,
            paymentMethods = paymentMethods,
            usdToCdfRate = usdToCdfRate,
            existing = existing,
            onDismiss = { editTarget = null },
            onCreateCurrency = { editTarget = null; showCurrencySheet = true },
            onSave = { model ->
                viewModelGlobal?.room?.operation?.update(model)
                editTarget = null
                viewModelGlobal?.room?.operation?.getAllOperation(userId)
                viewModelGlobal?.requestSync()
                scope.launch { snackbarHost.showSnackbar(updatedMsg) }
            },
            organismId = organismId,
            userId = userId
        )
    }

    if (showCurrencySheet) {
        AddCurrencySheet(
            onDismiss = { showCurrencySheet = false },
            onSave = { model ->
                viewModelGlobal?.room?.currency?.insert(model)
                showCurrencySheet = false
                showSheet = true
            }
        )
    }

    statusTarget?.let { target ->
        StatusPickerSheet(
            current = OperationStatus.from(target.operation?.status),
            onDismiss = { statusTarget = null },
            onSelect = { status ->
                target.operation?.let { viewModelGlobal?.room?.operation?.updateStatus(it.id, status.name) }
                statusTarget = null
                scope.launch {
                    viewModelGlobal?.room?.operation?.getAllOperation(userId)
                    snackbarHost.showSnackbar(statusMsg)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterRow(selected: OperationStatus?, onSelect: (OperationStatus?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.all)) }
        )
        OperationStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationFormSheet(
    categories: List<elieoko.app.mcoresystem.domain.model.room.CategoryModel>,
    currencies: List<CurrencyModel>,
    paymentMethods: List<elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel>,
    usdToCdfRate: Double,
    organismId: Int,
    userId: Int,
    existing: OperationModel? = null,
    onDismiss: () -> Unit,
    onCreateCurrency: () -> Unit,
    onSave: (OperationModel) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taskName by remember { mutableStateOf(existing?.taskName ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amount by remember { mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var dateInput by remember { mutableStateOf(existing?.createdOn ?: "") }
    var selectedCategoryId by remember { mutableIntStateOf(existing?.categoryId ?: categories.firstOrNull()?.id ?: 0) }
    var selectedCurrencyId by remember { mutableIntStateOf(existing?.currencyId ?: currencies.firstOrNull()?.id ?: 0) }
    var selectedPaymentId by remember { mutableIntStateOf(existing?.paymentMethodId ?: paymentMethods.firstOrNull()?.id ?: 0) }
    var selectedStatus by remember { mutableStateOf(OperationStatus.from(existing?.status)) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(categories) { if (selectedCategoryId == 0) selectedCategoryId = categories.firstOrNull()?.id ?: 0 }
    LaunchedEffect(currencies) { if (selectedCurrencyId == 0) selectedCurrencyId = currencies.firstOrNull()?.id ?: 0 }
    LaunchedEffect(paymentMethods) { if (selectedPaymentId == 0) selectedPaymentId = paymentMethods.firstOrNull()?.id ?: 0 }

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val selectedCurrency = currencies.find { it.id == selectedCurrencyId }
    val conversion = if (amountValue > 0 && selectedCurrency != null)
        CurrencyConverter.conversionLabel(amountValue, selectedCurrency.code, usdToCdfRate) else ""

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (existing == null)
                    "${stringResource(R.string.add)} ${stringResource(R.string.operations)}"
                else stringResource(R.string.edit_operation),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Space(y = 16)
            if (currencies.isEmpty()) {
                AnimatedFeedback(visible = true, message = stringResource(R.string.no_currency_hint), isError = true)
                Space(y = 8)
                MCoreOutlinedButton(text = stringResource(R.string.add_currency), onClick = onCreateCurrency)
                Space(y = 8)
            }
            if (categories.isEmpty()) {
                AnimatedFeedback(visible = true, message = stringResource(R.string.no_category_hint), isError = true)
                Space(y = 8)
            }
            MCoreTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = stringResource(R.string.task_name),
                isError = showError && taskName.isBlank()
            )
            Space(y = 8)
            MCoreTextField(value = description, onValueChange = { description = it }, label = stringResource(R.string.description))
            Space(y = 8)
            MCoreTextField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(R.string.amount),
                keyboardType = KeyboardType.Decimal,
                isError = showError && amountValue <= 0.0
            )
            if (conversion.isNotBlank()) {
                Space(y = 6)
                Text(
                    text = "${stringResource(R.string.conversion_preview)} : $conversion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.animateContentSize()
                )
            }
            if (existing != null) {
                Space(y = 8)
                MCoreTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = stringResource(R.string.date)
                )
            }
            Space(y = 12)
            LabeledDropdown(
                label = stringResource(R.string.category),
                options = categories.map { it.id to it.name },
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )
            Space(y = 8)
            LabeledDropdown(
                label = stringResource(R.string.currency),
                options = currencies.map { it.id to "${it.symbol} (${it.code})" },
                selectedId = selectedCurrencyId,
                onSelect = { selectedCurrencyId = it }
            )
            Space(y = 8)
            LabeledDropdown(
                label = stringResource(R.string.payment_method),
                options = paymentMethods.map { it.id to it.name },
                selectedId = selectedPaymentId,
                onSelect = { selectedPaymentId = it }
            )
            Space(y = 12)
            Text(stringResource(R.string.status), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Space(y = 6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperationStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.label) }
                    )
                }
            }
            Space(y = 12)
            AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
            Space(y = 12)
            MCoreButton(
                text = stringResource(R.string.save),
                enabled = currencies.isNotEmpty(),
                onClick = {
                    if (taskName.isBlank() || amountValue <= 0.0 || selectedCategoryId == 0 || selectedCurrencyId == 0) {
                        showError = true
                        return@MCoreButton
                    }
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    if (existing == null) {
                        onSave(
                            OperationModel(
                                organismId = organismId,
                                categoryId = selectedCategoryId,
                                userId = userId,
                                paymentMethodId = selectedPaymentId,
                                currencyId = selectedCurrencyId,
                                amount = amountValue,
                                taskName = taskName,
                                description = description,
                                createdOn = date,
                                isActive = selectedStatus != OperationStatus.CLOTURE,
                                status = selectedStatus.name
                            )
                        )
                    } else {
                        onSave(
                            existing.copy(
                                categoryId = selectedCategoryId,
                                paymentMethodId = selectedPaymentId,
                                currencyId = selectedCurrencyId,
                                amount = amountValue,
                                taskName = taskName,
                                description = description,
                                createdOn = dateInput.ifBlank { existing.createdOn },
                                isActive = selectedStatus != OperationStatus.CLOTURE,
                                status = selectedStatus.name
                            )
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCurrencySheet(onDismiss: () -> Unit, onSave: (CurrencyModel) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.add_currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Space(y = 16)
            MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.currency_name), isError = showError && name.isBlank())
            Space(y = 8)
            MCoreTextField(value = code, onValueChange = { code = it }, label = stringResource(R.string.currency_code), isError = showError && code.isBlank())
            Space(y = 8)
            MCoreTextField(value = symbol, onValueChange = { symbol = it }, label = stringResource(R.string.currency_symbol), isError = showError && symbol.isBlank())
            Space(y = 12)
            AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
            Space(y = 12)
            MCoreButton(text = stringResource(R.string.save), onClick = {
                if (name.isBlank() || code.isBlank() || symbol.isBlank()) { showError = true; return@MCoreButton }
                onSave(CurrencyModel(name = name, code = code.uppercase(), symbol = symbol))
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusPickerSheet(
    current: OperationStatus,
    onDismiss: () -> Unit,
    onSelect: (OperationStatus) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.change_status), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Space(y = 16)
            OperationStatus.entries.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status)
                    RadioButton(selected = current == status, onClick = { onSelect(status) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    options: List<Pair<Int, String>>,
    selectedId: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.find { it.first == selectedId }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun OperationListItem(
    operation: OperationRelation,
    usdToCdfRate: Double,
    onClick: () -> Unit,
    onStatusClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyCode = operation.currency?.code ?: ExchangeRateRepository.CURRENCY_CDF_CODE
    val amount = operation.operation?.amount ?: 0.0
    val conversion = CurrencyConverter.conversionLabel(amount, currencyCode, usdToCdfRate)
    val status = OperationStatus.from(operation.operation?.status)

    DeleteableListItem(onClick = onClick, onDelete = onDelete) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    operation.operation?.taskName ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(operation.category?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Space(y = 4)
            Box(Modifier.clickable { onStatusClick() }) {
                StatusBadge(status)
            }
            if (conversion.isNotBlank()) {
                Space(y = 2)
                Text(conversion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${operation.currency?.symbol ?: ""} $amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
