package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.IPaymentMethodDao
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.util.TimeUtil

class PaymentMethodRepository(
    val paymentMethodDao: IPaymentMethodDao,
    private val tracker: ChangeTracker? = null
) {
    @WorkerThread
    fun allPaymentMethod() : List<PaymentMethodModel> = paymentMethodDao.getAll()

    @WorkerThread
    suspend fun insert(paymentMethod: PaymentMethodModel) {
        val stamped = paymentMethod.copy(updatedAt = TimeUtil.nowIso())
        paymentMethodDao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_PAYMENT_METHOD, stamped.uuid)
    }

    @WorkerThread
    suspend fun update(paymentMethod: PaymentMethodModel) {
        val stamped = paymentMethod.copy(updatedAt = TimeUtil.nowIso())
        paymentMethodDao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_PAYMENT_METHOD, stamped.uuid)
    }

    @WorkerThread
    suspend fun delete(paymentMethod: PaymentMethodModel) {
        paymentMethodDao.delete(paymentMethod)
        tracker?.recordDelete(SyncQueueModel.TYPE_PAYMENT_METHOD, paymentMethod.uuid)
    }
}
