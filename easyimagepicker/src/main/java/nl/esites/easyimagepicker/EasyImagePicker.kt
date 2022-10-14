package nl.esites.easyimagepicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import nl.esites.easyimagepicker.databinding.SelectPickerDialogBinding
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*


class EasyImagePicker private constructor(
    builder: Builder,
    savedInstanceState: Bundle?,
    activity: AppCompatActivity?,
    fragment: Fragment?,
    private val callback: (Uri?) -> Unit,
) {
    companion object {
        const val CURRENT_PHOTO_PATH_KEY = "CURRENT_PHOTO_PATH_KEY"
        const val CURRENT_PHOTO_DISPLAY_NAME_KEY = "CURRENT_PHOTO_DISPLAY_NAME_KEY"
        const val DEFAULT_MAX_IMAGE_DIMENSION = 1024
        const val DEFAULT_COMPRESSION_QUALITY = 90
    }

    /**
     * BOTH will show a dialog before showing the gallery or camera
     * GALLERY_ONLY and CAMERA_ONLY will directly show
     */
    enum class MODE {
        GALLERY_ONLY,
        CAMERA_ONLY,
        BOTH
    }

    private val maxImageDimension: Int
    private val compressionQuality: Int

    @StyleRes
    private val themeResId: Int

    private var currentPhotoPath: String? = null
    private var currentPhotoDisplayName: String? = null

    private var activityStarter: ActivityStarterInterface

    init {
        maxImageDimension = builder.maxImageDimension
        compressionQuality = builder.compressionQuality
        themeResId = builder.themeResId

        currentPhotoPath = savedInstanceState?.getString(CURRENT_PHOTO_PATH_KEY, null)
        currentPhotoDisplayName = savedInstanceState?.getString(
            CURRENT_PHOTO_DISPLAY_NAME_KEY,
            null
        )

        activityStarter = when {
            fragment != null -> {
                getActivityStarter(fragment)
            }
            activity != null -> {
                getActivityStarter(activity)
            }
            else -> {
                throw IllegalArgumentException("no fragment or activity provided")
            }
        }
    }

    class Builder {

        var maxImageDimension: Int = DEFAULT_MAX_IMAGE_DIMENSION
            private set

        var compressionQuality: Int = DEFAULT_COMPRESSION_QUALITY
            private set

        @StyleRes
        var themeResId: Int = 0
            private set

        /**
         * set the max image dimension for the resulting image
         */
        fun maxImageDimension(maxImageDimension: Int) =
            apply { this.maxImageDimension = maxImageDimension }

        /**
         * set the compression quality between 0 and 100
         */
        fun compressionQuality(compressionQuality: Int) =
            apply { this.compressionQuality = compressionQuality }

        /**
         * set the theme for the dialog, (only used in `MODE.BOTH`)
         */
        fun themeResId(@StyleRes themeResId: Int) =
            apply { this.themeResId = themeResId }

        /**
         * create an `EasyImagePicker` instance
         */
        fun create(savedInstanceState: Bundle?, fragment: Fragment, callback: (Uri?) -> Unit): EasyImagePicker {
            return EasyImagePicker(this, savedInstanceState, null, fragment, callback)
        }

        fun create(savedInstanceState: Bundle?, activity: AppCompatActivity, callback: (Uri?) -> Unit): EasyImagePicker {
            return EasyImagePicker(this, savedInstanceState, activity, null, callback)
        }
    }

    /**
     * persist image path so it's possible to recover
     */
    fun onSaveInstanceState(outState: Bundle) {
        currentPhotoPath?.let {
            outState.putString(CURRENT_PHOTO_PATH_KEY, it)
        }

        currentPhotoDisplayName?.let {
            outState.putString(CURRENT_PHOTO_DISPLAY_NAME_KEY, it)
        }
    }

    private fun getActivityStarter(
        fragment: Fragment,
    ): ActivityStarterInterface {
        return FragmentStarter(
            fragment,
            cameraCallback = {
                callback(handleActivityResultCamera(it))
            },
            galleryCallback = {
                callback(handleActivityResultGallery(it, fragment.requireContext()))
            },
            cameraPermissionCallback = { granted ->
                handleCameraPermission(granted)
            }
        )
    }

    private fun getActivityStarter(
        activity: AppCompatActivity,
    ): ActivityStarterInterface {
        return ActivityStarter(
            activity,
            cameraCallback = {
                callback(handleActivityResultCamera(it))
            },
            galleryCallback = {
                callback(handleActivityResultGallery(it, activity))
            },
            cameraPermissionCallback = { granted ->
                handleCameraPermission(granted)
            }
        )
    }

    /**
     * handle the callback from the camera permission request
     */
    private fun handleCameraPermission(
        granted: Boolean,
    ) {
        if (granted) {
            startCamera()
        } else {
            callback(null)
        }
    }

    /**
     * handle the result from the camera
     */
    private fun handleActivityResultCamera(
        result: ActivityResult,
    ): Uri? {
        if (result.resultCode != Activity.RESULT_OK) {
            return null
        }

        val imagePath = currentPhotoPath ?: return null
        val outputPath = currentPhotoPath ?: return null
        try {
            ImageCompressor.compressImageWithPath(
                imagePath,
                outputPath,
                maxImageDimension,
                compressionQuality
            )
        } catch (e: Exception) {
            return null
        }

        return outputPath.toUri()
    }

    /**
     * handle the result from a gallery pick
     */
    private fun handleActivityResultGallery(
        uri: Uri?,
        context: Context
    ): Uri? {
        uri ?: return null

        try {
            createImageFile(context)
        } catch (e: Exception) {
            return null
        }

        val outputPath = currentPhotoPath ?: return null
        try {
            ImageCompressor.compressImageWithResolver(
                uri,
                outputPath,
                context.contentResolver,
                maxImageDimension,
                compressionQuality
            )
        } catch (e: Exception) {
            return null
        }

        return outputPath.toUri()
    }

    /**
     * show the dialog or gallery/camera
     * @param mode mode a dialog or directly open the gallery or camera
     */
    fun start(
        mode: MODE = MODE.BOTH
    ) {
        when (mode) {
            MODE.BOTH -> showPickerDialog()
            MODE.GALLERY_ONLY -> startGallery()
            MODE.CAMERA_ONLY -> startCamera()
        }
    }

    /**
     * show the picker dialog
     */
    private fun showPickerDialog() {
        val layoutInflater = LayoutInflater.from(activityStarter.getContext())
        val view = SelectPickerDialogBinding.inflate(layoutInflater)

        val dialogBuilder = if (themeResId == 0) {
            AlertDialog.Builder(activityStarter.getContext())
        } else {
            AlertDialog.Builder(activityStarter.getContext(), themeResId)
        }

        val dialog = dialogBuilder
            .setTitle(R.string.eip_dialog_title)
            .setView(view.root)
            .setNegativeButton(R.string.eip_dialog_cancel) { dialog, _ ->
                callback(null)
                dialog.dismiss()
            }
            .setOnCancelListener {
                callback(null)
            }
            .create()

        view.selectCameraRegion.setOnClickListener {
            startCamera()
            dialog.dismiss()
        }

        view.selectGalleryRegion.setOnClickListener {
            startGallery()
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * start the gallery
     */
    private fun startGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        activityStarter.startGallery(intent)
    }

    /**
     * Check if a permission is present in the manifest
     */
    private fun hasPermissionInManifest(
        context: Context,
        @Suppress("SameParameterValue") permissionName: String
    ): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            return packageInfo.requestedPermissions?.contains(permissionName) == true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun startCamera() {
        /**
         * If the Camera permission is present in the manifest the Camera permission should also be granted
         * otherwise a Security exception will be thrown
         *
         * see:
         * https://developer.android.com/reference/android/provider/MediaStore.html#ACTION_IMAGE_CAPTURE
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            hasPermissionInManifest(
                activityStarter.getContext(),
                Manifest.permission.CAMERA
            ) &&
            ContextCompat.checkSelfPermission(
                activityStarter.getContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityStarter.requestCameraPermission()
            return
        }

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(activityStarter.getContext().packageManager)
                ?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile(activityStarter.getContext())
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        null
                    }

                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            activityStarter.getContext(),
                            "${activityStarter.getContext().packageName}.EasyImagePickerFileProvider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                        activityStarter.startCamera(takePictureIntent)
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