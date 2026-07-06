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
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.route.ScreenRoute
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

    var showDialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableIntStateOf(0) }
    var selectedCurrencyId by remember { mutableIntStateOf(1) }
    var selectedPaymentId by remember { mutableIntStateOf(1) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

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
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(operations) { operation ->
                    OperationListItem(
                        operation = operation,
                        onClick = {
                            navC?.navigate("${ScreenRoute.OperationDetail.name}/${operation.operation?.id}")
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.add) + " " + stringResource(R.string.operations)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MCoreTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = stringResource(R.string.task_name)
                    )
                    MCoreTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = stringResource(R.string.description)
                    )
                    MCoreTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = stringResource(R.string.amount),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currencies.find { it.id == selectedCurrencyId }?.code ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.currency)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text("${currency.symbol} (${currency.code})") },
                                    onClick = {
                                        selectedCurrencyId = currency.id
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = paymentExpanded,
                        onExpandedChange = { paymentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = paymentMethods.find { it.id == selectedPaymentId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.payment_method)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        selectedPaymentId = method.id
                                        paymentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (taskName.isNotBlank() && amount.isNotBlank()) {
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        val operation = OperationModel(
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
                        viewModelGlobal?.room?.operation?.insert(operation)
                        taskName = ""
                        description = ""
                        amount = ""
                        showDialog = false
                    }
                }) {
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun OperationListItem(operation: OperationRelation, onClick: () -> Unit) {
    MCoreCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = operation.operation?.taskName ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = operation.category?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = operation.operation?.createdOn ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${operation.currency?.symbol ?: ""} ${operation.operation?.amount ?: 0.0}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
