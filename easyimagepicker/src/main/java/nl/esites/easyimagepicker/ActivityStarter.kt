package nl.esites.easyimagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

internal interface ActivityStarterInterface {
    fun startActivityForResult(intent: Intent, requestCode: Int)

    fun getContext(): Context
}

internal class ActivityStarter(private val activity: Activity) : ActivityStarterInterface {
    override fun getContext(): Context {
        return activity
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
    }
}

internal class FragmentStarter(private val fragment: Fragment) : ActivityStarterInterface {
    override fun getContext(): Context {
        return fragment.requireContext()
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        fragment.startActivityForResult(intent, requestCode)
    }
}