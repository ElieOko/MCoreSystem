package elieoko.app.mcoresystem.domain.viewmodel.room

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.repository.room.PaymentMethodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentMethodViewModel(private val repository: PaymentMethodRepository) : ViewModel() {

    private val _listPaymentMethod = MutableStateFlow<List<PaymentMethodModel>>(arrayListOf())
    private val _error = MutableStateFlow<String?>(null)
    val listPaymentMethod get() = _listPaymentMethod.asStateFlow()
    val error get() = _error.asStateFlow()

    fun getAllPaymentMethod() = safeLaunch { refresh() }

    fun insert(paymentMethod: PaymentMethodModel) = safeLaunch {
        repository.insert(paymentMethod)
        refresh()
    }

    fun update(paymentMethod: PaymentMethodModel) = safeLaunch {
        repository.update(paymentMethod)
        refresh()
    }

    fun delete(paymentMethod: PaymentMethodModel) = safeLaunch {
        repository.delete(paymentMethod)
        refresh()
    }

    fun consumeError() { _error.value = null }

    private suspend fun refresh() {
        _listPaymentMethod.value = repository.allPaymentMethod()
    }

    private fun safeLaunch(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            Log.e("PaymentMethodVM", "Opération échouée", e)
            _error.value = e.message ?: "Une erreur est survenue"
        }
    }
}

class PaymentMethodViewModelFactory(private val repository: PaymentMethodRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentMethodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentMethodViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
