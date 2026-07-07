package elieoko.app.mcoresystem.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "mcore_session")

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userId: Int = 0,
    val username: String = "",
    val organismId: Int = 1,
    val organismUuid: String = "",
    val organismName: String = "",
    val userUuid: String = "",
    val role: String = "MEMBER"
)

/**
 * Persistance de session : l'utilisateur reste connecté entre les lancements
 * tant qu'il ne se déconnecte pas volontairement (ou que la session Supabase expire).
 */
class SessionRepository(private val context: Context) {

    private object Keys {
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val ORGANISM_ID = intPreferencesKey("organism_id")
        val ORGANISM_UUID = stringPreferencesKey("organism_uuid")
        val ORGANISM_NAME = stringPreferencesKey("organism_name")
        val USER_UUID = stringPreferencesKey("user_uuid")
        val ROLE = stringPreferencesKey("role")
    }

    val session: Flow<UserSession> = context.sessionStore.data.map { prefs ->
        UserSession(
            isLoggedIn = prefs[Keys.LOGGED_IN] ?: false,
            userId = prefs[Keys.USER_ID] ?: 0,
            username = prefs[Keys.USERNAME] ?: "",
            organismId = prefs[Keys.ORGANISM_ID] ?: 1,
            organismUuid = prefs[Keys.ORGANISM_UUID] ?: "",
            organismName = prefs[Keys.ORGANISM_NAME] ?: "",
            userUuid = prefs[Keys.USER_UUID] ?: "",
            role = prefs[Keys.ROLE] ?: "MEMBER"
        )
    }

    suspend fun saveSession(session: UserSession) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = session.isLoggedIn
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.ORGANISM_ID] = session.organismId
            prefs[Keys.ORGANISM_UUID] = session.organismUuid
            prefs[Keys.ORGANISM_NAME] = session.organismName
            prefs[Keys.USER_UUID] = session.userUuid
            prefs[Keys.ROLE] = session.role
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
