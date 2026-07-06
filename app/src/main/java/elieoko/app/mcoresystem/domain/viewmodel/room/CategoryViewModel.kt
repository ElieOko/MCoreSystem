package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.repository.room.CategorieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel(private val repository: CategorieRepository) : ViewModel() {
    private val _listCategory = MutableStateFlow<List<CategoryModel>>(arrayListOf())
    val listCategories get() = _listCategory.asStateFlow()

    fun getAll() = viewModelScope.launch {
        refresh()
    }

    fun insert(data: CategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(data)
            refresh()
        }
    }

    fun update(data: CategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(data)
            refresh()
        }
    }

    fun delete(data: CategoryModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.delete(data)
            refresh()
        }
    }

    private suspend fun refresh() {
        _listCategory.value = repository.allData()
    }
}

class CategoryViewModelFactory(private val repository: CategorieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
