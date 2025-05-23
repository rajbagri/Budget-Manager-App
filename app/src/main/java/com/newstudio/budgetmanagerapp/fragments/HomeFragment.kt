package com.newstudio.budgetmanagerapp.fragments


import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldValue.arrayUnion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.newstudio.budgetmanagerapp.R
import com.newstudio.budgetmanagerapp.dataClasses.Transaction
import com.newstudio.budgetmanagerapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class HomeFragment : Fragment() {

    private lateinit var binding:FragmentHomeBinding
    private lateinit var userRef:DocumentReference
    private lateinit var db:FirebaseFirestore
    private lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in. Please log in again.", Toast.LENGTH_LONG).show()
            return null
        }
        userRef  = db.collection("users").document(userId)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            show()
            title = getString(R.string.app_name)
            setBackgroundDrawable(getDrawable(requireContext(),R.color.themeBg))
        }

        binding.CorutineProgBar.visibility = View.VISIBLE
        binding.fullscreen.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch{
            initValues()
        }
        binding.editBudget.setOnClickListener{
            showEditDialog()
        }
        binding.addTransaction.setOnClickListener {
            showAddDialog()
        }
        binding.showAll.setOnClickListener {
            val navControler = Navigation.findNavController(it)
            navControler.navigate(R.id.action_homeFragment_to_transactionListFragment)
        }
    }

    private fun ResetValues(lstMonth:Int) {
        val c = Calendar.getInstance()
        val month = c[Calendar.MONTH]
        Log.d("TAGYY",month.toString())
        Log.d("TAGYY",lstMonth.toString())
        if(month !=lstMonth)
        {
            userRef.update("totalTransactions", mutableListOf<Transaction>())
            userRef.update("budget",0)
            userRef.update("spent", 0)
            binding.BudgetTv.text = "₹0"
            binding.SpentTv.text = "₹0"
            binding.safeTv.text = "₹0/day"
            binding.progressText.text = "0.00%"
            binding.Prog.setProgress(0,true)
        }
        userRef.update("month",month).addOnFailureListener{
            Toast.makeText(context, "User Not Found or Update Failed!", Toast.LENGTH_LONG).show()
            LogOut()
        }
    }

    private fun showAddDialog() {
        val dialog = Dialog(requireContext())
        dialog.setCancelable(true)

        val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.add_transaction_popup, null)
        dialog.setContentView(view)

        val cancel = view.findViewById(R.id.cancelBtn) as MaterialButton
        val done = view.findViewById(R.id.DoneBtn) as MaterialButton
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        done.setOnClickListener {
            binding.CorutineProgBar.visibility = View.VISIBLE
            binding.fullscreen.visibility = View.GONE
            val amount = view.findViewById<EditText>(R.id.transactionEt).text.toString()
            val note = view.findViewById<EditText>(R.id.noteEt).text.toString()
            val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
            val creditSelected = radioGroup.checkedRadioButtonId == R.id.radio_credit
            val debitSelected = radioGroup.checkedRadioButtonId == R.id.radio_debit

            if(amount.isNotEmpty() && amount.isDigitsOnly() && (creditSelected || debitSelected))
            {
                val type = !debitSelected
                val transaction = Transaction(System.currentTimeMillis().toString(),amount.toInt(),type,note)
                userRef.update("totalTransactions",arrayUnion(transaction)).addOnFailureListener {
                    binding.CorutineProgBar.visibility = View.GONE
                    binding.fullscreen.visibility = View.VISIBLE
                    Toast.makeText(requireContext().applicationContext,"Unexpected error! Try Again",Toast.LENGTH_LONG).show()
                }
                    .addOnCompleteListener {
                        addTransaction(amount,type)
                        dialog.dismiss()
                    }

            }
            else
            {
                binding.CorutineProgBar.visibility = View.GONE
                binding.fullscreen.visibility = View.VISIBLE
                Toast.makeText(context,"Please enter a valid amount and select a type",Toast.LENGTH_LONG).show()
            }

        }


        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show();
    }

    private fun addTransaction(amount: String, type: Boolean) {
        val money = amount.toInt()
        userRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val data = documentSnapshot.data
                val current = data?.get("spent") as? Long ?: 0L
                val finalSpent = if (!type) money + current else current - money
                userRef.update("spent", finalSpent).addOnSuccessListener {
                    Toast.makeText(context?.applicationContext, "Transaction Added Successfully", Toast.LENGTH_LONG).show()
                    initValues()
                }.addOnFailureListener {
                    Toast.makeText(context?.applicationContext, "Failed to update spent amount! Try Again", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context?.applicationContext, "User data not found!", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context?.applicationContext, "Error fetching user data! Try Again", Toast.LENGTH_LONG).show()
        }
    }


    private fun showEditDialog() {
        val dialog = Dialog(requireContext())
        dialog.setCancelable(true)

        val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.edit_budget_popup, null)
        dialog.setContentView(view)

        val cancel = view.findViewById(R.id.cancelBtn) as MaterialButton
        val done = view.findViewById(R.id.DoneBtn) as MaterialButton
        cancel.setOnClickListener{
            dialog.cancel()
        }
        done.setOnClickListener{
            binding.CorutineProgBar.visibility = View.VISIBLE
            binding.fullscreen.visibility = View.GONE
            val newVal:EditText = view.findViewById(R.id.budgetEt)
            val budgetVal = newVal.text.toString()
            if(budgetVal.isNotEmpty() && budgetVal.isDigitsOnly()){
                userRef.update("budget",budgetVal.toInt()).addOnFailureListener {
                    binding.CorutineProgBar.visibility = View.GONE
                    binding.fullscreen.visibility = View.VISIBLE
                    Toast.makeText(context,"Unexpected error! Try Again",Toast.LENGTH_LONG).show()
                }
                    .addOnSuccessListener {
                        initValues()
                        dialog.dismiss()
                    }

            }
            else
            {
                binding.CorutineProgBar.visibility = View.GONE
                binding.fullscreen.visibility = View.VISIBLE
                Toast.makeText(context,"Enter Valid Amount",Toast.LENGTH_LONG).show()
            }

        }
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show();
    }

    private   fun initValues() {

        binding.CorutineProgBar.visibility = View.VISIBLE
        binding.fullscreen.visibility = View.GONE
        userRef.get()
            .addOnSuccessListener {
                if(it.exists())
                {
                    val data = it.data
                    val name = data?.get("name").toString()
                    val budget = (data?.get("budget") as? Long)?.toInt() ?: 0
                    val spent = (data?.get("spent") as? Long)?.toInt() ?: 0
                    val lstMonth = (data?.get("month") as? Long)?.toInt() ?: -1
                    binding.BudgetTv.text = "₹$budget"
                    binding.SpentTv.text = "₹$spent"
                    binding.GreetTv.text = name
                    binding.CorutineProgBar.visibility = View.GONE
                    binding.fullscreen.visibility = View.VISIBLE
                    val calendar = Calendar.getInstance()
                    val lastDay: Int = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)
                    val daysLeft = lastDay - currentDay+1
                    var Safemoney = (budget-spent)/daysLeft
                    if(Safemoney<0)
                        Safemoney = 0
                    binding.safeTv.text = "₹$Safemoney/day"
                    var precent:Double = 0.0
                    if(budget != 0) {
                        precent = (spent.toDouble()/budget.toDouble())*100
                    }
                    val finalPer:Double = String.format("%.2f", precent).toDouble()

                    binding.progressText.text = "$finalPer%"
                    binding.Prog.setProgress(finalPer.toInt(),true)
                    ResetValues(lstMonth)

                }
                else
                {
                    Toast.makeText(context,"User Data Not Found! Please log in again.",Toast.LENGTH_LONG).show()
                    LogOut()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context,"Error fetching user data: ${it.message}",Toast.LENGTH_LONG).show()
                binding.CorutineProgBar.visibility = View.GONE
                binding.fullscreen.visibility = View.VISIBLE
                LogOut()
            }

        userRef.update("lastIn",System.currentTimeMillis()).addOnFailureListener{
            Toast.makeText(context,"Failed to update last login time!",Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.logout_menu->{
                LogOut()
            }
            R.id.resetAll->{
                val updates = hashMapOf<String, Any>(
                    "totalTransactions" to mutableListOf<Transaction>(),
                    "budget" to 0,
                    "spent" to 0
                )
                userRef.update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "All data reset successfully!", Toast.LENGTH_SHORT).show()
                        initValues()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context,"Unexpected error, try again! ${it.message}",Toast.LENGTH_LONG).show()
                    }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            show()
            title = getString(R.string.app_name)
        }
    }

    private fun LogOut() {
        auth.signOut()
        val nav = Navigation.findNavController(requireView())
        nav.navigate(R.id.action_homeFragment_to_loginFragment)
    }
}