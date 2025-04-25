package com.kwos.dronepilotapp.data

import android.bluetooth.le.ScanResult
import android.net.wifi.ScanResult as WifiScanResult
import android.text.TextUtils
import android.util.Log
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class LogWriter(file: File) {

    private val writer: BufferedWriter = BufferedWriter(
        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
    )
    private val logQueue: BlockingQueue<String> = LinkedBlockingQueue()
    private var loggingActive = false

    init {
        val exec = Executors.newSingleThreadExecutor()
        Log.i(TAG, "Starting logging to ${file.path}")

        exec.submit {
            try {
                loggingActive = true
                var last = System.currentTimeMillis()

                // Write CSV header
                writer.write(TextUtils.join(",", LogEntry.HEADER))
                writer.write("," + OpenDroneIdParser.BasicId.csvHeader())
                writer.write(OpenDroneIdParser.BasicId.csvHeader())
                writer.write(OpenDroneIdParser.Location.csvHeader())
                writer.write(OpenDroneIdParser.SelfID.csvHeader())
                writer.write(OpenDroneIdParser.SystemMsg.csvHeader())
                writer.write(OpenDroneIdParser.OperatorID.csvHeader())
                for (i in 0 until Constants.MAX_AUTH_DATA_PAGES) {
                    writer.write(OpenDroneIdParser.Authentication.csvHeader())
                }
                writer.newLine()

                while (loggingActive) {
                    val log = try {
                        logQueue.take()
                    } catch (e: InterruptedException) {
                        break
                    }
                    writer.write(log)
                    writer.newLine()

                    val now = System.currentTimeMillis()
                    if (now - last > 1000) {
                        writer.flush()
                        last = now
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error writing log", e)
            } finally {
                try {
                    writer.flush()
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun logBluetooth(msgVersion: Int, result: ScanResult, transportType: String?, csvLog: StringBuilder?) {
        val entry = LogEntry().apply {
            session = Companion.session
            timestamp = result.timestampNanos
            this.transportType = transportType ?: ""
            macAddress = result.device.address
            this.msgVersion = msgVersion
            rssi = result.rssi
            data = result.scanRecord?.bytes ?: byteArrayOf()
            this.csvLog = csvLog ?: StringBuilder()
        }
        logQueue.add(entry.toString())
    }

    fun logNaN(msgVersion: Int, timeNano: Long, peerHash: Int, serviceSpecificInfo: ByteArray?, transportType: String?, csvLog: StringBuilder?) {
        val entry = LogEntry().apply {
            session = Companion.session
            timestamp = timeNano
            this.transportType = transportType ?: ""
            macAddress = peerHash.toString()
            this.msgVersion = msgVersion
            rssi = 0
            data = serviceSpecificInfo ?: byteArrayOf()
            this.csvLog = csvLog ?: StringBuilder()
        }
        logQueue.add(entry.toString())
    }

    fun logBeacon(msgVersion: Int, timeNano: Long, scanResult: WifiScanResult, data: ByteArray?, transportType: String?, csvLog: StringBuilder?) {
        val entry = LogEntry().apply {
            session = Companion.session
            timestamp = timeNano
            this.transportType = transportType ?: ""
            macAddress = scanResult.BSSID
            this.msgVersion = msgVersion
            rssi = scanResult.level
            this.data = data ?: byteArrayOf()
            this.csvLog = csvLog ?: StringBuilder()
        }
        logQueue.add(entry.toString())
    }

    fun close() {
        loggingActive = false
        try {
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "LogWriter"
        var session = 0
            private set

        fun bumpSession() {
            session++
        }
    }
}
