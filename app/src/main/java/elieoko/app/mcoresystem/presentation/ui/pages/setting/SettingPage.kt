package elieoko.app.mcoresystem.presentation.ui.pages.setting

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

@Composable
fun SettingPage(
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val currencies by viewModelGlobal?.room?.currency?.listCurrencies?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val paymentMethods by viewModelGlobal?.room?.paymentMethod?.listPaymentMethod?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val username = viewModelGlobal?.currentUsername?.value ?: "—"

    LaunchedEffect(Unit) {
        viewModelGlobal?.room?.currency?.getAllCurrencies()
        viewModelGlobal?.room?.paymentMethod?.getAllPaymentMethod()
    }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.settings),
                isMain = false,
                onBackEvent = onBackEvent,
                username = username
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
            SettingSection(title = stringResource(R.string.settings_general)) {
                SettingInfoRow(Icons.Default.Person, stringResource(R.string.user), username)
                SettingInfoRow(Icons.Default.Business, stringResource(R.string.organism), "Organisme #${viewModelGlobal?.currentOrganismId?.intValue ?: 1}")
            }
            Space(y = 16)
            SettingSection(title = stringResource(R.string.settings_currencies)) {
                if (currencies.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                } else {
                    currencies.forEach { currency ->
                        SettingInfoRow(
                            Icons.Default.CurrencyExchange,
                            currency.name,
                            "${currency.symbol} (${currency.code})"
                        )
                    }
                }
            }
            Space(y = 16)
            SettingSection(title = stringResource(R.string.settings_payment_methods)) {
                if (paymentMethods.isEmpty()) {
                    Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                } else {
                    paymentMethods.forEach { method ->
                        SettingInfoRow(Icons.Default.Payment, method.name, "—")
                    }
                }
            }
            Space(y = 16)
            SettingSection(title = stringResource(R.string.settings_about)) {
                SettingInfoRow(Icons.Default.Info, "MCoreSystem", stringResource(R.string.app_version))
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Space(y = 8)
    MCoreCard {
        Column(Modifier.padding(4.dp), content = content)
    }
}

@Composable
private fun SettingInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Space(x = 16)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}
