package fi.vm.sade.lokalisointi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LokalisointiApplication

fun main(args: Array<String>) {
    runApplication<LokalisointiApplication>(*args)
}
