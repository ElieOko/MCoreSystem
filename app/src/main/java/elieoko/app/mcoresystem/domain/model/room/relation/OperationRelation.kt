package elieoko.app.mcoresystem.domain.model.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.model.room.UserModel


data class OperationRelation(

    @Embedded
    val operation: OperationModel?=null,

    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    val user: UserModel?=null,

    @Relation(
        parentColumn = "payment_method_id",
        entityColumn = "payment_method_id"
    )
    val paymentMethod: PaymentMethodModel?=null,

    @Relation(
        parentColumn = "currency_id",
        entityColumn = "currency_id"
    )
    val currency: CurrencyModel?=null,

    @Relation(
        parentColumn = "organism_id",
        entityColumn = "organism_id"
    )
    val organism: OrganismModel?=null,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: CategoryModel?=null,

)