package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.stock.StockViewModel

@Composable
fun StockLoginPage(viewModel: StockViewModel, onLeave: () -> Unit) {
    val accountCount by viewModel.accountCount.collectAsState()
    if (accountCount < 0) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Text("Chargement de l'espace stock…")
        }
        return
    }
    if (accountCount == 0) {
        RegisterOrganizationForm(viewModel, onLeave)
    } else {
        LoginForm(viewModel, onLeave)
    }
}

@Composable
private fun RegisterOrganizationForm(viewModel: StockViewModel, onLeave: () -> Unit) {
    var organization by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Créer l'espace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Le propriétaire de l'organisation crée l'espace. Les livreurs s'y connecteront ensuite avec l'identifiant que vous leur donnerez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LabeledField("Nom de l'organisation", organization, { organization = it })
        LabeledField("Identifiant du propriétaire", username, { username = it })
        LabeledField("Mot de passe", password, { password = it })
        LabeledField("Confirmer le mot de passe", confirm, { confirm = it })
        Button(
            onClick = {
                sending = true
                viewModel.registerOrganization(organization, username, password, confirm) { sending = false }
            },
            enabled = !sending,
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
        ) { Text("Créer l'espace") }
        TextButton(onClick = onLeave) { Text("Retour aux modules") }
    }
}

@Composable
private fun LoginForm(viewModel: StockViewModel, onLeave: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Espace organisation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Propriétaire ou livreur : connectez-vous à l'organisation à laquelle vous appartenez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LabeledField("Identifiant", username, { username = it })
        LabeledField("Mot de passe", password, { password = it })
        Button(
            onClick = {
                sending = true
                viewModel.login(username, password) { sending = false }
            },
            enabled = !sending && username.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
        ) { Text("Se connecter") }
        TextButton(onClick = onLeave) { Text("Retour aux modules") }
    }
}
