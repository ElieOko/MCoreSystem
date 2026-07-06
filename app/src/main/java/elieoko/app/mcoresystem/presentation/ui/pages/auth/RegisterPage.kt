package elieoko.app.mcoresystem.presentation.ui.pages.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import elieoko.app.mcoresystem.R
import elieoko.app.mcoresystem.domain.repository.AuthResult
import elieoko.app.mcoresystem.domain.route.ScreenRoute
import elieoko.app.mcoresystem.domain.viewmodel.config.ApplicationViewModel
import elieoko.app.mcoresystem.presentation.components.element.AnimatedFeedback
import elieoko.app.mcoresystem.presentation.components.element.MCoreButton
import elieoko.app.mcoresystem.presentation.components.element.MCoreTextField
import elieoko.app.mcoresystem.presentation.components.element.Space
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark

@Composable
fun RegisterPage(
    navC: NavHostController? = null,
    viewModelGlobal: ApplicationViewModel? = null
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var organizationName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authResult by viewModelGlobal?.authResult?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoading by viewModelGlobal?.isAuthLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                viewModelGlobal?.applySession(result.session)
                viewModelGlobal?.consumeAuthResult()
                navC?.navigate(ScreenRoute.Home.name) {
                    popUpTo(ScreenRoute.Login.name) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
                errorMessage = result.message
                showError = true
                viewModelGlobal?.consumeAuthResult()
            }
            null -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BluePrimary, BluePrimaryDark)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500), initialOffsetY = { -it / 4 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = Color.White
                        )
                    }
                    Space(y = 12)
                    Text(
                        text = stringResource(R.string.register_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.register_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Space(y = 24)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700), initialOffsetY = { it / 4 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Space(x = 8)
                            Text(
                                text = stringResource(R.string.organization_section),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Space(y = 8)
                        MCoreTextField(
                            value = organizationName,
                            onValueChange = { organizationName = it; showError = false },
                            label = stringResource(R.string.organization_name),
                            isError = showError && organizationName.isBlank(),
                            supportingText = stringResource(R.string.organization_hint)
                        )
                        Space(y = 16)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Space(y = 16)
                        MCoreTextField(
                            value = username,
                            onValueChange = { username = it; showError = false },
                            label = stringResource(R.string.username),
                            isError = showError && username.isBlank()
                        )
                        Space(y = 12)
                        MCoreTextField(
                            value = email,
                            onValueChange = { email = it; showError = false },
                            label = stringResource(R.string.email),
                            keyboardType = KeyboardType.Email
                        )
                        Space(y = 12)
                        MCoreTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = stringResource(R.string.phone),
                            keyboardType = KeyboardType.Phone
                        )
                        Space(y = 12)
                        MCoreTextField(
                            value = password,
                            onValueChange = { password = it; showError = false },
                            label = stringResource(R.string.password),
                            isPassword = true,
                            isError = showError && password.isBlank()
                        )
                        Space(y = 8)
                        AnimatedFeedback(
                            visible = showError,
                            message = errorMessage.ifBlank { stringResource(R.string.validation_error) },
                            isError = true
                        )
                        Space(y = 20)
                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        } else {
                            MCoreButton(
                                text = stringResource(R.string.register_button),
                                onClick = {
                                    if (username.isBlank() || password.isBlank() || organizationName.isBlank()) {
                                        errorMessage = ""
                                        showError = true
                                        return@MCoreButton
                                    }
                                    viewModelGlobal?.register(
                                        username = username.trim(),
                                        email = email.trim().ifBlank { null },
                                        phone = phone.trim().ifBlank { null },
                                        password = password,
                                        organizationName = organizationName.trim()
                                    )
                                }
                            )
                        }
                        Space(y = 12)
                        TextButton(
                            onClick = { navC?.navigate(ScreenRoute.Login.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${stringResource(R.string.has_account)} ${stringResource(R.string.login_button)}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
