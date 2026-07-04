package elieoko.app.mcoresystem.domain.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.domain.repository.room.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _listUser = MutableStateFlow<List<UserModel>>(arrayListOf())
    val listUser get() = _listUser.asStateFlow()
    fun getUser(userId : Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _listUser.value =  repository.getCurrentUser(userId)
        }

    }

    fun insert(user : UserModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.insert(user)
        }
    }

    fun update(user : UserModel) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.update(user)
        }

    }

}

class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}