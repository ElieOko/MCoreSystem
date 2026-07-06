package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.util.TimeUtil

class CurrencyRepository(
    private val currencyDao: ICurrencyDao,
    private val tracker: ChangeTracker? = null
) {
    @WorkerThread
    fun allCurrency() : List<CurrencyModel> = currencyDao.getAll()

    @WorkerThread
    suspend fun insert(currencies: CurrencyModel) {
        val stamped = currencies.copy(updatedAt = TimeUtil.nowIso())
        currencyDao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_CURRENCY, stamped.uuid)
    }

    @WorkerThread
    suspend fun update(currencies: CurrencyModel) {
        val stamped = currencies.copy(updatedAt = TimeUtil.nowIso())
        currencyDao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_CURRENCY, stamped.uuid)
    }

    @WorkerThread
    suspend fun delete(currencies: CurrencyModel) {
        currencyDao.delete(currencies)
        tracker?.recordDelete(SyncQueueModel.TYPE_CURRENCY, currencies.uuid)
    }
}
