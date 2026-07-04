package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.*
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.repository.room.OrganismRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*

class OrganismViewModel(private val repository: OrganismRepository) : ViewModel() {

    private val _listOrganism = MutableStateFlow<List<OrganismModel>>(arrayListOf())
    val listOrganism get() = _listOrganism.asStateFlow()

    fun getAllOrganism(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _listOrganism.value = repository.allData()
            }
        }
    }
    fun insert(data : OrganismModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(data)
        }
    }

    fun update(data : OrganismModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(data)
        }

    }

}

class OrganismViewModelFactory(private val repository: OrganismRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrganismViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrganismViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}