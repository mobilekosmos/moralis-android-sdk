package com.moralis.web3

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.walletconnect.Session

class MoralisWeb3Transaction {
    companion object {
        private const val TAG = "Transaction"
        private var mTxRequest: Long? = null

        fun transfer(
            transferObject: TransferObject,
            context: Context,
            moralisAuthCallback: MoralisTransferCallback
        ) {

            val user = MoralisUser.getCurrentUser()
            val accounts = (user.get("accounts") as ArrayList<*>)
            val sender = accounts[0]?.toString() ?: return

//            val transferOperation
//            val customToken
//
//            if (transferObject.mType != TransferType.NATIVE) {
//                customToken = new web3 . eth . Contract (TransferUtils.abi[type], contractAddress)
//            }

            val id = System.currentTimeMillis()

            when (transferObject.mType) {
                TransferType.NATIVE -> {
                    val obj = transferObject as TransferObject.TransferObjectNATIVE
                    val transaction = Session.MethodCall.SendTransaction(
                        id = id,
                        from = sender,
                        to = obj.mTransactionReceiver,
                        nonce = null, // Optional
                        gasPrice = null, // Optional
                        gasLimit = null, // Optional
                        value = obj.mAmount,
                        data = "" // Required
                    )
                    // TODO: maybe use Sign Typed Data v4 instead?
                    MoralisApplication.session.performMethodCall(
                        transaction
                    ) {
                        handleTransferResponse(it, moralisAuthCallback)
                    }
                    mTxRequest = id
                    navigateToWallet(context)
                }

                TransferType.ERC20 -> {
                    val obj = transferObject as TransferObject.TransferObjectERC20
                    val transaction = Session.MethodCall.SendTransaction(
                        id = id,
                        from = sender,
                        to = obj.mTransactionReceiver,
                        nonce = null, // Optional
                        gasPrice = null, // Optional
                        gasLimit = null, // Optional
                        value = obj.mAmount,
                        data = Web3TransactionUtils.encodeTransferData(
                            obj.mTransactionReceiver,
                            obj.mAmount.toBigInteger()
                        )
                    )
                    // TODO: maybe use Sign Typed Data v4 instead?
                    MoralisApplication.session.performMethodCall(
                        transaction
                    ) {
                        handleTransferResponse(it, moralisAuthCallback)
                    }
                    mTxRequest = id
                    navigateToWallet(context)
                }
                TransferType.ERC721 -> {
                    // TODO: wait for walletconnect 2.0
                }
                TransferType.ERC1155 -> {
                    // TODO: wait for walletconnect 2.0
                }
            }

//            if (awaitReceipt) return transferOperation;
//
//            transferOperation
//                .on('transactionHash', hash => {
//                    transferEvents.emit('transactionHash', hash);
//                })
//            .on('receipt', receipt => {
//                transferEvents.emit('receipt', receipt);
//            })
//            .on('confirmation', (confirmationNumber, receipt) => {
//                transferEvents.emit('confirmation', (confirmationNumber, receipt));
//            })
//            .on('error', error => {
//                transferEvents.emit('error', error);
//                throw error;
//            });
//
//            return transferEvents;
        }

        private fun handleTransferResponse(
            response: Session.MethodCall.Response,
            moralisTransferCallback: MoralisTransferCallback,
        ) {
            if (response.id == mTxRequest) {
                mTxRequest = null
                Log.d(TAG, "Transfer done")
                if (response.error != null) {
                    Log.e(TAG, "Transaction error: ${response.error}")
                    // TODO: analyze if ".error!!" is right.
                    moralisTransferCallback.onError(response.error!!.message)
                } else {
                    moralisTransferCallback.onResponse(response.result)
//                    moralisAuthCallback.onConfirmation()
//                    moralisAuthCallback.onReceipt()
//                    moralisAuthCallback.onTransactionHash(null)
                }
            }
        }

        private fun navigateToWallet(context: Context) {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("wc:")
            context.startActivity(i)
        }
    }

    enum class TransferType {
        NATIVE, ERC20, ERC721, ERC1155
    }

    sealed class TransferObject(
        val mType: TransferType,
        val mSystem: String = "evm",
        val mAwaitReceipt: Boolean = true,
        val mTransactionReceiver: String
    ) {
        class TransferObjectNATIVE(
            system: String = "evm",
            awaitReceipt: Boolean = true,
            amountToTransfer: String,
            receiver: String
        ) : TransferObject(TransferType.NATIVE, system, awaitReceipt, receiver) {
            // does not work on TrustWallet if not converting to BigInteger.
            val mAmount: String = MoralisUnitConverter.convertETHToWei(amountToTransfer)

//            val mAmount : String = MoralisUnitConverter.convertETHToWeiHex(amountToTransfer) // works on TrustWallet
//            val mAmount : String = "0x5AF3107A4000" // works on TrustWallet
            //                        val mAmount = "5500000000000000000" // works on TrustWallet
//            val mAmount = "0x4C53ECDC18A60000" // works but freezes in metamask, works on TrustWallet
        }

        class TransferObjectERC20(
            system: String = "evm",
            awaitReceipt: Boolean = true,
            amountToTransfer: String,
            val contractAddress: String,
            receiver: String,
        ) : TransferObject(TransferType.ERC20, system, awaitReceipt, receiver) {
            val mAmount: String = MoralisUnitConverter.convertETHToWei(amountToTransfer)
        }

        class TransferObjectERC721(
            system: String = "evm",
            awaitReceipt: Boolean = true,
            val contractAddress: String,
            receiver: String,
        ) : TransferObject(TransferType.ERC721, system, awaitReceipt, receiver)

        class TransferObjectERC1155(
            system: String = "evm",
            awaitReceipt: Boolean = true,
            val contractAddress: String,
            receiver: String,
            val tokenId: String,
        ) : TransferObject(TransferType.ERC1155, system, awaitReceipt, receiver)
    }

    interface MoralisTransferCallback {
        //        fun onTransactionHash(accounts: List<String>?)
//        fun onReceipt()
//        fun onConfirmation()
        fun onError(message: String)
        fun onResponse(result: Any?)
    }

}