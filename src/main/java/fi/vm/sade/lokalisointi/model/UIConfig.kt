package fi.vm.sade.lokalisointi.model

data class UIConfig(val sourceEnvironments: Collection<OphEnvironment>, val currentEnvironment: OphEnvironment)