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
    private val _listOperationAll = MutableStateFlow<List<OperationRelation>>(emptyList())
    private val _stateSave = MutableStateFlow<Long>(0)
    val listOperation get() = _listOperation.asStateFlow()
    val operationDetail get() = _operationDetail.asStateFlow()
    val stateSave get() = _stateSave.asStateFlow()
    val listOperationAll get() = _listOperationAll.asStateFlow()

    private val _listOperationToDay = MutableStateFlow<Int?>(0)
    private val _listOperationToDayCDF = MutableStateFlow<Int?>(0)
    val listOperationToday get() = _listOperationToDay.asStateFlow()
    val listOperationTodayCDF get() = _listOperationToDayCDF.asStateFlow()

    fun getAllOperation(userId : Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.allOperation(userId).collect {
                _listOperation.value = it
            }
        }
    }

    fun getDetailOperation(operationId : Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.getDetailOperation(operationId).collect {
                _operationDetail.value = it
            }
        }
    }

    fun getAllOperationToDay(dateCurrent : String, currencyId : Int, userId : Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _listOperationToDay.value = repository.allOperationDay(dateCurrent,currencyId,userId)
            Log.e("vm today ->","${_listOperationToDay.value}")
        }
    }

    fun getAllOperationToDayCDF(dateCurrent : String, currencyId : Int, userId : Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _listOperationToDayCDF.value = repository.allOperationDayCDF(dateCurrent,currencyId,userId)
        }
    }

    fun insert(operation: OperationModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _stateSave.value = repository.insert(operation)
            Log.e("vm insert =>","$operation")
            Log.e("state row =>","${_stateSave.value}")
        }
    }

    fun update(operation: OperationModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(operation)
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