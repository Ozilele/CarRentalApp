package com.example.carrental

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import kotlin.random.Random

class MainActivity : AppCompatActivity(), FavoriteInterface {

    private var carsList : List<Car> = emptyList()
    private lateinit var db : FirebaseFirestore
    private lateinit var imagePaths : MutableList<String>
    private lateinit var carsListData: List<CarData?>
    private var favoriteCarsListData: ArrayList<CarData?> = ArrayList()
    private var favoriteId : String = ""
    private lateinit var favoriteCar:  List<FavoriteCar?>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = Firebase.firestore

        GlobalScope.launch(Dispatchers.IO) {
            db.collection("favorites")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    favoriteCar = querySnapshot.documents.map { documentSnapshot ->
                        var favoriteCar = documentSnapshot.toObject(FavoriteCar::class.java)
                        if (favoriteCar!!.userID == FirebaseAuthSingleton.getInstance().currentUser!!.uid)
                            favoriteId = documentSnapshot.id
                        favoriteCar
                    }
                    if (favoriteId == "") {
                        db.collection("favorites")
                            .add(mapOf("userID" to FirebaseAuthSingleton.getInstance().currentUser!!.uid, "cars" to ArrayList<String>()))
                            .addOnSuccessListener { documentSnapshot ->
                                favoriteId =  documentSnapshot.id
                            }
                    } else {
                        for (i in favoriteCar.indices) {
                            if (favoriteCar[i]!!.userID == FirebaseAuthSingleton.getInstance().currentUser!!.uid) {
                                for (j in 0 until favoriteCar[i]!!.cars.size) {
                                    db.collection("carsData")
                                        .document(favoriteCar[i]!!.cars[j])
                                        .get()
                                        .addOnSuccessListener { documentSnapshot ->
                                            if(documentSnapshot.exists()) {
                                                favoriteCarsListData.add(documentSnapshot.toObject(CarData::class.java)!!)
                                            }
                                        }
                                        .addOnFailureListener{ ex ->
                                            Log.e(ContentValues.TAG, "Error getting cars data: ", ex)
                                        }

                                }
                                break
                            }
                        }
                    }
                    db.collection("carsData")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            carsListData = querySnapshot.documents.map { documentSnapshot ->
                                var carData = documentSnapshot.toObject(CarData::class.java)
                                carData
                            }
                            replaceFragment(HomeFragment(carsListData, favoriteCarsListData, listener = this@MainActivity))
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting cars data: ", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting cars data: ", e)
                }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val currentUser = FirebaseAuthSingleton.getInstance().currentUser
        val userEmail: String? = currentUser!!.email
        val photoUrl: Uri? = currentUser.photoUrl
        val userName: String? = currentUser.displayName

        val storageRef = FirebaseStorage.getInstance().reference.child("images/")
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                imagePaths = mutableListOf<String>()

                for (item in listResult.items) {
                    val path = item.path
                    imagePaths.add(path)
                }

                println("Lista ścieżek do zdjęć: $imagePaths")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Błąd pobierania listy elementów z Firebase Storage", exception)
            }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment(carsListData, favoriteCarsListData, this))
                    return@setOnItemSelectedListener true
                }
                R.id.favorite -> {
                    Log.e("", favoriteCarsListData.size.toString())
                    replaceFragment(FavoriteFragment(favoriteCarsListData, favoriteCarsListData, this))
                    return@setOnItemSelectedListener true
                }
                R.id.account -> {
                    replaceFragment(AccountFragment.newInstance(userName, userEmail, photoUrl))
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    private fun insertCarData() {
        val categoryList = arrayListOf<String>()
        categoryList.add("ALL")
        categoryList.add("SPORT")
        categoryList.add("CITY")
        categoryList.add("PREMIUM")
        categoryList.add("OFF ROAD")

        carsList.forEach {car ->
            val cost = Random.nextInt(100, 1000)
            val category = categoryList[Random.nextInt(0, categoryList.size)]
            val randomImgPath = imagePaths[Random.nextInt(0, imagePaths.size)]

            val collectionRef = db.collection("carsData")
            val documentRef = collectionRef.document()

            val carData = hashMapOf(
                "id" to documentRef.id,
                "category" to category,
                "brand" to car.make.name,
                "model" to car.name,
                "url" to randomImgPath,
                "cost" to cost,
            )

            // Add a new document with a generated ID
            documentRef
                .set(carData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${car.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                }
        }
    }

    private fun AppCompatActivity.replaceFragment(fragment:Fragment){
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.commit()
    }

    override fun addCar(car: CarData) {
        favoriteCarsListData.add(car)

        val favoriteCarsID = ArrayList<String>()

        for (i in favoriteCarsListData.indices) {
            favoriteCarsID.add(favoriteCarsListData[i]!!.id)
        }

        GlobalScope.launch(Dispatchers.IO) {
            db.collection("favorites")
                .document(favoriteId)
                .set(mapOf("userID" to FirebaseAuthSingleton.getInstance().currentUser!!.uid, "cars" to favoriteCarsID))
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Car added successful")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding Car", e)
                }
        }

    }

    override fun deleteCar(car: CarData) {
        var favoriteCarList: ArrayList<CarData?> = ArrayList()
        val favoriteCarsID = ArrayList<String>()

        for (i in favoriteCarsListData.indices) {
            if (favoriteCarsListData[i]!!.id != car.id) {
                favoriteCarList.add(favoriteCarsListData[i])
            }
        }
        favoriteCarsListData = favoriteCarList

        for (i in favoriteCarsListData.indices) {
            favoriteCarsID.add(favoriteCarsListData[i]!!.id)
        }


        GlobalScope.launch(Dispatchers.IO) {
            db.collection("favorites")
                .document(favoriteId)
                .set(mapOf("userID" to FirebaseAuthSingleton.getInstance().currentUser!!.uid, "cars" to favoriteCarsID))
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Car added successful")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding Car", e)
                }
        }
    }
}