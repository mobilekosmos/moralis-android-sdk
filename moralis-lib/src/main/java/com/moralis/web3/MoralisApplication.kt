package com.moralis.web3

import android.app.Application
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.moralis.web3.walletconnect.server.BridgeServer
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.*
import org.walletconnect.nullOnThrow
import java.io.File
import java.util.*

open class MoralisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    fun init() {
        initMoshi()
        initClient()
        initBridge()
        initSessionStorage()
        appName = getApplicationName()
        appPackage = applicationInfo.packageName
    }

    private fun getApplicationName(): String {
        val stringId: Int = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else getString(stringId)
    }
    private fun initClient() {
        client = OkHttpClient.Builder().build()
    }

    private fun initMoshi() {
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    private fun initBridge() {
        bridge = BridgeServer(moshi)
        bridge.start()
    }

    private fun initSessionStorage() {
        storage = FileWCSessionStore(File(cacheDir, "session_store.json").apply { createNewFile() }, moshi)
    }

    companion object {
        private lateinit var client: OkHttpClient
        private lateinit var moshi: Moshi
        private lateinit var bridge: BridgeServer
        private lateinit var storage: WCSessionStore
        private lateinit var appName: String
        private lateinit var appPackage : String
        lateinit var config: Session.FullyQualifiedConfig
        lateinit var session: Session

        fun resetSession() {
            nullOnThrow { session }?.clearCallbacks()
            val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
            config = Session.FullyQualifiedConfig(UUID.randomUUID().toString(), "http://localhost:${BridgeServer.PORT}", key)
            session = WCSession(
                    config,
                    MoshiPayloadAdapter(moshi),
                    storage,
                    OkHttpTransport.Builder(client, moshi),
                    Session.PeerMeta(
                        url = appPackage,
                        name = appName,
                        description = appName,
                        icons = listOf("https://www.google.com/favicon.ico")
                    )
            )
            session.offer()
        }
    }
}