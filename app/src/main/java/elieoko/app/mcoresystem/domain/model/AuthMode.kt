package elieoko.app.mcoresystem.domain.model

/**
 * Mode d'authentification choisi par l'utilisateur :
 * - AUTO   : vérifie d'abord en local, puis sur Supabase si le compte n'existe pas localement.
 * - LOCAL  : uniquement la base Room de l'appareil.
 * - ONLINE : uniquement la table `users` de Supabase.
 */
enum class AuthMode { AUTO, LOCAL, ONLINE }
