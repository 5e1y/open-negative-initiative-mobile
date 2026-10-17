package fr.courtillon.onimobile.link

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import uniffi.oni_link_ffi.generateIdentity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The desktop this phone pairs with, kept to reconnect without the QR code. */
data class PairedDesktop(val key: ByteArray, val name: String, val addresses: List<String>)

/**
 * The phone's identity and its paired desktop, sealed by a key that never leaves this device's keystore:
 * moved to another phone, the blobs no longer open and the phone pairs again.
 */
class Vault(context: Context) {
    private val prefs = context.getSharedPreferences("link", Context.MODE_PRIVATE)

    /** The phone's key pair, made and stored on first use. */
    fun identity(): ByteArray = read("identity") ?: generateIdentity().also { write("identity", it) }

    fun desktop(): PairedDesktop? = read("desktop")?.let { bytes ->
        runCatching {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            val addresses = json.getJSONArray("addresses")
            PairedDesktop(
                key = Base64.decode(json.getString("key"), Base64.NO_WRAP),
                name = json.getString("name"),
                addresses = List(addresses.length()) { addresses.getString(it) },
            )
        }.getOrNull()
    }

    fun save(desktop: PairedDesktop) {
        val json = JSONObject()
            .put("key", Base64.encodeToString(desktop.key, Base64.NO_WRAP))
            .put("name", desktop.name)
            .put("addresses", JSONArray(desktop.addresses))
        write("desktop", json.toString().toByteArray(Charsets.UTF_8))
    }

    fun forgetDesktop() {
        prefs.edit().remove("desktop").apply()
    }

    // MARK: Sealing

    private fun read(name: String): ByteArray? {
        val stored = prefs.getString(name, null) ?: return null
        return runCatching {
            val sealed = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_LENGTH))
            cipher.doFinal(sealed, IV_LENGTH, sealed.size - IV_LENGTH)
        }.getOrNull()
    }

    private fun write(name: String, data: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = cipher.iv + cipher.doFinal(data)
        prefs.edit().putString(name, Base64.encodeToString(sealed, Base64.NO_WRAP)).commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "oni-link"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}
