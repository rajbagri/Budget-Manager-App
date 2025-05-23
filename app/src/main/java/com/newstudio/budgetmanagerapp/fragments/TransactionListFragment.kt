package com.newstudio.budgetmanagerapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.newstudio.budgetmanagerapp.ListUi.TransactionListAdapter
import com.newstudio.budgetmanagerapp.dataClasses.Transaction
import com.newstudio.budgetmanagerapp.databinding.FragmentTransactionListBinding

class TransactionListFragment : Fragment() {
    private lateinit var binding: FragmentTransactionListBinding
    private lateinit var usersTransationList:MutableList<Transaction>
    private lateinit var userRef: DocumentReference
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionListBinding.inflate(layoutInflater)
        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        // Ensure auth.currentUser is not null before accessing uid
        val userId = auth.currentUser?.uid
        if (userId == null) {

            Toast.makeText(context, "User not logged in.", Toast.LENGTH_LONG).show()
            return null
        }
        userRef  = db.collection("users").document(userId)
        usersTransationList = mutableListOf()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Transactions made this month"

        binding.CorutineProgBar.visibility = View.VISIBLE
        binding.recycler.visibility = View.GONE

        userRef.get()
            .addOnSuccessListener {
                if(it.exists())
                {
                    val data = it.data
                    val x  = data?.get("totalTransactions") as? List<HashMap<String,*>>
                    if(x.isNullOrEmpty())
                    {
                        binding.empty.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                    }
                    else {
                        for (tr in x) {
                            val date = tr["date"]?.toString() ?: ""
                            val amount = (tr["amount"] as? Long)?.toInt() ?: 0
                            val type = tr["type"] as? Boolean ?: false
                            val note = tr["note"]?.toString() ?: ""

                            usersTransationList.add(Transaction(date, amount, type, note))
                        }
                        usersTransationList = usersTransationList.reversed() as MutableList<Transaction>
                        binding.recycler.layoutManager = LinearLayoutManager(context)
                        binding.recycler.adapter = TransactionListAdapter(usersTransationList)

                        if(usersTransationList.isEmpty())
                        {
                            binding.empty.visibility = View.VISIBLE
                            binding.recycler.visibility = View.GONE
                        }
                        else
                        {
                            binding.empty.visibility = View.GONE
                            binding.recycler.visibility = View.VISIBLE
                        }
                    }
                    binding.CorutineProgBar.visibility = View.GONE
                } else {
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                    binding.CorutineProgBar.visibility = View.GONE
                }
            }.addOnFailureListener{
                Toast.makeText(context?.applicationContext,"Unexpected error! Try Again: ${it.message}",Toast.LENGTH_LONG).show()
                binding.CorutineProgBar.visibility = View.GONE // Hide progress bar on failure
            }
    }
}