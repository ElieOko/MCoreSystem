package elieoko.app.mcoresystem.presentation.ui.pages.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimaryDark

@Composable
fun ModuleLauncherPage(
    organismName: String,
    username: String,
    showFinance: Boolean,
    onOpenFinance: () -> Unit,
    onOpenStock: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = organismName.ifBlank { "MCoreSystem" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (username.isBlank()) "Choisissez un module" else "Bonjour $username",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Déconnexion")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "La connexion ouvre toute l'organisation : comptabilité et stock des livreurs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        if (showFinance) {
            ModuleCard(
                title = "Comptabilité",
                description = "Opérations, catégories, rapports et paramètres.",
                icon = Icons.Default.AccountBalance,
                gradient = listOf(BluePrimary, BluePrimaryDark),
                onClick = onOpenFinance
            )
            Spacer(Modifier.height(16.dp))
        }
        ModuleCard(
            title = "Stock livreurs",
            description = "Dépôt, montants, mini-stocks, commandes et historique. Les données partent vers le cloud de l'organisation.",
            icon = Icons.Default.LocalShipping,
            gradient = listOf(AccentTeal, Color(0xFF00796B)),
            onClick = onOpenStock
        )
    }
}

@Composable
private fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
                Spacer(Modifier.height(16.dp))
                Text("Ouvrir", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
