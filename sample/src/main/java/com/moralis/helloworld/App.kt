package com.moralis.helloworld

import com.moralis.web3.MoralisApplication
import com.moralis.web3.Moralis

const val APP_ID = "TlygdyM0oqw39Qej6J0lAOppcrNAe2sA1FfZijQQ"
const val SERVER_URL = "https://zda0u2csr0us.grandmoralis.com:2053/server"

class App: MoralisApplication() {

    override fun onCreate() {
        super.onCreate()
        Moralis.initialize(APP_ID, SERVER_URL, this)
    }
}