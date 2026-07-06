package elieoko.app.mcoresystem.presentation.ui.pages.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
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
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.MCoreButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreOutlinedButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreTextField
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.ui.theme.MCoreSystemTheme
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark

@Composable
fun LoginPage(
    navC: NavHostController? = null,
    viewModelGlobal: ApplicationViewModel? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginResult by viewModelGlobal?.room?.user?.loginResult?.collectAsState() ?: remember { mutableStateOf(null) }
    val loginError by viewModelGlobal?.room?.user?.loginError?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(loginResult) {
        loginResult?.let { user ->
            viewModelGlobal?.currentUserId?.intValue = user.id
            viewModelGlobal?.currentUsername?.value = user.username
            viewModelGlobal?.currentOrganismId?.intValue = user.organismId
            viewModelGlobal?.room?.user?.resetLoginState()
            navC?.navigate(ScreenRoute.Home.name) {
                popUpTo(ScreenRoute.Login.name) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BluePrimary, BluePrimaryDark)
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
                Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Space(y = 16)
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.login_subtitle),
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
                        isError = loginError && username.isBlank()
                    )
                    Space(y = 12)
                    MCoreTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.password),
                        isPassword = true,
                        isError = loginError
                    )
                    if (loginError) {
                        Space(y = 4)
                        Text(
                            text = stringResource(R.string.login_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Space(y = 24)
                    MCoreButton(
                        text = stringResource(R.string.login_button),
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                viewModelGlobal?.room?.user?.login(username, password)
                            }
                        }
                    )
                    Space(y = 12)
                    MCoreOutlinedButton(
                        text = stringResource(R.string.register_button),
                        onClick = { navC?.navigate(ScreenRoute.Register.name) }
                    )
                    Space(y = 8)
                    Text(
                        text = stringResource(R.string.no_account),
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
fun LoginPagePreview() {
    MCoreSystemTheme { LoginPage() }
}
