package elieoko.app.mcoresystem.presentation.ui.pages.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val organismId = viewModelGlobal?.currentOrganismId?.intValue ?: 1
    val categories by viewModelGlobal?.room?.category?.listCategories?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val typeCategories by viewModelGlobal?.room?.typeCategory?.listTypeCategories?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val categoryError by viewModelGlobal?.room?.category?.error?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableIntStateOf(0) }
    var typeExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.item_added)
    val deletedMsg = stringResource(R.string.item_deleted)
    val genericError = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.category?.getAll()
        viewModelGlobal?.room?.typeCategory?.getAll()
    }

    LaunchedEffect(typeCategories) {
        if (typeCategories.isNotEmpty() && selectedTypeId == 0) {
            selectedTypeId = typeCategories.first().id
        }
    }

    LaunchedEffect(categoryError) {
        if (categoryError != null) {
            snackbarHost.showSnackbar(genericError)
            viewModelGlobal?.room?.category?.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.categories),
                isMain = false,
                onBackEvent = onBackEvent,
                username = viewModelGlobal?.currentUsername?.value
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
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
            SectionHeader(title = stringResource(R.string.categories))
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    if (categories.isEmpty()) {
                        EmptyState(message = stringResource(R.string.no_data))
                    } else {
                        categories.forEach { category ->
                            val typeName = typeCategories.find { it.id == category.typeCategoryId }?.name
                            ManageableRow(
                                icon = Icons.Default.Category,
                                title = category.name,
                                subtitle = listOfNotNull(
                                    category.description.ifBlank { null },
                                    typeName
                                ).joinToString(" • ").ifBlank { null },
                                onDelete = {
                                    viewModelGlobal?.room?.category?.delete(category)
                                    scope.launch { snackbarHost.showSnackbar(deletedMsg) }
                                }
                            )
                        }
                    }
                }
            }
            Space(y = 80)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("${stringResource(R.string.add)} ${stringResource(R.string.category)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name), isError = showError && name.isBlank())
                    MCoreTextField(value = description, onValueChange = { description = it }, label = stringResource(R.string.description))
                    AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                        OutlinedTextField(
                            value = typeCategories.find { it.id == selectedTypeId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.type_categories)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            typeCategories.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = { selectedTypeId = type.id; typeExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) { showError = true; return@TextButton }
                    viewModelGlobal?.room?.category?.insert(
                        CategoryModel(organismId = organismId, typeCategoryId = selectedTypeId, name = name, description = description)
                    )
                    name = ""
                    description = ""
                    showError = false
                    showDialog = false
                    viewModelGlobal?.requestSync()
                    scope.launch { snackbarHost.showSnackbar(addedMsg) }
                }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
