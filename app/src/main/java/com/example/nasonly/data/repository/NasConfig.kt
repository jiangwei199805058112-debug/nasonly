package com.example.nasonly.data.repository

/**
 * NAS 连接配置。
 */
data class NasConfig(
    val ip: String,
    val port: Int,
    val username: String,
    val password: String,
    val saveCredentials: Boolean
)
