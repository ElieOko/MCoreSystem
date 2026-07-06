package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.IPaymentMethodDao
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

class PaymentMethodRepository(val paymentMethodDao: IPaymentMethodDao) {
    @WorkerThread
    fun allPaymentMethod() : List<PaymentMethodModel> = paymentMethodDao.getAll()

    @WorkerThread
    suspend fun insert(paymentMethod: PaymentMethodModel) {
        paymentMethodDao.insertAll(paymentMethod)
    }

    @WorkerThread
    suspend fun update(paymentMethod: PaymentMethodModel) {
        paymentMethodDao.updateAll(paymentMethod)
    }

    @WorkerThread
    suspend fun delete(paymentMethod: PaymentMethodModel) {
        paymentMethodDao.delete(paymentMethod)
    }
}