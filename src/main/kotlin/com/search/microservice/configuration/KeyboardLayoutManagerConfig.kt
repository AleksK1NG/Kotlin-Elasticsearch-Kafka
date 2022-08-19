package com.search.microservice.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.search.microservice.utils.KeyboardLayoutManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import reactor.util.Loggers


@Configuration
class KeyboardLayoutManagerConfig(private val objectMapper: ObjectMapper) {

    @Value(value = "\${elasticsearch.keyboard-layout-mappings-path:classpath:translate.json}")
    val keyboardLayoutMappingsResourceFile: Resource? = null

    @Bean
    fun getKeyboardLayoutManager(): KeyboardLayoutManager {
        val keyboardLayoutMappingsBytes = keyboardLayoutMappingsResourceFile?.inputStream?.readAllBytes() ?: byteArrayOf()
        val typeRef = HashMap<String, String>()::class.java
        val keyboardLayoutMappings = objectMapper.readValue(keyboardLayoutMappingsBytes, typeRef)
        return KeyboardLayoutManager(keyboardLayoutMappings).also { log.info("keyboard layout manager bean created: $keyboardLayoutMappings") }
    }

    companion object {
        private val log = Loggers.getLogger(ElasticSearchIndexConfig::class.java)
    }
}