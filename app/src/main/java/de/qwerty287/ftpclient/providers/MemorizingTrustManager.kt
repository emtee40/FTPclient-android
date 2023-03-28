package de.qwerty287.ftpclient.providers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Base64
import android.util.SparseArray
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.withLock

@SuppressLint("CustomX509TrustManager")
class MemorizingTrustManager(private var context: Context) : X509TrustManager {
    private val openDecisions = SparseArray<MTMDecision>()
    private val defaultTrustManager: X509TrustManager? = getTrustManager(null)
    private var decisionId = 0
    private var keyStoreStorage: SharedPreferences = context.getSharedPreferences(KEYSTORE_NAME, Context.MODE_PRIVATE)
    private var appKeyStore: KeyStore? = loadAppKeyStore()
    private var appTrustManager: X509TrustManager? = getTrustManager(appKeyStore)

    /**
     * Get a list of all certificate aliases stored in MTM.
     *
     * @return an [Enumeration] of all certificates
     */
    val certificates: Enumeration<String>
        get() = try {
            appKeyStore!!.aliases()
        } catch (e: KeyStoreException) {
            // this should never happen, however...
            throw RuntimeException(e)
        }

    /**
     * Get a certificate for a given alias.
     *
     * @param alias the certificate's alias as returned by [.getCertificates].
     * @return the certificate associated with the alias or <tt>null</tt> if none found.
     */
    fun getCertificate(alias: String?): Certificate {
        return try {
            appKeyStore!!.getCertificate(alias)
        } catch (e: KeyStoreException) {
            // this should never happen, however...
            throw RuntimeException(e)
        }
    }

    /**
     * Removes the given certificate from MTMs key store.
     *
     *
     *
     * **WARNING**: this does not immediately invalidate the certificate. It is
     * well possible that (a) data is transmitted over still existing connections or
     * (b) new connections are created using TLS renegotiation, without a new cert
     * check.
     *
     *
     * @param alias the certificate's alias as returned by [.getCertificates].
     * @throws KeyStoreException if the certificate could not be deleted.
     */
    fun deleteCertificate(alias: String?) {
        appKeyStore!!.deleteEntry(alias)
        keyStoreUpdated()
    }

