package com.kwos.dronepilotapp.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

object WearMessageSender {

    private const val TAG = "DronePilotApp"

    fun send(context: Context, path: String, message: String) {

        Log.d(TAG, "send() called → path=$path msg=$message")

        val nodeClient = Wearable.getNodeClient(context)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->

                Log.d(TAG, "Connected nodes: ${nodes.size}")

                if (nodes.isEmpty()) {
                    Log.e(TAG, "⚠️ NESSUN WATCH CONNESSO")
                }

                nodes.forEach { node ->
                    Log.d(TAG, "Invio a nodeId=${node.id}, name=${node.displayName}")

                    Wearable.getMessageClient(context)
                        .sendMessage(
                            node.id,
                            path,
                            message.toByteArray()
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Messaggio inviato a ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Errore invio", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Errore get connectedNodes", e)
            }
    }
}
