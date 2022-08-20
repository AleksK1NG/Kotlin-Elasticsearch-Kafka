package com.search.microservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SearchMicroserviceApplication

fun main(args: Array<String>) {
	runApplication<SearchMicroserviceApplication>(*args)
}
