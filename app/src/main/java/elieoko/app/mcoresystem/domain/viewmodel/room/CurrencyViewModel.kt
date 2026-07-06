package elieoko.app.mcoresystem.domain.viewmodel.room

import android.util.Log
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
    private val _error = MutableStateFlow<String?>(null)
    val listCurrencies get() = _listCurrencies.asStateFlow()
    val error get() = _error.asStateFlow()

    fun getAllCurrencies() = safeLaunch { refresh() }

    fun insert(currency: CurrencyModel) = safeLaunch {
        repository.insert(currency)
        refresh()
    }

    fun update(currency: CurrencyModel) = safeLaunch {
        repository.update(currency)
        refresh()
    }

    fun delete(currency: CurrencyModel) = safeLaunch {
        repository.delete(currency)
        refresh()
    }

    fun consumeError() { _error.value = null }

    private suspend fun refresh() {
        _listCurrencies.value = repository.allCurrency()
    }

    private fun safeLaunch(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            Log.e("CurrencyViewModel", "Opération échouée", e)
            _error.value = e.message ?: "Une erreur est survenue"
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
