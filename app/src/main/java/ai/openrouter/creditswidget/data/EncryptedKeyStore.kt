package ai.openrouter.creditswidget.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps only IV+ciphertext in private preferences; the AES key never leaves Android Keystore. */
class EncryptedKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("secure_key", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("openrouter_api_key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(KeyGenParameterSpec.Builder("openrouter_api_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build()) }.generateKey()
    }
    fun save(value: String) { val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key()); prefs.edit().putString("blob", Base64.encodeToString(c.iv, Base64.NO_WRAP) + "." + Base64.encodeToString(c.doFinal(value.toByteArray()), Base64.NO_WRAP)).apply() }
    fun read(): String? = try { val parts = prefs.getString("blob", null)?.split('.') ?: return null; if (parts.size != 2) return null; val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))); String(c.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))) } catch (_: Exception) { delete(); null }
    fun delete() { prefs.edit().clear().apply() }
    fun exists() = read() != null
}
