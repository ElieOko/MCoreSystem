package elieoko.app.mcoresystem.data.remote

import elieoko.app.mcoresystem.domain.interfaces.room.ISyncQueueDao
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel

/**
 * Journalise chaque changement local dans la file de synchronisation.
 * Injecté dans les repositories : c'est le seul point de contact entre
 * la couche données locale et le mécanisme de synchronisation.
 */
class ChangeTracker(private val syncQueueDao: ISyncQueueDao) {

    fun recordUpsert(entityType: String, uuid: String) {
        if (uuid.isBlank()) return
        // Évite les doublons d'upsert pour une même entité.
        syncQueueDao.removeUpsertsFor(uuid)
        syncQueueDao.insert(
            SyncQueueModel(entityType = entityType, entityUuid = uuid, operation = SyncQueueModel.OP_UPSERT)
        )
    }

    fun recordDelete(entityType: String, uuid: String) {
        if (uuid.isBlank()) return
        syncQueueDao.removeUpsertsFor(uuid)
        syncQueueDao.insert(
            SyncQueueModel(entityType = entityType, entityUuid = uuid, operation = SyncQueueModel.OP_DELETE)
        )
    }
}
