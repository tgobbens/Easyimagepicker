package nl.esites.easyimagepicker


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import nl.esites.easyimagepicker.databinding.SelectPickerDialogBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class EasyImagePicker private constructor(builder: Builder, savedInstanceState: Bundle?){

    companion object {
        const val DEFAULT_CAMERA_REQUEST_CODE = 21251
        const val DEFAULT_GALLERY_REQUEST_CODE = 21252

        const val CURRENT_PHOTO_PATH_KEY = "CURRENT_PHOTO_PATH_KEY"
        const val CURRENT_PHOTO_DISPLAY_NAME_KEY = "CURRENT_PHOTO_DISPLAY_NAME_KEY"
        const val DEFAULT_MAX_IMAGE_DIMENSION = 1024
    }

    enum class MODE {
        GALLERY_ONLY,
        CAMERA_ONLY,
        BOTH
    }

    private val mode: MODE
    private val requestCameraCode: Int
    private val requestGalleryCode: Int
    private val maxImageDimension: Int
    @StyleRes
    private val themeResId: Int

    private var currentPhotoPath: String? = null
    private var currentPhotoDisplayName: String? = null

    init {
        mode = builder.mode
        requestCameraCode = builder.cameraRequestCode
        requestGalleryCode = builder.galleryRequestCode
        maxImageDimension = builder.maxImageDimension
        themeResId = builder.themeResId

        currentPhotoPath = savedInstanceState?.getString(CURRENT_PHOTO_PATH_KEY, null)
        currentPhotoDisplayName = savedInstanceState?.getString(CURRENT_PHOTO_DISPLAY_NAME_KEY, null)
    }

    class Builder {
        var mode: MODE = MODE.BOTH
            private set

        var cameraRequestCode: Int = DEFAULT_CAMERA_REQUEST_CODE
            private set

        var galleryRequestCode: Int = DEFAULT_GALLERY_REQUEST_CODE
            private set

        var maxImageDimension: Int = DEFAULT_MAX_IMAGE_DIMENSION
            private set

        @StyleRes
        var themeResId: Int = 0
            private set

        fun mode(mode: MODE) =
            apply { this.mode = mode }

        fun themeResId(@StyleRes themeResId: Int) =
            apply { this.themeResId = themeResId }

        fun create(savedInstanceState: Bundle?): EasyImagePicker {
            return create(savedInstanceState, DEFAULT_CAMERA_REQUEST_CODE, DEFAULT_GALLERY_REQUEST_CODE)
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun create(savedInstanceState: Bundle?,
                   cameraRequestCode: Int,
                   galleryRequestCode: Int
        ) : EasyImagePicker {
            this.cameraRequestCode = cameraRequestCode
            this.galleryRequestCode = galleryRequestCode
            return EasyImagePicker(this, savedInstanceState)
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        currentPhotoPath?.let {
            outState.putString(CURRENT_PHOTO_PATH_KEY, it)
        }

        currentPhotoDisplayName?.let {
            outState.putString(CURRENT_PHOTO_DISPLAY_NAME_KEY, it)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity): Boolean {
        @Suppress("unused")
        if (this.requestCameraCode == requestCode && resultCode == Activity.RESULT_OK) {
            val imagePath = currentPhotoPath  ?: return false
            val outputPath = currentPhotoPath  ?: return false

            return try {
                ImageCompressor.compressImageWithPath(imagePath, outputPath, maxImageDimension)
            } catch (e: Exception) {
                return false
            }
        } else if (this.requestGalleryCode == requestCode && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri = data?.data ?: return false

            try {
                createImageFile(activity)
            } catch (e: Exception) {
                return false
            }

            val outputPath = currentPhotoPath  ?: return false

            return try {
                ImageCompressor.compressImageWithResolver(imageUri, outputPath, activity.contentResolver, maxImageDimension)
            } catch (e: Exception) {
                false
            }
        }

        return false
    }

    fun getResultImageUri(): Uri? {
        return currentPhotoPath?.toUri()
    }

    @Suppress("unused")
    fun start(activity: Activity): EasyImagePicker {
        when (mode) {
            MODE.BOTH ->showPickerDialog(ActivityStarter(activity))
            MODE.GALLERY_ONLY -> startGallery(ActivityStarter(activity))
            MODE.CAMERA_ONLY -> startCamera(ActivityStarter(activity))
        }

        return this
    }

    @Suppress("unused")
    fun start(fragment: Fragment): EasyImagePicker {
        when (mode) {
            MODE.BOTH ->showPickerDialog(FragmentStarter(fragment))
            MODE.GALLERY_ONLY -> startGallery(FragmentStarter(fragment))
            MODE.CAMERA_ONLY -> startCamera(FragmentStarter(fragment))
        }

        return this
    }

    private fun showPickerDialog(starter: ActivityStarterInterface) {
        val layoutInflater = LayoutInflater.from(starter.getContext())
        val view = SelectPickerDialogBinding.inflate(layoutInflater)

        val dialogBuilder = if (themeResId == 0) {
            AlertDialog.Builder(starter.getContext())
        } else {
            AlertDialog.Builder(starter.getContext(), themeResId)
        }

        val dialog = dialogBuilder
            .setTitle(R.string.eip_dialog_title)
            .setView(view.root)
            .setNegativeButton(R.string.eip_dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        view.selectCameraRegion.setOnClickListener {
            startCamera(starter)
            dialog.dismiss()
        }

        view.selectGalleryRegion.setOnClickListener {
            startGallery(starter)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startGallery(activityStarterInterface: ActivityStarterInterface) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activityStarterInterface.startActivityForResult(intent, requestGalleryCode)
    }

    private fun startCamera(activityStarterInterface: ActivityStarterInterface) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(activityStarterInterface.getContext().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile(activityStarterInterface.getContext())
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        activityStarterInterface.getContext(),
                        "${activityStarterInterface.getContext().packageName}.EasyImagePickerFileProvider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                    activityStarterInterface.startActivityForResult(takePictureIntent, requestCameraCode)
                }
            }
        }
    }

    private fun createImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val prefix = "image_${timeStamp}_"
        currentPhotoDisplayName = prefix
        val storageDir: File =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw IOException()

        return File.createTempFile(prefix, ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }
}