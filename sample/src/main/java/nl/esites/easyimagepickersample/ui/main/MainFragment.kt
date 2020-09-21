package nl.esites.easyimagepickersample.ui.main

import android.content.Intent
import android.graphics.Rect
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.main_fragment.*
import nl.esites.easyimagepicker.EasyImagePicker
import nl.esites.easyimagepicker.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    private lateinit var easyImagePicker: EasyImagePicker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        easyImagePicker = EasyImagePicker.Builder()
            .mode(EasyImagePicker.MODE.BOTH)
            .themeResId(R.style.AppThemeDialog)
            .create(savedInstanceState)

        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        open_picker_button?.setOnClickListener {
            easyImagePicker.start(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        easyImagePicker.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (easyImagePicker.handleActivityResult(requestCode, resultCode, data, requireActivity())) {
            val uri = easyImagePicker.getResultImageUri()

            imageView.setImageURI(uri)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        easyImagePicker.handleOnRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
    }
}
