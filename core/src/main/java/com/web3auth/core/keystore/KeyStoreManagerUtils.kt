package com.web3auth.core.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.text.Charsets.UTF_8

object KeyStoreManagerUtils {

    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val Android_KEY_STORE = "AndroidKeyStore"
    private const val WEB3AUTH = "Web3Auth"
    private lateinit var encryptedPairData: Pair<ByteArray, ByteArray>
    private lateinit var sharedPreferences: EncryptedSharedPreferences

    fun initializePreferences(context: Context) {
        try {
            val keyGenParameterSpec = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "Web3Auth",
                keyGenParameterSpec,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Key generator to encrypt and decrypt data
     */
    fun getKeyGenerator() {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Android_KEY_STORE)
        val keyGeneratorSpec = KeyGenParameterSpec.Builder(
            WEB3AUTH,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(keyGeneratorSpec)
        keyGenerator.generateKey()
    }

    /**
     * Method to encrypt data with key
     */
    fun encryptData(key: String, data: String) {
        if (this::sharedPreferences.isInitialized) {
            sharedPreferences.edit().putString(key, data)?.apply()
            encryptedPairData = getEncryptedDataPair(data)
            encryptedPairData.second.toString(UTF_8)
        }
    }

    /**
     * Key generator to encrypt/decrypt data
     */
    private fun getEncryptedDataPair(data: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv: ByteArray = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(UTF_8))
        return Pair(iv, encryptedData)
    }

    /**
     * Method to decrypt data with key
     */
    fun decryptData(key: String): String? {
        var result: String? = null
        try {
            if (this::sharedPreferences.isInitialized) {
                val sharedPreferenceIds = sharedPreferences.all
                sharedPreferenceIds.forEach {
                    if (it.key.contains(key)) {
                        result = sharedPreferences.getString(it.key, "")
                    }
                }
                if (result == null) return null
                val encryptedPairData = result?.let { getEncryptedDataPair(it) }
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val keySpec = IvParameterSpec(encryptedPairData?.first)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), keySpec)
                return cipher.doFinal(encryptedPairData?.second).toString(UTF_8)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return result
    }

    /**
     * Delete All local storage
     */
    fun deletePreferencesData(key: String) {
        sharedPreferences.edit().remove(key)?.apply()
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.contains(key)) {
                sharedPreferences.edit().remove(key)?.apply()
            }
        }
    }

    /**
     * Get key from KeyStore
     */
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(Android_KEY_STORE)
        keyStore.load(null)
        val secreteKeyEntry: KeyStore.SecretKeyEntry =
            keyStore.getEntry(WEB3AUTH, null) as KeyStore.SecretKeyEntry
        return secreteKeyEntry.secretKey
    }
}