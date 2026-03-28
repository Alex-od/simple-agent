package com.danichapps.ragserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class RagServerApplication

fun main(args: Array<String>) {
    runApplication<RagServerApplication>(*args)
}
