package elieoko.app.mcoresystem.presentation.ui.pages.category.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*
import kotlinx.coroutines.launch

@Composable
fun TypeCategoryPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val organismId = viewModelGlobal?.currentOrganismId?.intValue ?: 1
    val typeCategories by viewModelGlobal?.room?.typeCategory?.listTypeCategories?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val typeError by viewModelGlobal?.room?.typeCategory?.error?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.item_added)
    val deletedMsg = stringResource(R.string.item_deleted)
    val genericError = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.typeCategory?.getAll()
    }

    LaunchedEffect(typeError) {
        if (typeError != null) {
            snackbarHost.showSnackbar(genericError)
            viewModelGlobal?.room?.typeCategory?.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.type_categories),
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
            SectionHeader(title = stringResource(R.string.type_categories))
            Space(y = 8)
            MCoreCard {
                Column(Modifier.padding(vertical = 4.dp)) {
                    if (typeCategories.isEmpty()) {
                        EmptyState(message = stringResource(R.string.no_data))
                    } else {
                        typeCategories.forEach { type ->
                            ManageableRow(
                                icon = Icons.AutoMirrored.Filled.Label,
                                title = type.name,
                                subtitle = type.description.ifBlank { null },
                                trailing = {
                                    Text(
                                        text = if (type.isActive) stringResource(R.string.active) else stringResource(R.string.inactive),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (type.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onDelete = {
                                    viewModelGlobal?.room?.typeCategory?.delete(type)
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
            title = { Text("${stringResource(R.string.add)} ${stringResource(R.string.type_categories)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name), isError = showError && name.isBlank())
                    MCoreTextField(value = description, onValueChange = { description = it }, label = stringResource(R.string.description))
                    AnimatedFeedback(visible = showError, message = stringResource(R.string.validation_error), isError = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) { showError = true; return@TextButton }
                    viewModelGlobal?.room?.typeCategory?.insert(
                        TypeCategoryModel(organismId = organismId, name = name, description = description)
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
