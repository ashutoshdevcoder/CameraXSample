package com.ashdev.cameraxsample

import android.Manifest
import android.R.attr.value
import android.R.attr.valueFrom
import android.R.attr.valueTo
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.ashdev.cameraxsample.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var mDefaultCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var filesToBeStored: File
    private var currentFileUri: Uri? = null
    private lateinit var outputFileOptions: ImageCapture.OutputFileOptions
    private var controller: LifecycleCameraController? = null

    private val visible = View.VISIBLE
    private val invisible = View.INVISIBLE

    private var typeOfImageProcess = 1
    private var cameraDialog: Dialog? = null
    private lateinit var cleverTapObject: HashMap<String, Any>


    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
           initVar()
            bindCameraUseCases()
        } else {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onStart() {
        super.onStart()
        allPermissionsGranted()
    }
    private fun initVar() {
        filesToBeStored = File(cacheDir, "images")
        if (!filesToBeStored.exists()) {
            filesToBeStored.mkdirs()
        }

        mDefaultCameraSelector =
            if (typeOfImageProcess == 0) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

    }
    override fun onDestroy() {
        controller?.unbind()
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Request permissions
        if (allPermissionsGranted()) {
            initVar()
            bindCameraUseCases()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        setupClickListeners()

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("RestrictedApi")
    private fun setupClickListeners() {
        binding.rejectButton.setOnClickListener {
            currentFileUri = null
            binding.previewView.visibility = visible
            binding.previewImage.visibility = invisible
            binding.previewImage.setImageURI(null)
            binding.flipLens.visibility = visible
            binding.shutterButton.visibility = visible
            binding.acceptButton.visibility = invisible
            binding.rejectButton.visibility = invisible
            currentFileUri?.toFile()?.delete()
        }

        binding.flipLens.setOnClickListener {
            val temp =
                if (mDefaultCameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            if (controller?.hasCamera(temp) == true){
                mDefaultCameraSelector = temp
                controller?.cameraSelector = temp
            }

        }
        binding.acceptButton.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra("imageUri", currentFileUri))
            binding.previewView.visibility = visible
            finish()
        }

        binding.shutterButton.setOnClickListener {
            if (controller == null) {
                allPermissionsGranted()
                return@setOnClickListener
            }
            //if (!binding.tvStatus.text.toString().equals(String.format(getString(R.string.perfect_just_smile), String(Character.toChars(0x1F642))),true)) return@setOnClickListener

            initImageCapture()
            vibratePhone()
            binding.flipLens.visibility = invisible
            controller?.takePicture(
                outputFileOptions,
                Executors.newSingleThreadExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        runOnUiThread {
                            binding.previewView.visibility = invisible
                        }

                        outputFileResults.savedUri?.let {
                            val f = it.toFile()
                            if (f.exists()) {
                                currentFileUri = it
                                binding.previewImage.post {
                                    val exif = ExifInterface(it.toFile().absolutePath)
                                    val orientation = exif.getAttributeInt(
                                        ExifInterface.TAG_ORIENTATION,
                                        ExifInterface.ORIENTATION_UNDEFINED
                                    )
                                    binding.previewImage.rotation =
                                        getRotationFromOrientation(orientation)
                                    binding.previewImage.visibility = visible
                                    Glide.with(applicationContext)
                                        .load(it)
                                        .override(
                                            binding.previewImage.width,
                                            binding.previewImage.height
                                        )
                                        //.apply(RequestOptions().transform(MyRotation(orientation)))
                                        .sizeMultiplier(.8f)
                                        .into(binding.previewImage)
//                                    val bm = rotateBitmap(BitmapFactory.decodeFile(it.toFile().absolutePath),orientation, it)
//                                    binding.previewImage.visibility = visible
//                                    if (bm != null) {
//                                        binding.previewImage.setImageBitmap(bm)
//                                    }else{
//                                        binding.previewImage.setImageURI(it)
//                                    }
                                    binding.shutterButton.visibility = invisible
                                    binding.acceptButton.visibility = visible
                                    binding.rejectButton.visibility = visible
                                }
                            } else {
                                currentFileUri = null
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {

                        runOnUiThread {
                            binding.rejectButton.performClick()
                            Toast.makeText(this@MainActivity,"Unable to capture image", Toast.LENGTH_LONG).show()
                        }
                    }
                })
        }
    }
    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibManager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator())
            return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun initImageCapture() {

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = mDefaultCameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(
                File(
                    filesToBeStored,
                    "photo_${System.currentTimeMillis()}.jpg"
                )
            )
                .setMetadata(metadata)
                .build()
    }

    private fun bindCameraUseCases() {
        binding.root.post {
            LifecycleCameraController(this).apply {
                controller?.unbind()
                unbind()

                this.initializationFuture.addListener({
                    try {

                        this.isTapToFocusEnabled = true
//                        this.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
//                        clearImageAnalysisAnalyzer()

                        bindToLifecycle(this@MainActivity)
                        controller = this
                        binding.previewView.controller = this
                        if(hasCamera(mDefaultCameraSelector)) {
                            this.cameraSelector = mDefaultCameraSelector
                        }
                        binding.flipLens.visibility = visible
                    } catch (e: Exception){
                        Toast.makeText(this@MainActivity,"Camera not found", Toast.LENGTH_LONG).show()
                    }
                }, ContextCompat.getMainExecutor(this@MainActivity))
            }

            if (typeOfImageProcess == 1) {
                //binding.tvStatus.visibility = visible
                if (binding.rejectButton.isVisible.not())
                    binding.shutterButton.visibility = visible
            } else {
                binding.tvStatus.visibility = invisible
                binding.shutterButton.visibility = invisible
                binding.rejectButton.visibility = invisible
                binding.acceptButton.visibility = invisible
                binding.previewImage.visibility = invisible
            }
        }
    }
    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

fun getRotationFromOrientation(orientation: Int) = (when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}).toFloat()

