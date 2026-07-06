package elieoko.app.mcoresystem.presentation.ui.pages.operation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreCard
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.components.element.TopBarSimple

@Composable
fun OperationDetailPage(
    operationId: Int?,
    viewModelGlobal: ApplicationViewModel? = null,
    onBackEvent: () -> Unit = {}
) {
    val operationDetail by viewModelGlobal?.room?.operation?.operationDetail?.collectAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(operationId) {
        operationId?.let { viewModelGlobal?.room?.operation?.getDetailOperation(it) }
    }

    Scaffold(
        topBar = {
            TopBarSimple(
                title = stringResource(R.string.operation_detail),
                isMain = false,
                onBackEvent = onBackEvent,
                username = viewModelGlobal?.currentUsername?.value
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
            MCoreCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = operationDetail?.operation?.taskName ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Space(y = 16)
                    DetailRow(stringResource(R.string.amount), "${operationDetail?.currency?.symbol ?: ""} ${operationDetail?.operation?.amount ?: 0.0}")
                    DetailRow(stringResource(R.string.description), operationDetail?.operation?.description ?: "—")
                    DetailRow(stringResource(R.string.category), operationDetail?.category?.name ?: "—")
                    DetailRow(stringResource(R.string.payment_method), operationDetail?.paymentMethod?.name ?: "—")
                    DetailRow(stringResource(R.string.currency), "${operationDetail?.currency?.name ?: ""} (${operationDetail?.currency?.code ?: ""})")
                    DetailRow(stringResource(R.string.user), operationDetail?.user?.username ?: "—")
                    DetailRow(stringResource(R.string.organism), operationDetail?.organism?.name ?: "—")
                    DetailRow(stringResource(R.string.date), operationDetail?.operation?.createdOn ?: "—")
                    DetailRow(
                        stringResource(R.string.active),
                        if (operationDetail?.operation?.isActive == true) stringResource(R.string.active) else stringResource(R.string.inactive)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}
