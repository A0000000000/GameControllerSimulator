package cn.maoyanluo.bluetooth_library.bean

data class HIDRegisterData(
    val hidReporter: ByteArray,
    val name: String,
    val description: String,
    val provider: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HIDRegisterData

        if (!hidReporter.contentEquals(other.hidReporter)) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (provider != other.provider) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hidReporter.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + provider.hashCode()
        return result
    }
}
