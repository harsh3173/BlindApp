package com.example.text

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
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import kotlinx.android.synthetic.main.activity_text.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ObjectActivity : AppCompatActivity() {

    private var snapBtn: Button? = null
    private var detectBtn: Button? = null
    private var imageView: ImageView? = null
    private var txtView: TextView? = null
    private var imageBitmap: Bitmap? = null
    private var newBitmap: Bitmap? = null
    private val REQUEST_TAKE_PHOTO = 1
    private var rotation: Int? = 0


    //@BindView(R.id.graphic_overlay)
    var mGraphicOverlay: GraphicOverlay? = null
    var flag: Boolean = false

    private var currentPath: String? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object)

        mGraphicOverlay = findViewById(R.id.graphic_overlay)
        snapBtn = findViewById(R.id.btncamera)
        detectBtn = findViewById(R.id.btndetect)
        imageView = findViewById(R.id.imageView)
        txtView = findViewById(R.id.txtview)


        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Object Detection"

        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        supportActionBar?.setDisplayShowHomeEnabled(true);

        btncamera.setOnClickListener(View.OnClickListener { dispatchTakePictureIntent() })
        btndetect.setOnClickListener(View.OnClickListener {
            if (flag == false) {
                Toast.makeText(
                    this,
                    "Add an image to detect using camera!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                detectObject()
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
                        this@ObjectActivity,
                        "com.example.text.fileprovider",
                        it
                    )
                    try {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)

                    } catch (e: SecurityException) {
                        Log.d("Main", "Can't get required Permissions!!")
                        Toast.makeText(
                            this@ObjectActivity,
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

    private fun detectObject() {
        //val image = FirebaseVisionImage.fromBitmap(newBitmap!!)
        val image = FirebaseVisionImage.fromBitmap(newBitmap!!)

        //val labeler = FirebaseVision.getInstance().onDeviceImageLabeler

        val options = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler(options)

        //val labelercloud = FirebaseVision.getInstance().cloudImageLabeler

        labeler.processImage(image)
            .addOnSuccessListener { labels ->
                txtView?.text = "\n"
                for (label in labels) {
                    val text = label.text
                    val entityId = label.entityId
                    val confidence = label.confidence
                    Log.d("Main", "Label: $label\n\n$text\n$confidence\n\n")
                    txtView?.append(label.text + ":\t" + label.confidence + "\n")
                }
            }
            .addOnFailureListener { e ->
                Log.d("Main", "Failed in Image Labeler")
                Log.e("Main", e.toString())
            }


    }


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }


}
