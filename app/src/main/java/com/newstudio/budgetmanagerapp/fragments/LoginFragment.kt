package com.newstudio.budgetmanagerapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.newstudio.budgetmanagerapp.R
import com.newstudio.budgetmanagerapp.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private lateinit var binding:FragmentLoginBinding
    private lateinit var auth :FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        binding.gotoRegister.setOnClickListener{
            GotoRegister(view)
        }
        binding.loginBtn.setOnClickListener{
            LoginUser(view)
        }
    }

    private fun LoginUser(view: View) {
        val mail = binding.mailEt.text.toString().trim()
        val password = binding.passEt.text.toString().trim()

        if(mail.isNotEmpty() && password.isNotEmpty())
        {
            auth.signInWithEmailAndPassword(mail,password).addOnSuccessListener {
                val navController = Navigation.findNavController(view)
                navController.navigate(R.id.action_loginFragment_to_homeFragment)
            }.addOnFailureListener {
                Toast.makeText(context?.applicationContext,it.message,Toast.LENGTH_LONG).show()
            }
        }else
        {
            Toast.makeText(context?.applicationContext,"Please fill all the entries!",Toast.LENGTH_LONG).show()
        }
    }

    fun GotoRegister(view: View) {
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_loginFragment_to_signUpFragment)
    }

}