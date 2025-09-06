package com.example.payagg.ports

interface ConfigPort {
    fun <T> getConfig(key: String, type: Class<T>): T?
    fun setConfig(key: String, value: Any): Boolean
    fun getAllConfigs(): Map<String, Any>
    fun deleteConfig(key: String): Boolean
    fun refreshConfig()
}
