package elieoko.app.mcoresystem.data.remote

import elieoko.app.mcoresystem.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Fournit le client Supabase.
 *
 * Les identifiants (URL + clé anonyme) sont injectés via BuildConfig depuis
 * local.properties ou les variables d'environnement — jamais dans le code source.
 * Si aucun identifiant n'est configuré, l'application fonctionne en mode 100 % local
 * (Room uniquement) sans planter : c'est le socle de l'architecture Offline First.
 */
object SupabaseProvider {

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!isConfigured) null
        else createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
