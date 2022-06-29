package ru.notalive.register

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.ModuleInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.notalive.register.databinding.ActivityScanningBinding
import ru.notalive.register.databinding.ActivitySendBinding
import java.lang.Exception



data class QrCode(val uid: Int, val token: String)

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

            .setTargetResolution(Size(viewCamera.width, viewCamera.height))
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
                var uid = " "
                var token = " "
                var url = " "
                val result = scanner.process(InputImage.fromMediaImage(img, rotationDegrees))
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val bounds = barcode.boundingBox
                            val corners = barcode.cornerPoints

                            val rawValue = barcode.rawValue

                            val gson = Gson()
                            // See API reference for complete list of supported types
                            try {
                                val data = gson.fromJson(rawValue, QrCode::class.java)

                                url = data.toString()
                                uid = data.uid.toString()
                                token = data.token
                                setResult(1)
                                Log.d("fuck", "${imageProxy.height} x ${imageProxy.width}")
                            } catch ( exc: Exception){
                                setResult(-1)
                            }

                        }

                    }
                if(result.isSuccessful){
                    Log.d("fuck", "${imageProxy.height} x ${imageProxy.width}")
                    Toast.makeText(this, url, Toast.LENGTH_LONG).show()
                    val i = Intent(this@ScanningActivity, SendActivity::class.java)
                    i.putExtra("barcodeDataUid", uid)
                    i.putExtra("barcodeDataToken", token)
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


    @SuppressLint("ClickableViewAccessibility")
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

                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preView) // todo задать для распознования лайфцикл сюда
                viewCamera.setOnTouchListener(View.OnTouchListener { view: View, motionEvent: MotionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> return@OnTouchListener true
                        MotionEvent.ACTION_UP -> {
                            // Get the MeteringPointFactory from PreviewView
                            val factory = viewCamera.meteringPointFactory

                            // Create a MeteringPoint from the tap coordinates
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)

                            // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                            val action = FocusMeteringAction.Builder(point).build()

                            // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                            // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                            camera.cameraControl.startFocusAndMetering(action)

                            return@OnTouchListener true
                        }
                        else -> return@OnTouchListener false
                    }
                })

            } catch (exc: Exception){
                Log.e(TAG, "Failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun start_recognize(){

    }

    private fun draw_rect(canvas: Canvas, rect:Rect){
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        canvas.drawRect(rect, paint)
    }
}