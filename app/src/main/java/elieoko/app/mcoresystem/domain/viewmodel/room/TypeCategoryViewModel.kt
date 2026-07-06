package elieoko.app.mcoresystem.domain.viewmodel.room

import android.util.Log
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
    private val _error = MutableStateFlow<String?>(null)
    val listTypeCategories get() = _listTypeCategory.asStateFlow()
    val error get() = _error.asStateFlow()

    fun getAll() = safeLaunch { refresh() }

    fun insert(data: TypeCategoryModel) = safeLaunch {
        repository.insert(data)
        refresh()
    }

    fun update(data: TypeCategoryModel) = safeLaunch {
        repository.update(data)
        refresh()
    }

    fun delete(data: TypeCategoryModel) = safeLaunch {
        repository.delete(data)
        refresh()
    }

    fun consumeError() { _error.value = null }

    private suspend fun refresh() {
        _listTypeCategory.value = repository.allData()
    }

    private fun safeLaunch(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            Log.e("TypeCategoryViewModel", "Opération échouée", e)
            _error.value = e.message ?: "Une erreur est survenue"
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
