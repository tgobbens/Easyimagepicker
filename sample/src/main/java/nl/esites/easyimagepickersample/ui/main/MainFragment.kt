package nl.esites.easyimagepickersample.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        easyImagePicker = EasyImagePicker.Builder()
            .themeResId(R.style.AppThemeDialog)
            .create(savedInstanceState, this) { uri ->
                if (uri == null) {
                    Toast.makeText(requireContext(), R.string.no_image_selected, Toast.LENGTH_SHORT).show()
                }

                binding.imageView.setImageURI(uri)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openPickerButton.setOnClickListener {
            easyImagePicker.start(EasyImagePicker.MODE.BOTH)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        easyImagePicker.onSaveInstanceState(outState)
    }
}
