package com.newstudio.budgetmanagerapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.newstudio.budgetmanagerapp.R


class SplashScreen : Fragment() {
    private lateinit var auth:FirebaseAuth
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Moved this line here and added null-safety checks
        (activity as? AppCompatActivity)?.supportActionBar?.hide() // Safer way to hide the action bar

        auth = FirebaseAuth.getInstance()
        navController = Navigation.findNavController(view)
        Handler(Looper.myLooper()!!).postDelayed(Runnable {
            if(auth.currentUser == null){
                navController.navigate(R.id.action_splashScreen_to_loginFragment)
            }
            else
            {
                navController.navigate(R.id.action_splashScreen_to_homeFragment)
            }
        },1300)
    }
}