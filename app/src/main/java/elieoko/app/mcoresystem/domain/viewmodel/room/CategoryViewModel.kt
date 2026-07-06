package elieoko.app.mcoresystem.domain.viewmodel.room

import android.util.Log
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
    private val _error = MutableStateFlow<String?>(null)
    val listCategories get() = _listCategory.asStateFlow()
    val error get() = _error.asStateFlow()

    fun getAll() = safeLaunch { refresh() }

    fun insert(data: CategoryModel) = safeLaunch {
        repository.insert(data)
        refresh()
    }

    fun update(data: CategoryModel) = safeLaunch {
        repository.update(data)
        refresh()
    }

    fun delete(data: CategoryModel) = safeLaunch {
        repository.delete(data)
        refresh()
    }

    fun consumeError() { _error.value = null }

    private suspend fun refresh() {
        _listCategory.value = repository.allData()
    }

    private fun safeLaunch(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            Log.e("CategoryViewModel", "Opération échouée", e)
            _error.value = e.message ?: "Une erreur est survenue"
        }
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
