package nl.esites.easyimagepicker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.activity.result.PickVisualMediaRequest

/**
 * helper class to allow fragments and activities to use this library
 */
internal interface ActivityStarterInterface {

    fun requestCameraPermission()

    fun getContext(): Context

    fun startCamera(intent: Intent)

    fun startGallery(intent: Intent)
}

internal class ActivityStarter(
    private val activity: AppCompatActivity,
    cameraCallback: (ActivityResult) -> Unit,
    galleryCallback: (Uri?) -> Unit,
    cameraPermissionCallback: (Boolean) -> Unit,
) : ActivityStarterInterface {
    private val getContentCamera =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            cameraCallback(activityResult)
        }

    private val getContentGallery = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        galleryCallback(uri)
    }

    private val requestPermissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            cameraPermissionCallback(isGranted)
        }

    override fun getContext(): Context {
        return activity
    }

    override fun startCamera(intent: Intent) {
        getContentCamera.launch(intent)
    }

    override fun startGallery(intent: Intent) {
        getContentGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

internal class FragmentStarter(
    private val fragment: Fragment,
    cameraCallback: (ActivityResult) -> Unit,
    galleryCallback: (Uri?) -> Unit,
    cameraPermissionCallback: (Boolean) -> Unit,
) : ActivityStarterInterface {

    private val getContentCamera =
        fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            cameraCallback(activityResult)
        }

    private val getContentGallery = fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        galleryCallback(uri)
    }

    private val requestPermissionLauncher =
        fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            cameraPermissionCallback(isGranted)
        }

    override fun getContext(): Context {
        return fragment.requireContext()
    }

    override fun startCamera(intent: Intent) {
        getContentCamera.launch(intent)
    }

    override fun startGallery(intent: Intent) {
        getContentGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}