package com.example.admin_blinkit_clone.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.admin_blinkit_clone.Utils
import com.example.admin_blinkit_clone.model.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class AdminViewModel : ViewModel() {
    private val _isImagesUploaded = MutableStateFlow(false)
    var isImageUploaded: StateFlow<Boolean> = _isImagesUploaded


    private val _downloadUrls = MutableStateFlow<ArrayList<String?>>(arrayListOf())
    var downloadUrls: StateFlow<ArrayList<String?>> = _downloadUrls


    private val _isProductSaved = MutableStateFlow(false)
    var isProductSaved: StateFlow<Boolean> = _isProductSaved

    fun saveImageInDB(imageUri: ArrayList<Uri>) {
        val downloadUrls = ArrayList<String?>()

        imageUri.forEach { uri ->
            val imageRef =
                FirebaseStorage.getInstance().reference.child(Utils.getCurrentUserId()).child("images")
                    .child(UUID.randomUUID().toString())
            imageRef.putFile(uri).continueWithTask {
                Log.d("merc","${imageRef.downloadUrl}")
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                val url = task.result
                Log.d("BLINKIT", "saveImageInDB: $uri ${task.isSuccessful}")
                Log.d("BLINKIT","Download Urls ${url.toString()}")
                downloadUrls.add(url.toString())
                Log.e("merc","Download Urls in AdminView Model $downloadUrls")
                if (downloadUrls.size == imageUri.size) {
                    _isImagesUploaded.value = true
                    _downloadUrls.value = downloadUrls
                }
            }
        }
    }

    fun saveProduct(product: Product) {
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("AllProducts/${product.productRandomId}").setValue(product)
            .addOnSuccessListener {
                Log.d("merc", "AllProducts added succesfully")
                FirebaseDatabase.getInstance().getReference("Admins")
                    .child("ProductCategory/${product.productRandomId}").setValue(product)
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().getReference("Admins")
                            .child("ProductType/${product.productRandomId}").setValue(product)
                            .addOnSuccessListener {
                                _isProductSaved.value = true
                            }.addOnFailureListener { e ->
                                Log.e("merc", "Failed to add ProductCategory", e)
                            }
                    }.addOnFailureListener { e ->
                        Log.e("merc", "Failed to add Product Type", e)
                    }
            }.addOnFailureListener { exception ->
                Log.e("merc", "Failed to add Allproducts", exception)
            }
    }

    fun fetchAllTheProducts(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)
                    if (category == "All" || prod?.productCategory == category) {
                        products.add(prod!!)
                    }
                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                // Nothing to do here
            }
        }
        db.addValueEventListener(eventListener)

        awaitClose { db.removeEventListener(eventListener) }
    }


}