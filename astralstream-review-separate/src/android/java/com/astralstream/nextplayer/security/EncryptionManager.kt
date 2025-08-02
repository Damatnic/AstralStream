package com.astralstream.nextplayer.security

interface EncryptionManager {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
    fun encryptString(data: String): String
    fun decryptString(data: String): String
}