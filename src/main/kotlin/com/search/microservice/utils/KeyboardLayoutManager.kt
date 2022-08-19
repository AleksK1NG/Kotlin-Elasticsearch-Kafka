package com.search.microservice.utils

class KeyboardLayoutManager(private val keyboardLayoutMap: Map<String, String>) {

    fun getOppositeKeyboardLayoutTerm(originalTerm: String): String {
        val stringBuilder = StringBuilder()
        val termList = originalTerm.split("").toList()
        termList.forEach {
            if (keyboardLayoutMap.containsKey(it)) {
                stringBuilder.append(keyboardLayoutMap[it])
            } else {
                stringBuilder.append(it)
            }
        }
        return stringBuilder.toString()
    }
}