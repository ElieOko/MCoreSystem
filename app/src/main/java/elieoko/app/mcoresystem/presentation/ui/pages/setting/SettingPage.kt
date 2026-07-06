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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.util.CurrencyConverter
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreCard
import elieoko.app.mcoresystem.presentation.components.element.MCoreTextField
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
    val usdToCdfRate by viewModelGlobal?.usdToCdfRate?.collectAsState()
        ?: remember { mutableDoubleStateOf(ExchangeRateRepository.DEFAULT_RATE) }
    val username = viewModelGlobal?.currentUsername?.value ?: "—"

    var rateInput by remember(usdToCdfRate) { mutableStateOf(usdToCdfRate.toLong().toString()) }
    var rateSaved by remember { mutableStateOf(false) }

    LaunchedEffect(rateInput) { rateSaved = false }

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
            SettingSection(title = stringResource(R.string.exchange_rate)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.exchange_rate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Space(y = 12)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.exchange_rate_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Space(x = 8)
                        MCoreTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = stringResource(R.string.exchange_rate_cdf),
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Space(y = 8)
                    Text(
                        text = "1 USD = ${CurrencyConverter.formatCDF(rateInput.toDoubleOrNull() ?: usdToCdfRate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "1 FC = ${CurrencyConverter.formatUSD(CurrencyConverter.toUSD(1.0, ExchangeRateRepository.CURRENCY_CDF_CODE, rateInput.toDoubleOrNull() ?: usdToCdfRate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Space(y = 12)
                    MCoreButton(
                        text = stringResource(R.string.save_rate),
                        onClick = {
                            val rate = rateInput.toDoubleOrNull()
                            if (rate != null && rate > 0) {
                                viewModelGlobal?.setUsdToCdfRate(rate)
                                rateSaved = true
                            }
                        }
                    )
                    if (rateSaved) {
                        Space(y = 4)
                        Text(
                            text = stringResource(R.string.rate_saved),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Space(y = 16)
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
                        val convertedExample = when (currency.code) {
                            ExchangeRateRepository.CURRENCY_USD_CODE -> "100 USD = ${CurrencyConverter.formatCDF(100 * usdToCdfRate)}"
                            ExchangeRateRepository.CURRENCY_CDF_CODE -> "100 000 FC = ${CurrencyConverter.formatUSD(CurrencyConverter.toUSD(100000.0, currency.code, usdToCdfRate))}"
                            else -> "${currency.symbol} (${currency.code})"
                        }
                        SettingInfoRow(Icons.Default.CurrencyExchange, currency.name, convertedExample)
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
    MCoreCard { Column(Modifier.padding(4.dp), content = content) }
}

@Composable
private fun SettingInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
