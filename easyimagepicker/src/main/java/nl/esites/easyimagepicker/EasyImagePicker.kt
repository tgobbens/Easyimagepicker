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
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
        const val DEFAULT_CAMERA_PERMISSION_REQUEST_CODE = 21253

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

    private val mode: MODE
    private val requestCameraCode: Int
    private val requestGalleryCode: Int
    private val maxImageDimension: Int
    private val compressionQuality: Int
    @StyleRes
    private val themeResId: Int

    private var currentPhotoPath: String? = null
    private var currentPhotoDisplayName: String? = null

    init {
        mode = builder.mode
        requestCameraCode = builder.requestCameraCode
        requestGalleryCode = builder.requestGalleryCode
        maxImageDimension = builder.maxImageDimension
        compressionQuality = builder.compressionQuality
        themeResId = builder.themeResId

        currentPhotoPath = savedInstanceState?.getString(CURRENT_PHOTO_PATH_KEY, null)
        currentPhotoDisplayName = savedInstanceState?.getString(
            CURRENT_PHOTO_DISPLAY_NAME_KEY,
            null
        )
    }

    class Builder {
        var mode: MODE = MODE.BOTH
            private set

        var maxImageDimension: Int = DEFAULT_MAX_IMAGE_DIMENSION
            private set

        var compressionQuality: Int = DEFAULT_COMPRESSION_QUALITY
            private set

        var requestCameraCode: Int = DEFAULT_CAMERA_REQUEST_CODE
            private set

        var requestGalleryCode: Int = DEFAULT_GALLERY_REQUEST_CODE
            private set

        @StyleRes
        var themeResId: Int = 0
            private set

        /**
         * set the `MODE`, show a dialog or directly open camera or gallery
         */
        fun mode(mode: MODE) =
            apply { this.mode = mode }

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
         * change the default camera intent request code
         */
        fun requestCameraCode(requestCameraCode: Int) =
            apply { this.requestCameraCode = requestCameraCode }

        /**
         * change the default gallery intent request code
         */
        fun requestGalleryCode(requestGalleryCode: Int) =
            apply { this.requestGalleryCode = requestGalleryCode }

        /**
         * create an `EasyImagePicker` instance
         */
        fun create(savedInstanceState: Bundle?): EasyImagePicker {
            return EasyImagePicker(this, savedInstanceState)
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

    /**
     * handle the result from the permission request, if true is returned the library has handled the request
     */
    fun handleOnRequestPermissionsResult(
        activity: Activity, requestCode: Int, grantResults: IntArray
    ): Boolean {
        return handleOnRequestPermissionsResult(ActivityStarter(activity), requestCode, grantResults)
    }

    /**
     * handle the result from the permission request, if true is returned the library has handled the request
     */
    fun handleOnRequestPermissionsResult(
        fragment: Fragment, requestCode: Int, grantResults: IntArray
    ): Boolean {
        return handleOnRequestPermissionsResult(
            FragmentStarter(fragment),
            requestCode,
            grantResults
        )
    }

    private fun handleOnRequestPermissionsResult(
        activityStarterInterface: ActivityStarterInterface, requestCode: Int, grantResults: IntArray
    ): Boolean {
        if (requestCode == DEFAULT_CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(activityStarterInterface)
            }
            // no permission cannot start camera as it would crash
            return true
        }
        return false
    }

    /**
     * handle the EasyImagePickers intents, returns `true` is a result is available
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity): Boolean {
        @Suppress("unused")
        if (this.requestCameraCode == requestCode && resultCode == Activity.RESULT_OK) {
            val imagePath = currentPhotoPath  ?: return false
            val outputPath = currentPhotoPath  ?: return false

            return try {
                ImageCompressor.compressImageWithPath(
                    imagePath,
                    outputPath,
                    maxImageDimension,
                    compressionQuality
                )
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
                ImageCompressor.compressImageWithResolver(
                    imageUri,
                    outputPath,
                    activity.contentResolver,
                    maxImageDimension,
                    compressionQuality
                )
            } catch (e: Exception) {
                false
            }
        }

        return false
    }

    /**
     * get the result image uri
     */
    fun getResultImageUri(): Uri? {
        return currentPhotoPath?.toUri()
    }

    /**
     * show the dialog or gallery/camera
     */
    @Suppress("unused")
    fun start(activity: Activity, withMode: MODE? = null) {
        when (withMode ?: this.mode) {
            MODE.BOTH -> showPickerDialog(ActivityStarter(activity))
            MODE.GALLERY_ONLY -> startGallery(ActivityStarter(activity))
            MODE.CAMERA_ONLY -> startCamera(ActivityStarter(activity))
        }
    }

    @Suppress("unused")
    fun start(fragment: Fragment, withMode: MODE? = null) {
        when (withMode ?: this.mode) {
            MODE.BOTH -> showPickerDialog(FragmentStarter(fragment))
            MODE.GALLERY_ONLY -> startGallery(FragmentStarter(fragment))
            MODE.CAMERA_ONLY -> startCamera(FragmentStarter(fragment))
        }
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activityStarterInterface.startActivityForResult(intent, requestGalleryCode)
    }

    /**
     * Check if a permission is present in the manifest
     */
    private fun hasPermissionInManifest(context: Context, @Suppress("SameParameterValue") permissionName: String): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            return packageInfo.requestedPermissions?.contains(permissionName) == true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }

    private fun startCamera(activityStarterInterface: ActivityStarterInterface) {
        /**
         * If the Camera permission is present in the manifest the Camera permission should also be granted
         * otherwise a Security exception will be thrown
         *
         * see:
         * https://developer.android.com/reference/android/provider/MediaStore.html#ACTION_IMAGE_CAPTURE
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            hasPermissionInManifest(activityStarterInterface.getContext(), Manifest.permission.CAMERA) &&
            ContextCompat.checkSelfPermission(activityStarterInterface.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {

            activityStarterInterface.requestPermissions(arrayOf(Manifest.permission.CAMERA), DEFAULT_CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

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

                    activityStarterInterface.startActivityForResult(
                        takePictureIntent,
                        requestCameraCode
                    )
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