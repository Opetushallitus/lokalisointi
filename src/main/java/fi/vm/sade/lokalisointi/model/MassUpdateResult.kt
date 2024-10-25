package fi.vm.sade.lokalisointi.model

data class MassUpdateResult(
    var notModified: Int? = 0,
    var created: Int? = 0,
    var updated: Int? = 0,
    var status: String? = null
) {
    fun incNotModified() {
        notModified?.inc()
    }

    fun incCreated() {
        created?.inc()
    }

    fun incUpdated() {
        updated?.inc()
    }
}
