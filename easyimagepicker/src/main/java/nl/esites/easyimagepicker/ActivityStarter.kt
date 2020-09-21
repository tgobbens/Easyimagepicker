package nl.esites.easyimagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment

internal interface ActivityStarterInterface {
    fun startActivityForResult(intent: Intent, requestCode: Int)

    fun requestPermissions(permissions: Array<String>, requestCode: Int)

    fun getContext(): Context
}

internal class ActivityStarter(private val activity: Activity) : ActivityStarterInterface {
    override fun getContext(): Context {
        return activity
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
    }

    override fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions, requestCode)
        }
    }
}

internal class FragmentStarter(private val fragment: Fragment) : ActivityStarterInterface {
    override fun getContext(): Context {
        return fragment.requireContext()
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        fragment.startActivityForResult(intent, requestCode)
    }

    override fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.requestPermissions(permissions, requestCode)
        }
    }
}