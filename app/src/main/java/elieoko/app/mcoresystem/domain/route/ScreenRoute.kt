package elieoko.app.mcoresystem.domain.route

enum class ScreenRoute {
    Login,
    Register,
    Portal,
    Stock,
    Home,
    History,
    Detail,
    Payment,
    PaymentPrinter,
    PrinterConfig,
    Category,
    TypeCategory,
    Report,
    Setting,
    Operation,
    OperationDetail;

    companion object {
        fun afterLogin(role: String): String =
            if (role.equals("LIVREUR", ignoreCase = true)) Stock.name else Portal.name
    }
}