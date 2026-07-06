package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel
import elieoko.app.mcoresystem.domain.repository.room.TypeCategorieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TypeCategoryViewModel(private val repository: TypeCategorieRepository) : ViewModel() {
    private val _listTypeCategory = MutableStateFlow<List<TypeCategoryModel>>(arrayListOf())
    val listTypeCategories get() = _listTypeCategory.asStateFlow()

    fun getAll() = viewModelScope.launch {
        refresh()
    }

    fun insert(data: TypeCategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(data)
            refresh()
        }
    }

    fun update(data: TypeCategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(data)
            refresh()
        }
    }

    fun delete(data: TypeCategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.delete(data)
            refresh()
        }
    }

    private suspend fun refresh() {
        withContext(Dispatchers.IO) {
            _listTypeCategory.value = repository.allData()
        }
    }
}

class TypeCategoryViewModelFactory(private val repository: TypeCategorieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TypeCategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TypeCategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
