package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.repository.room.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrencyViewModel(private val repository: CurrencyRepository) : ViewModel() {
    private val _listCurrencies = MutableStateFlow<List<CurrencyModel>>(arrayListOf())
    val listCurrencies get() = _listCurrencies.asStateFlow()

    fun getAllCurrencies() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _listCurrencies.value = repository.allCurrency()
        }
    }

    fun insert(currency : CurrencyModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(currency)
        }
    }

    fun update(currency : CurrencyModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(currency)
        }
    }

}

class CurrencyViewModelFactory(private val repository: CurrencyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurrencyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CurrencyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}