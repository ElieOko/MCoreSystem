package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel

class CurrencyRepository(private val currencyDao: ICurrencyDao) {
    @WorkerThread
    fun allCurrency() : List<CurrencyModel> = currencyDao.getAll()

    @WorkerThread
    suspend fun insert(currencies: CurrencyModel) {
        currencyDao.insertAll(currencies)
    }

    @WorkerThread
    suspend fun update(currencies: CurrencyModel) {
        currencyDao.updateAll(currencies)
    }
}