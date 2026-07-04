package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.repository.room.OperationRepository
import elieoko.app.mcoresystem.domain.repository.room.PaymentMethodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OperationViewModel(private val repository: OperationRepository) : ViewModel() {

    private val _listOperation = MutableStateFlow<List<OperationModel>>(arrayListOf())
    val listOperation get() = _listOperation.asStateFlow()

    fun getAllOperation(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _listOperation.value = repository.allData()
            }
        }
    }
    fun insert(data : OperationModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(data)
        }
    }

    fun update(data : OperationModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(data)
        }

    }

}

class OperationViewModelFactory(private val repository: OperationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OperationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OperationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}