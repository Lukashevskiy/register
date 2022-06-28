package ru.notalive.register

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.ModuleInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.notalive.register.databinding.ActivityScanningBinding
import ru.notalive.register.databinding.ActivitySendBinding
import java.lang.Exception

class ScanningActivity : AppCompatActivity() {
    lateinit var viewCamera: PreviewView;
    lateinit var binding: ActivityScanningBinding
    lateinit var imageAnalysis: ImageAnalysis

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewCamera = binding.viewCamera

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PackageManager.PERMISSION_GRANTED)
        }

        imageAnalysis = ImageAnalysis.Builder()
            // enable the following line if RGBA output is needed.
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .build()
        val scanner = BarcodeScanning.getClient()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ImageAnalysis.Analyzer { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // insert your code here.
            val img = imageProxy.image
            if(img != null){
                val result = scanner.process(InputImage.fromMediaImage(img, rotationDegrees))
                    .addOnSuccessListener { barcodes ->
                        var url = ""
                        for (barcode in barcodes) {
                            val bounds = barcode.boundingBox
                            val corners = barcode.cornerPoints

                            val rawValue = barcode.rawValue

                            val valueType = barcode.valueType
                            // See API reference for complete list of supported types
                            when (valueType) {
                                Barcode.TYPE_URL -> {
                                    val title = barcode.url!!.title
                                    url = barcode.url!!.url.toString()
                                }
                            }

                        }
                        Toast.makeText(this, url, Toast.LENGTH_LONG).show()

                        val i = Intent(this@ScanningActivity, SendActivity::class.java)
                        i.putExtra("barcodeData", url)
                        startActivity(i)

                    }

            }

            // after done, release the ImageProxy object
            imageProxy.close()
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PackageManager.PERMISSION_GRANTED -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startCamera()
                } else {
                    Toast.makeText(this, "Camera didn't start", Toast.LENGTH_SHORT).show()
                }
                return
            }
            PackageManager.PERMISSION_DENIED -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PackageManager.PERMISSION_GRANTED)
            }
            else -> {
                Toast.makeText(this, "This app need to use your camera to recognize QR-code", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preView = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewCamera.surfaceProvider)
                }


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preView) // todo задать для распознования лайфцикл сюда
            } catch (exc: Exception){
                Log.e(TAG, "Failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun start_recognize(){

    }
}