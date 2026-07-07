package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.IUserDao
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.domain.util.TimeUtil
import kotlinx.coroutines.flow.Flow

class UserRepository(
    val dao : IUserDao,
    private val tracker: ChangeTracker? = null
) {
    val allUser : Flow<List<UserModel>> = dao.getAll()

    @WorkerThread
    fun getCurrentUser(userId : Int): List<UserModel> = dao.loadAllById(userId)

    @WorkerThread
    fun login(username: String, password: String): UserModel? = dao.login(username, password)

    @WorkerThread
    fun countUsers(): Int = dao.countUsers()

    @WorkerThread
    fun insert(user: UserModel) {
        val stamped = user.copy(updatedAt = TimeUtil.nowIso())
        dao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_USER, stamped.uuid)
    }

    @WorkerThread
    fun update(user: UserModel) {
        val stamped = user.copy(updatedAt = TimeUtil.nowIso())
        dao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_USER, stamped.uuid)
    }
}