package elieoko.app.mcoresystem.domain.viewmodel.config

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.data.preferences.SessionRepository
import elieoko.app.mcoresystem.data.preferences.UserSession
import elieoko.app.mcoresystem.data.remote.SyncManager
import elieoko.app.mcoresystem.data.remote.SyncStatus
import elieoko.app.mcoresystem.domain.repository.AuthRepository
import elieoko.app.mcoresystem.domain.repository.AuthResult
import elieoko.app.mcoresystem.domain.viewmodel.room.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstanceRoomViewModel(
    currencyViewModel: CurrencyViewModel,
    userViewModel: UserViewModel,
    paymentMethodViewModel: PaymentMethodViewModel,
    organismViewModel: OrganismViewModel,
    operationViewModel: OperationViewModel,
    categoryViewModel: CategoryViewModel,
    typeCategoryViewModel: TypeCategoryViewModel
) : ViewModel() {
    var currency = currencyViewModel
    var user = userViewModel
    var paymentMethod = paymentMethodViewModel
    var organism = organismViewModel
    var operation = operationViewModel
    var category = categoryViewModel
    var typeCategory = typeCategoryViewModel
}

class ApplicationViewModel(
    roomVm: InstanceRoomViewModel,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val sessionRepository: SessionRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val syncManager: SyncManager? = null,
    private val onRequestSync: () -> Unit = {}
) : ViewModel() {
    val room = roomVm
    var currentUserId = mutableIntStateOf(1)
    var currentUsername = mutableStateOf("Utilisateur")
    var currentOrganismId = mutableIntStateOf(1)

    private val _usdToCdfRate = MutableStateFlow(ExchangeRateRepository.DEFAULT_RATE)
    val usdToCdfRate = _usdToCdfRate.asStateFlow()

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult = _authResult.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> =
        syncManager?.status ?: MutableStateFlow(SyncStatus.OFFLINE_ONLY).asStateFlow()

    init {
        viewModelScope.launch {
            exchangeRateRepository.usdToCdfRate.collect { rate ->
                _usdToCdfRate.value = rate
            }
        }
        viewModelScope.launch {
            sessionRepository?.session?.collect { session ->
                if (session.isLoggedIn) {
                    applySession(session)
                }
            }
        }
    }

    fun applySession(session: UserSession) {
        currentUserId.intValue = session.userId
        currentUsername.value = session.username.ifBlank { "Utilisateur" }
        currentOrganismId.intValue = session.organismId
    }

    fun setUsdToCdfRate(rate: Double) {
        viewModelScope.launch {
            exchangeRateRepository.setUsdToCdfRate(rate)
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authResult.value = authRepository?.login(identifier, password)
            _isAuthLoading.value = false
            if (_authResult.value is AuthResult.Success) requestSync()
        }
    }

    fun register(username: String, email: String?, phone: String?, password: String, organizationName: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authResult.value = authRepository?.register(username, email, phone, password, organizationName)
            _isAuthLoading.value = false
            if (_authResult.value is AuthResult.Success) requestSync()
        }
    }

    fun consumeAuthResult() {
        _authResult.value = null
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository?.logout()
            onDone()
        }
    }

    fun requestSync() {
        onRequestSync()
    }
}
