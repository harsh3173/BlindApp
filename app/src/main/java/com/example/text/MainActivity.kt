package com.example.text

//import android.support.v7.app.AppCompatActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.text.Graphic.GraphicOverlay
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var snapBtn: Button? = null
    private var detectBtn: Button? = null
    private var imageView: ImageView? = null
    private var txtView: TextView? = null
    private var imageBitmap: Bitmap? = null
    private var newBitmap: Bitmap? = null
    private val REQUEST_TAKE_PHOTO = 1
    private var rotation: Int? = 0


    private val perms = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )


    //@BindView(R.id.graphic_overlay)
    var mGraphicOverlay: GraphicOverlay? = null
    var flag: Boolean = false

    private var currentPath: String? = null


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGraphicOverlay = findViewById(R.id.graphic_overlay)
        snapBtn = findViewById(R.id.btncamera)
        detectBtn = findViewById(R.id.btndetect)
        imageView = findViewById(R.id.imageView)
        txtView = findViewById(R.id.txtview)

        EasyPermissions.requestPermissions(
            this,
            "Permission to Access Camera and Storage is essential for the Apps Functionality!",
            0,
            *perms
        )

        btncamera.setOnClickListener(View.OnClickListener { dispatchTakePictureIntent() })
        btndetect.setOnClickListener(View.OnClickListener {
            if (flag == false) {
                Toast.makeText(
                    this@MainActivity,
                    "Add an image to detect using camera!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                detectTxt()
            }

        })
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun dispatchTakePictureIntent() {
        mGraphicOverlay?.clear();
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.text.fileprovider",
                        it
                    )
                    try {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)

                    } catch (e: SecurityException) {
                        Log.d("Main", "Can't get required Permissions!!")
                        Toast.makeText(
                            this@MainActivity,
                            "Permission Denied!!\nAccess to Camera and Storage is essential for the Apps Functionality!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    //Creates a temp file for the image to store it in orignal resolution.

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        /* "JPEG_${timeStamp}_",prefix */
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPath = absolutePath
        }
    }

    private fun RotateImage(bitmap: Bitmap): Bitmap {
        var exifInterface: ExifInterface? = null
        try {
            exifInterface = ExifInterface(currentPath!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val orientation: Int? =
            exifInterface?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

        val matrix: Matrix = Matrix()
        Log.d("Main", "Orientation$orientation\n\n\n\n")
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270F)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            imageBitmap = BitmapFactory.decodeFile(currentPath)

            newBitmap = RotateImage(imageBitmap!!)
            Log.d("Main", "Inside on Activity Result")
            flag = true
            imageView?.setImageBitmap(newBitmap)
        }
    }

    private fun detectTxt() {
        val image = FirebaseVisionImage.fromBitmap(newBitmap!!)
        val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        detector.processImage(image)
            .addOnSuccessListener(OnSuccessListener<FirebaseVisionText> { firebaseVisionText ->
                Log.d("Main", "Success Listener for Firebase!")
                processTxt(firebaseVisionText)

            }).addOnFailureListener(OnFailureListener {
                Log.d("Main", "Error in Firebase")
            })
    }


    private fun processTxt(text: FirebaseVisionText) {
        val resultText = text.text

        if (resultText.isEmpty()) {
            Toast.makeText(this@MainActivity, "No Text :(", Toast.LENGTH_LONG).show()
            return
        }
        //mGraphicOverlay?.clear()
        for (block in text.textBlocks) {
            val blockText = block.text
            val blockConfidence = block.confidence
            val blockLanguages = block.recognizedLanguages
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox
            for (line in block.lines) {
                val lineText = line.text
                val lineConfidence = line.confidence
                val lineLanguages = line.recognizedLanguages
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    val elementConfidence = element.confidence
                    val elementLanguages = element.recognizedLanguages
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox

                    /*
                    val textGraphic: TextGraphic? =
                        mGraphicOverlay?.let { TextGraphic(it, element) }
                    if (textGraphic != null) {
                        mGraphicOverlay!!.add(textGraphic)
                    }

                     */

                }
            }
        }
        Log.d("Main", "${text.text} \n")
        Log.d("Main", "${text.textBlocks}")
        txtView?.text = text.text
    }


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }
}

