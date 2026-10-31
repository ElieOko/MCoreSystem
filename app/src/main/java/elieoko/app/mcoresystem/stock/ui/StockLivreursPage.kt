package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.StockLivreur

@Composable
fun StockLivreursPage(
    viewModel: StockViewModel,
    onBack: () -> Unit,
    onOpenLivreur: (Long) -> Unit
) {
    val livreurs by viewModel.livreurs.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StockLivreur?>(null) }
    var deleting by remember { mutableStateOf<StockLivreur?>(null) }

    StockScaffold(
        title = "Livreurs",
        onBack = onBack,
        floatingAction = { StockFab(Icons.Default.Add, "Nouveau livreur") { creating = true } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (livreurs.isEmpty()) {
                item { EmptyHint("Aucun livreur. Ajoutez les personnes qui reçoivent un mini-stock.") }
            }
            items(livreurs, key = { it.id }) { livreur ->
                val total = viewModel.livreurTotal(livreur.id, lines)
                val kinds = lines.count { it.livreurId == livreur.id && it.quantity > 0.0 }
                val login = accounts.firstOrNull { it.livreurId == livreur.id }?.username
                Card(
                    onClick = { onOpenLivreur(livreur.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 8.dp)) {
                        Text(livreur.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (livreur.phone.isBlank()) "Sans téléphone" else livreur.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (login.isNullOrBlank()) "Sans identifiant" else "Connexion : $login",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Mini-stock : ${StockRules.formatQty(total)} unités • $kinds article(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editing = livreur }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier")
                            }
                            IconButton(onClick = { deleting = livreur }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        LivreurFormDialog(
            title = "Nouveau livreur",
            name = "",
            phone = "",
            username = "",
            requirePassword = true,
            onDismiss = { creating = false },
            onSave = { name, phone, username, password ->
                viewModel.createLivreur(name, phone, username, password) { ok -> if (ok) creating = false }
            }
        )
    }
    editing?.let { livreur ->
        LivreurFormDialog(
            title = "Modifier le livreur",
            name = livreur.name,
            phone = livreur.phone,
            username = accounts.firstOrNull { it.livreurId == livreur.id }?.username ?: "",
            requirePassword = false,
            onDismiss = { editing = null },
            onSave = { name, phone, username, password ->
                viewModel.renameLivreur(livreur, name, phone, username, password) { ok -> if (ok) editing = null }
            }
        )
    }
    deleting?.let { livreur ->
        ConfirmDialog(
            title = "Supprimer le livreur",
            message = "« ${livreur.name} » sera retiré. La suppression est refusée s'il lui reste un mini-stock.",
            onConfirm = {
                viewModel.deleteLivreur(livreur) { ok -> if (ok) deleting = null }
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun LivreurFormDialog(
    title: String,
    name: String,
    phone: String,
    username: String,
    requirePassword: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var nameValue by remember { mutableStateOf(name) }
    var phoneValue by remember { mutableStateOf(phone) }
    var usernameValue by remember { mutableStateOf(username) }
    var passwordValue by remember { mutableStateOf("") }
    FormDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onSave(nameValue, phoneValue, usernameValue, passwordValue) }
    ) {
        LabeledField("Nom", nameValue, { nameValue = it })
        LabeledField("Téléphone", phoneValue, { phoneValue = it }, "Facultatif", androidx.compose.ui.text.input.KeyboardType.Phone)
        LabeledField("Identifiant de connexion", usernameValue, { usernameValue = it })
        LabeledField(
            if (requirePassword) "Mot de passe" else "Nouveau mot de passe",
            passwordValue,
            { passwordValue = it },
            if (requirePassword) "4 caractères minimum" else "Laisser vide pour conserver",
            password = true
        )
    }
}
