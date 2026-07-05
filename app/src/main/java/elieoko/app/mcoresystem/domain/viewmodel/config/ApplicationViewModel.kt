package elieoko.app.mcoresystem.domain.viewmodel.config

import androidx.lifecycle.ViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.CategoryViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.CurrencyViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.OperationViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.OrganismViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.PaymentMethodViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.TypeCategoryViewModel
import elieoko.app.mcoresystem.domain.viewmodel.room.UserViewModel

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