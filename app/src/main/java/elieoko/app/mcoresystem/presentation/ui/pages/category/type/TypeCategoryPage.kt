package elieoko.app.mcoresystem.presentation.ui.pages.category.type

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

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.item_added)
    val deletedMsg = stringResource(R.string.item_deleted)

    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.typeCategory?.getAll()
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
        if (typeCategories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = stringResource(R.string.no_data))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(typeCategories, key = { it.id }) { type ->
                    TypeCategoryListItem(
                        type = type,
                        onDelete = {
                            viewModelGlobal?.room?.typeCategory?.delete(type)
                            scope.launch { snackbarHost.showSnackbar(deletedMsg) }
                        }
                    )
                }
            }
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
                    scope.launch { snackbarHost.showSnackbar(addedMsg) }
                }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TypeCategoryListItem(type: TypeCategoryModel, onDelete: () -> Unit) {
    DeleteableListItem(onDelete = onDelete) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(type.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(type.description.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AssistChip(
            onClick = {},
            label = { Text(if (type.isActive) stringResource(R.string.active) else stringResource(R.string.inactive)) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (type.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                labelColor = if (type.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
