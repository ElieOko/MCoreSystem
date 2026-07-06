package elieoko.app.mcoresystem.domain.viewmodel.config

import androidx.lifecycle.*
import elieoko.app.mcoresystem.domain.viewmodel.room.*

class InstanceRoomViewModel(
    currencyViewModel : CurrencyViewModel,
    userViewModel : UserViewModel,
    paymentMethodViewModel : PaymentMethodViewModel,
    organismViewModel: OrganismViewModel,
    operationViewModel: OperationViewModel,
    categoryViewModel: CategoryViewModel,
    typeCategoryViewModel: TypeCategoryViewModel

) : ViewModel(){
    var currency        = currencyViewModel
    var user            = userViewModel
    var paymentMethod   = paymentMethodViewModel
    var organism        = organismViewModel
    var operation       = operationViewModel
    var category        = categoryViewModel
    var typeCategory    = typeCategoryViewModel
}

class ApplicationViewModel(
    roomVm : InstanceRoomViewModel
): ViewModel(){
    val room = roomVm
}