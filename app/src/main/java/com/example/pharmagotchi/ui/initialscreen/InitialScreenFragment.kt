package com.example.pharmagotchi.ui.initialscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pharmagotchi.databinding.FragmentInitialScreenBinding


class InitialScreenFragment : Fragment() {

    private var _binding: FragmentInitialScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewInitalScreenModel = ViewModelProvider(this).get(InitialScreenViewModel::class.java)
        _binding = FragmentInitialScreenBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}