package com.search.microservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SearchMicroserviceApplication

fun main(args: Array<String>) {
	runApplication<SearchMicroserviceApplication>(*args)
}
