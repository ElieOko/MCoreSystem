package elieoko.app.mcoresystem.domain.viewmodel.room

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
    val listPaymentMethod get() = _listPaymentMethod.asStateFlow()

    fun getAllPaymentMethod(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _listPaymentMethod.value = repository.allPaymentMethod()
            }
        }
    }
    fun insert(paymentMethod : PaymentMethodModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(paymentMethod)
        }
    }

    fun update(paymentMethod : PaymentMethodModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(paymentMethod)
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