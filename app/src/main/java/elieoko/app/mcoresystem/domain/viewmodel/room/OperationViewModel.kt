package elieoko.app.mcoresystem.domain.viewmodel.room

import android.util.Log
import androidx.lifecycle.*
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.repository.room.OperationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class OperationViewModel(private val repository: OperationRepository) : ViewModel() {

    private val _listOperation = MutableStateFlow<List<OperationRelation>>(arrayListOf())
    private val _operationDetail = MutableStateFlow<OperationRelation?>(OperationRelation())
    private val _stateSave = MutableStateFlow<Long>(0)
    private val _error = MutableStateFlow<String?>(null)
    val listOperation get() = _listOperation.asStateFlow()
    val operationDetail get() = _operationDetail.asStateFlow()
    val stateSave get() = _stateSave.asStateFlow()
    val error get() = _error.asStateFlow()

    private val _listOperationToDay = MutableStateFlow<Int?>(0)
    val listOperationToday get() = _listOperationToDay.asStateFlow()

    fun getAllOperation(userId : Int) = safeLaunch {
        repository.allOperation(userId).collect {
            _listOperation.value = it
        }
    }

    fun getDetailOperation(operationId : Int) = safeLaunch {
        repository.getDetailOperation(operationId).collect {
            _operationDetail.value = it
        }
    }

    fun getAllOperationToDay(dateCurrent : String, currencyId : Int, userId : Int) = safeLaunch {
        _listOperationToDay.value = repository.allOperationDay(dateCurrent, currencyId, userId)
    }

    fun insert(operation: OperationModel) = safeLaunch {
        _stateSave.value = repository.insert(operation)
    }

    fun update(operation: OperationModel) = safeLaunch {
        repository.update(operation)
    }

    fun delete(operation: OperationModel) = safeLaunch {
        repository.delete(operation)
    }

    fun updateStatus(operationId: Int, status: String) = safeLaunch {
        repository.updateStatus(operationId, status)
    }

    fun consumeError() { _error.value = null }

    private fun safeLaunch(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("OperationViewModel", "Opération échouée", e)
            _error.value = e.message ?: "Une erreur est survenue"
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
