package elieoko.app.mcoresystem.stock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stockSessionStore by preferencesDataStore(name = "stock_session")

data class StockSession(
    val accountId: Long,
    val organismId: Long,
    val organismUuid: String,
    val organismName: String,
    val username: String,
    val role: String,
    val livreurId: Long = 0,
    val livreurName: String = ""
) {
    val isLivreur: Boolean get() = role == StockRole.LIVREUR.name || role == "LIVREUR"
    val isOwner: Boolean get() = role.isNotBlank() && !isLivreur
}

class StockSessionStore(private val context: Context) {
    private object Keys {
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val ACCOUNT_ID = longPreferencesKey("account_id")
        val ORGANISM_ID = longPreferencesKey("organism_id")
        val ORGANISM_UUID = stringPreferencesKey("organism_uuid")
        val ORGANISM_NAME = stringPreferencesKey("organism_name")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val LIVREUR_ID = longPreferencesKey("livreur_id")
        val LIVREUR_NAME = stringPreferencesKey("livreur_name")
    }

    val session: Flow<StockSession?> = context.stockSessionStore.data.map { prefs ->
        if (prefs[Keys.LOGGED_IN] != true) {
            null
        } else {
            StockSession(
                accountId = prefs[Keys.ACCOUNT_ID] ?: 0,
                organismId = prefs[Keys.ORGANISM_ID] ?: 0,
                organismUuid = prefs[Keys.ORGANISM_UUID] ?: "",
                organismName = prefs[Keys.ORGANISM_NAME] ?: "",
                username = prefs[Keys.USERNAME] ?: "",
                role = prefs[Keys.ROLE] ?: StockRole.LIVREUR.name,
                livreurId = prefs[Keys.LIVREUR_ID] ?: 0,
                livreurName = prefs[Keys.LIVREUR_NAME] ?: ""
            )
        }
    }

    suspend fun save(session: StockSession) {
        context.stockSessionStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = true
            prefs[Keys.ACCOUNT_ID] = session.accountId
            prefs[Keys.ORGANISM_ID] = session.organismId
            prefs[Keys.ORGANISM_UUID] = session.organismUuid
            prefs[Keys.ORGANISM_NAME] = session.organismName
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.ROLE] = session.role
            prefs[Keys.LIVREUR_ID] = session.livreurId
            prefs[Keys.LIVREUR_NAME] = session.livreurName
        }
    }

    suspend fun clear() {
        context.stockSessionStore.edit { it.clear() }
    }
}
