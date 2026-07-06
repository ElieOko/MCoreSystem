package elieoko.app.mcoresystem.presentation.ui.pages.category

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
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.*

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

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableIntStateOf(0) }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.category?.getAll()
        viewModelGlobal?.room?.typeCategory?.getAll()
    }

    LaunchedEffect(typeCategories) {
        if (typeCategories.isNotEmpty() && selectedTypeId == 0) {
            selectedTypeId = typeCategories.first().id
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
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(categories) { category ->
                    CategoryListItem(category, typeCategories)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("${stringResource(R.string.add)} ${stringResource(R.string.category)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MCoreTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name))
                    MCoreTextField(value = description, onValueChange = { description = it }, label = stringResource(R.string.description))
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
                    if (name.isNotBlank()) {
                        viewModelGlobal?.room?.category?.insert(
                            CategoryModel(organismId = organismId, typeCategoryId = selectedTypeId, name = name, description = description)
                        )
                        name = ""
                        description = ""
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

@Composable
private fun CategoryListItem(category: CategoryModel, typeCategories: List<elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel>) {
    val typeName = typeCategories.find { it.id == category.typeCategoryId }?.name ?: "—"
    MCoreCard {
        Column(Modifier.padding(16.dp)) {
            Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = category.description.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Space(y = 4)
            AssistChip(
                onClick = {},
                label = { Text(typeName) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
