package elieoko.app.mcoresystem.presentation.ui.pages.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreOutlinedButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreTextField
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.ui.theme.MCoreSystemTheme
import elieoko.app.mcoresystem.presentation.ui.theme.OrangePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.OrangePrimaryDark
import kotlinx.coroutines.launch

@Composable
fun RegisterPage(
    navC: NavHostController? = null,
    viewModelGlobal: ApplicationViewModel? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OrangePrimary, OrangePrimaryDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Space(y = 16)
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Space(y = 32)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(24.dp)) {
                    MCoreTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = stringResource(R.string.username),
                        isError = showError && username.isBlank()
                    )
                    Space(y = 12)
                    MCoreTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = stringResource(R.string.email),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    )
                    Space(y = 12)
                    MCoreTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = stringResource(R.string.phone),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                    Space(y = 12)
                    MCoreTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.password),
                        isPassword = true,
                        isError = showError && password.isBlank()
                    )
                    if (showSuccess) {
                        Space(y = 8)
                        Text(
                            text = stringResource(R.string.register_success),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Space(y = 24)
                    MCoreButton(
                        text = stringResource(R.string.register_button),
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                showError = true
                                return@MCoreButton
                            }
                            showError = false
                            scope.launch {
                                val userId = viewModelGlobal?.room?.user?.getNextUserId() ?: 1
                                val user = UserModel(
                                    id = userId,
                                    username = username,
                                    phone = phone.ifBlank { null },
                                    email = email.ifBlank { null },
                                    password = password,
                                    organismId = 1
                                )
                                viewModelGlobal?.room?.user?.insert(user)
                                showSuccess = true
                            }
                        }
                    )
                    Space(y = 12)
                    MCoreOutlinedButton(
                        text = stringResource(R.string.login_button),
                        onClick = { navC?.navigate(ScreenRoute.Login.name) }
                    )
                    Space(y = 8)
                    Text(
                        text = stringResource(R.string.has_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterPagePreview() {
    MCoreSystemTheme { RegisterPage() }
}
