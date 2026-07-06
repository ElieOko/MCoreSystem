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
        refresh()
    }

    fun insert(currency: CurrencyModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(currency)
            refresh()
        }
    }

    fun update(currency: CurrencyModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(currency)
            refresh()
        }
    }

    fun delete(currency: CurrencyModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.delete(currency)
            refresh()
        }
    }

    private suspend fun refresh() {
        withContext(Dispatchers.IO) {
            _listCurrencies.value = repository.allCurrency()
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