    private fun getTrustManager(ks: KeyStore?): X509TrustManager? {
        try {
            val tmf = TrustManagerFactory.getInstance("X509")
            tmf.init(ks)
            for (t in tmf.trustManagers) {
                if (t is X509TrustManager) {
                    return t
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadAppKeyStore(): KeyStore? {
        val keyStore: KeyStore = try {
            KeyStore.getInstance(KeyStore.getDefaultType())
        } catch (e: KeyStoreException) {
            e.printStackTrace()
            return null
        }
        try {
            keyStore.load(null, null)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val keystore = keyStoreStorage.getString(KEYSTORE_KEY, null)
        if (keystore != null) {
            val inputStream = ByteArrayInputStream(Base64.decode(keystore, Base64.DEFAULT))
            try {
                keyStore.load(inputStream, "MTM".toCharArray())
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return keyStore
    }

    private fun storeCert(alias: String, cert: Certificate) {
        try {
            appKeyStore!!.setCertificateEntry(alias, cert)
        } catch (e: KeyStoreException) {
            e.printStackTrace()
            return
        }
        keyStoreUpdated()
    }

    private fun storeCert(cert: X509Certificate) {
        storeCert(cert.subjectDN.toString(), cert)
    }

    private fun keyStoreUpdated() {
        // reload appTrustManager
        appTrustManager = getTrustManager(appKeyStore)

        // store KeyStore to shared preferences
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            appKeyStore!!.store(byteArrayOutputStream, "MTM".toCharArray())
            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()
            keyStoreStorage.edit()
                .putString(KEYSTORE_KEY, Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // if the certificate is stored in the app key store, it is considered "known"
    private fun isCertKnown(cert: X509Certificate): Boolean {
        return try {
            appKeyStore!!.getCertificateAlias(cert) != null
        } catch (e: KeyStoreException) {
            false
        }
    }

    private fun checkCertTrusted(chain: Array<X509Certificate>, authType: String, isServer: Boolean) {
        try {
            if (isServer) {
                appTrustManager!!.checkServerTrusted(chain, authType)
            } else {
                appTrustManager!!.checkClientTrusted(chain, authType)
            }
        } catch (ae: CertificateException) {
            // if the cert is stored in our appTrustManager, we ignore expiredness
            if (isExpiredException(ae) || isCertKnown(chain[0])) {
                return
            }
            try {
                if (defaultTrustManager == null) {
                    throw ae
                }
                if (isServer) {
                    defaultTrustManager.checkServerTrusted(chain, authType)
                } else {
                    defaultTrustManager.checkClientTrusted(chain, authType)
                }
            } catch (e: CertificateException) {
                interactCert(chain, authType, e)
            }
        }
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        checkCertTrusted(chain, authType, false)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        checkCertTrusted(chain, authType, true)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager!!.acceptedIssuers
    }

    private fun createDecisionId(d: MTMDecision): Int {
        var myId: Int
        synchronized(openDecisions) {
            myId = decisionId
            openDecisions.put(myId, d)
            decisionId += 1
        }
        return myId
    }

    private fun certDetails(c: X509Certificate): String {

        //val validityDateFormatter = SimpleDateFormat("yyyy-MM-dd", context.resources.configuration.locale)
        val validityDateFormatter = SimpleDateFormat.getInstance()
        return context.getString(
            R.string.mtm_cert_details_properties,
            c.subjectDN.toString(),
            validityDateFormatter.format(c.notBefore),
            validityDateFormatter.format(c.notAfter),
            certHash(c, "SHA-1"),
            certHash(c, "SHA-256"),
            c.issuerDN.toString(),
        )
    }

    private fun certChainMessage(chain: Array<X509Certificate>, cause: CertificateException): String {
        var e: Throwable = cause
        val stringBuilder = StringBuilder()
        for (c in chain) {
            stringBuilder.append(certDetails(c))
        }
        return if (isPathException(e)) {
            context.getString(R.string.mtm_trust_anchor, context.getString(R.string.mtm_cert_details, stringBuilder.toString()))
        } else if (isExpiredException(e)) {
            context.getString(R.string.mtm_cert_expired, context.getString(R.string.mtm_cert_details, stringBuilder.toString()))
        } else {
            // get to the cause
            while (e.cause != null) {
                e = e.cause!!
            }
            context.getString(R.string.mtm_unknown_err, e.localizedMessage, stringBuilder.toString())
        }
    }

    private fun hostNameMessage(cert: X509Certificate, hostname: String): String {
        var hostnames: String
        try {
            val sans = cert.subjectAlternativeNames
            if (sans == null) {
                hostnames = cert.subjectDN.toString()
            } else {
                val stringBuilder = StringBuilder()
                for (altName in sans) {
                    // TODO find a better way to display this (use string resources)
                    val name = altName[1]!!
                    if (name is String) {
                        stringBuilder.append("[")
                        stringBuilder.append(altName[0])
                        stringBuilder.append("] ")
                        stringBuilder.append(name)
                        stringBuilder.append("\n")
                    }
                }
                hostnames = stringBuilder.toString()
            }
        } catch (e: CertificateParsingException) {
            e.printStackTrace()
            hostnames = context.getString(R.string.mtm_parsing_err, e.localizedMessage)
        }
        return context.getString(R.string.mtm_hostname_mismatch,
            hostname,
            hostnames,
            context.getString(R.string.mtm_cert_details, certDetails(cert)))
    }

    private fun interact(message: String, titleId: Int): Int {
        val choice = MTMDecision()
        val myId = createDecisionId(choice)

        val condition = choice.newCondition()

        (context as Activity?)!!.runOnUiThread {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context).setTitle(titleId).setMessage(message)
                .setPositiveButton(R.string.mtm_decision_always) { _: DialogInterface?, _: Int ->
                    interactResult(
                        myId,
                        MTMDecision.DECISION_ALWAYS,
                        condition
                    )
                }
                .setNeutralButton(R.string.mtm_decision_abort) { _: DialogInterface?, _: Int ->
                    interactResult(
                        myId,
                        MTMDecision.DECISION_ABORT,
                        condition
                    )
                }
                .setOnCancelListener { _: DialogInterface? -> interactResult(myId, MTMDecision.DECISION_ABORT, condition) }
            materialAlertDialogBuilder.show()
        }
        try {
            choice.withLock {
                condition.await()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return choice.state
    }

    private fun interactCert(chain: Array<X509Certificate>, authType: String, cause: CertificateException) {
        if (interact(certChainMessage(chain, cause), R.string.mtm_accept_cert) == MTMDecision.DECISION_ALWAYS) {
            storeCert(chain[0]) // only store the server cert, not the whole chain
        } else {
            throw cause
        }
    }

    private fun interactHostname(cert: X509Certificate, hostname: String): Boolean {
        if (interact(hostNameMessage(cert, hostname), R.string.mtm_accept_server_name) == MTMDecision.DECISION_ALWAYS) {
            storeCert(hostname, cert)
            return true
        }
        return false
    }

    private fun interactResult(decisionId: Int, choice: Int, cond: Condition) {
        var d: MTMDecision?
        synchronized(openDecisions) {
            d = openDecisions[decisionId]
            openDecisions.remove(decisionId)
        }
        if (d == null) {
            return
        }

        d!!.withLock {
            d!!.state = choice
            cond.signal()
        }
    }

    inner class MemorizingHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return try {
                val cert = session.peerCertificates[0] as X509Certificate
                if (cert == appKeyStore!!.getCertificate(hostname.lowercase())) {
                    true
                } else {
                    interactHostname(cert, hostname)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            // otherwise, we check if the hostname is an alias for this cert in our keystore
        }
    }

    private class MTMDecision : ReentrantLock() {
        var state = DECISION_INVALID

        companion object {
            const val DECISION_INVALID = 0
            const val DECISION_ABORT = 1
            const val DECISION_ALWAYS = 2
        }
    }

    companion object {
        const val KEYSTORE_NAME = "keystore"
        const val KEYSTORE_KEY = "keystore"

        private fun isExpiredException(e: Throwable?): Boolean {
            var err = e
            do {
                if (err is CertificateExpiredException) {
                    return true
                }
                err = err!!.cause
            } while (err != null)
            return false
        }

        private fun isPathException(e: Throwable?): Boolean {
            var err = e
            do {
                if (err is CertPathValidatorException) {
                    return true
                }
                err = err!!.cause
            } while (err != null)
            return false
        }

        private fun hexString(data: ByteArray): String {
            val si = StringBuilder()
            for (i in data.indices) {
                si.append(String.format("%02x", data[i]))
                if (i < data.size - 1) {
                    si.append(":")
                }
            }
            return si.toString()
        }

        private fun certHash(cert: X509Certificate, digest: String): String? {
            return try {
                val md = MessageDigest.getInstance(digest)
                md.update(cert.encoded)
                hexString(md.digest())
            } catch (e: CertificateEncodingException) {
                e.message
            } catch (e: NoSuchAlgorithmException) {
                e.message
            }
        }

    }
}