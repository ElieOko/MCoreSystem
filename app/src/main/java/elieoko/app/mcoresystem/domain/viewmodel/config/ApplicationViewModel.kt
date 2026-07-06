package elieoko.app.mcoresystem.domain.viewmodel.config

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import elieoko.app.mcoresystem.domain.viewmodel.room.*
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {
    val room = roomVm
    var currentUserId = mutableIntStateOf(1)
    var currentUsername = mutableStateOf("Utilisateur")
    var currentOrganismId = mutableIntStateOf(1)

    private val _usdToCdfRate = MutableStateFlow(ExchangeRateRepository.DEFAULT_RATE)
    val usdToCdfRate = _usdToCdfRate.asStateFlow()

    init {
        viewModelScope.launch {
            exchangeRateRepository.usdToCdfRate.collect { rate ->
                _usdToCdfRate.value = rate
            }
        }
    }

    fun setUsdToCdfRate(rate: Double) {
        viewModelScope.launch {
            exchangeRateRepository.setUsdToCdfRate(rate)
        }
    }
}
