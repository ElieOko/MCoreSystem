package elieoko.app.mcoresystem.domain.model

data class MenuItem(
    val id : Int = 0,
    val name : String = "",
    val eventClick : ()-> Unit
)
