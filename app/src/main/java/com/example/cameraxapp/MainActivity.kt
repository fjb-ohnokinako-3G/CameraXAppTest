package com.example.cameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// IDEが自動インポートする場合もありますが
// それぞれ実装が異なる場合があるので、曖昧さを無くすためにここに列挙します
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.PreviewConfig.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.TimeUnit

// パーミッションを要求するときのリクエストコード番号です
// 複数のContextからパーミッションが要求された時にどこから要求されたかを区別するために使います
private const val REQUEST_CODE_PERMISSIONS = 10

// 要求する全パーミッションの配列
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // onCreate()の最後に以下を追加
        //Unresolved referenceは、Alt Enterで
        viewFinder = findViewById(R.id.view_finder)

        // カメラパーミッションの要求
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // texture viewが変化した時にLayoutの再計算を行う
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }





    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        // TODO: CameraXの処理を実装します


            // viewfinder use caseのコンフィグレーションオブジェクトを生成
            val previewConfig = Builder().apply {
                setTargetAspectRatio(Rational(1, 1))
                setTargetResolution(Size(640, 640))
            }.build()

            // viewfinder use caseの生成
            val preview = Preview(previewConfig)

            // viewfinderが更新されたらLayoutを再計算
            preview.setOnPreviewOutputUpdateListener {

                // SurfaceTextureの更新して再度親Viewに追加する
                val parent = viewFinder.parent as ViewGroup
                parent.removeView(viewFinder)
                parent.addView(viewFinder, 0)

                viewFinder.surfaceTexture = it.surfaceTexture
                updateTransform()

                // image capture use caseのコンフィグレーションオブジェクトを生成
                val imageCaptureConfig = ImageCaptureConfig.Builder()
                    .apply {
                        setTargetAspectRatio(Rational(1, 1))
                        // イメージキャプチャの解像度は設定しない。
                        // 代わりに、アスペクト比と要求されたモードに基づいて
                        // 適切な解像度を推測するキャプチャモードを選択します。
                        setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                    }.build()

                // image capture use caseの生成とボタンのClickリスナーの登録
                val imageCapture = ImageCapture(imageCaptureConfig)
                findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
                    val file = File(externalMediaDirs.first(),
                        "${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(file,
                        object : ImageCapture.OnImageSavedListener {
                            override fun onError(error: ImageCapture.UseCaseError,
                                                 message: String, exc: Throwable?) {
                                val msg = "Photo capture failed: $message"
                                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                                Log.e("CameraXApp", msg)
                                exc?.printStackTrace()
                            }

                            override fun onImageSaved(file: File) {
                                val msg = "Photo capture succeeded: ${file.absolutePath}"
                                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                                Log.d("CameraXApp", msg)
                            }
                        })
                }

            // use caseをlifecycleにバインドする
                CameraX.bindToLifecycle(this, preview, imageCapture)
        }
    }

    private fun updateTransform() {
        // TODO: カメラのビューファインダーが変化した時の処理を実装します

            val matrix = Matrix()

            // view finderの中心の計算
            val centerX = viewFinder.width / 2f
            val centerY = viewFinder.height / 2f

            // 表示回転を考慮したプレビュー出力
            val rotationDegrees = when(viewFinder.display.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> return
            }
            matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

            // TextureViewへのセット
            viewFinder.setTransform(matrix)

    }

    /**
     * カメラのパーミッションリクエストダイアログ処理の結果を確認します
     * パーミッションが許可された場合はCameraを開始します
     * そうでない場合はToastを表示して終了します
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "パーミッションが許可されませんでした",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * 定義されたパーミッションをチェックします
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it) == PackageManager.PERMISSION_GRANTED
    }


}
