package elieoko.app.mcoresystem.domain.model

enum class OperationStatus(val label: String) {
    OUVERT("Ouvert"),
    EN_ATTENTE("En attente"),
    CLOTURE("Clôturé");

    companion object {
        fun from(value: String?): OperationStatus =
            entries.firstOrNull { it.name == value } ?: OUVERT
    }
}
