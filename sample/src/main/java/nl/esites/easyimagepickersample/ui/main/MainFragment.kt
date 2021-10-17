package nl.esites.easyimagepickersample.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.esites.easyimagepicker.EasyImagePicker
import nl.esites.easyimagepickersample.R
import nl.esites.easyimagepickersample.databinding.MainFragmentBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var easyImagePicker: EasyImagePicker

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(layoutInflater, container, false)

        easyImagePicker = EasyImagePicker.Builder()
            .mode(EasyImagePicker.MODE.BOTH)
            .themeResId(R.style.AppThemeDialog)
            .create(savedInstanceState)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openPickerButton.setOnClickListener {
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

            binding.imageView.setImageURI(uri)
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
}
