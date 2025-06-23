package com.kwos.dronepilotapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.nio.ByteBuffer
import java.text.DecimalFormat

class NDFilterAssistantActivity : AppCompatActivity() {

    // private lateinit var surfaceView: SurfaceView
    private lateinit var textureView: TextureView
    private lateinit var fpsSpinner: Spinner
    private lateinit var shutterTextView: TextView
    private lateinit var isoTextView: TextView
    private lateinit var suggestionTextView: TextView
    private lateinit var luminanceValueText: TextView
    private lateinit var closeButton: Button

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var currentExposureNs: Long = 20_000_000L // default 1/50s
    private val fixedIso = 100

    private lateinit var imageReader: ImageReader
    //private val readerWidth = 320
    //private val readerHeight = 240
    private val readerWidth = 160
    private val readerHeight = 120

    private lateinit var skyConditionSpinner: Spinner
    private var skyConditionFactor: Double = 1.0
    private var ndOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nd_filter_assistant)

        textureView = findViewById(R.id.previewTexture)
        fpsSpinner = findViewById(R.id.fpsSpinner)
        shutterTextView = findViewById(R.id.shutterValueText)
        isoTextView = findViewById(R.id.isoValueText)
        suggestionTextView = findViewById(R.id.ndSuggestionText)
        luminanceValueText = findViewById(R.id.luminanceValueText)
        closeButton = findViewById(R.id.close_ndfilter_button)

        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()

        closeButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        val fpsOptions = listOf("24", "25", "30", "50", "60")
        val adapter = ArrayAdapter(this, R.layout.spinner_item_yellow, fpsOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fpsSpinner.adapter = adapter

        fpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val fps = fpsOptions[position].toInt()
                val shutterIdeal = 1.0 / (fps * 2)
                val df = DecimalFormat("#.###")
                shutterTextView.text = "Shutter ideale: 1/${fps * 2} ≈ ${df.format(shutterIdeal)}s"

                // Simula esposizione più corta per FPS alti
                currentExposureNs = when (fps) {
                    in 24..25 -> 20_000_000L  // 1/50
                    30 -> 16_666_666L         // 1/60
                    50 -> 10_000_000L         // 1/100
                    60 -> 8_333_333L          // 1/120
                    else -> 20_000_000L
                }

                // Aggiorna la preview se già avviata
                if (::previewRequestBuilder.isInitialized && captureSession != null) {
                    previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposureNs)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                }

                // offset logico ND (opzionale, puoi anche rimuoverlo se l'effetto è reale)
                ndOffset = 0
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }



        // condizioni del cielo
        skyConditionSpinner = findViewById(R.id.skyConditionSpinner)

        val skyConditions = listOf("☀️ Sole pieno", "⛅ Qualche nuvola", "☁️ Cielo coperto")
        val skyAdapter = ArrayAdapter(this, R.layout.spinner_item_white, skyConditions)
        skyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        skyConditionSpinner.adapter = skyAdapter

        skyConditionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                skyConditionFactor = when (position) {
                    0 -> 1.0  // Sole pieno
                    1 -> 1.30 // Qualche nuvola
                    2 -> 1.60 // Cielo coperto
                    else -> 1.0
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (textureView.isAvailable) {
            openCameraWithSurface(textureView.surfaceTexture!!)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCameraWithSurface(surface)
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            }
        }

    }

    private fun openCameraWithSurface(surfaceTexture: SurfaceTexture) {
        val cameraId = cameraManager.cameraIdList.first {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
            return
        }

        imageReader = ImageReader.newInstance(readerWidth, readerHeight, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            image.close()
            val avgY = data.map { it.toInt() and 0xFF }.average()
            runOnUiThread { updateExposureFeedback(avgY) }
        }, backgroundHandler)

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                if (cameraDevice != null && ::imageReader.isInitialized) {
                    val surface = Surface(surfaceTexture)
                    startPreviewWithSurface(surface)
                }
            }
            override fun onDisconnected(device: CameraDevice) { device.close() }
            override fun onError(device: CameraDevice, error: Int) { device.close() }
        }, backgroundHandler)
    }

    private fun startPreviewWithSurface(previewSurface: Surface) {
        val imageSurface = imageReader.surface
        cameraDevice?.let { device ->
            captureSession?.close()
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                addTarget(imageSurface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF)
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                set(CaptureRequest.SENSOR_SENSITIVITY, fixedIso)
                set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposureNs)
                set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f)
            }
            device.createCaptureSession(listOf(previewSurface, imageSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, backgroundHandler)
        }
    }

    private fun startThrottledPreview(session: CameraCaptureSession) {
        backgroundHandler?.post(object : Runnable {
            override fun run() {
                try {
                    previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposureNs)
                    session.capture(previewRequestBuilder.build(), null, backgroundHandler)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
                backgroundHandler?.postDelayed(this, 125)
            }
        })
    }

    private fun updateExposureFeedback(avgY: Double) {
        val suggestion = ndSuggestionFromLuminance(avgY)
        suggestionTextView.text = suggestion
        luminanceValueText.text = "Luminanza: ${"%.2f".format(avgY)}"
    }

    private fun ndSuggestionFromLuminance(avgY: Double): String {
        val adjustedY = avgY / skyConditionFactor

        val ndIndex = when {
            adjustedY < 90 -> 0  // nessun filtro
            adjustedY < 130 -> 1  // ND8
            adjustedY < 180 -> 2 // ND16
            adjustedY < 250 -> 3 // ND32
            else -> 4            // ND64 o più
        }

        val correctedIndex = (ndIndex + ndOffset).coerceIn(0, 4)

        return when (correctedIndex) {
            0 -> "💡 Nessun filtro necessario o quello attuale va bene"
            1 -> "🌤️ ND8 o ND4 consigliati"
            2 -> "☀️ ND8 suggerito o ND16"
            3 -> "🌞 Almeno ND32 necessario"
            else -> "🔥 Serve un ND64 o più!"
        }
    }



    override fun onResume() {
        super.onResume()
        backgroundThread = HandlerThread("Camera2Background").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    override fun onPause() {
        super.onPause()
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }
}
