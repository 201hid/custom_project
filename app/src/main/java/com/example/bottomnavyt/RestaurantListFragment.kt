package com.example.bottomnavyt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

import androidx.appcompat.app.AppCompatActivity

import com.example.bottomnavyt.Restaurant
import okhttp3.*

import okhttp3.Callback
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Resturants : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button
    private lateinit var adapter: RestaurantAdapter

    private val LOCATION_REQUEST_CODE = 1
    private var apiKey = "AIzaSyCG-YhCR3j6oq8Av3Jz78OuTrjbUnzLtzI"
    private var location: String = ""
    private var radius: Int = 1500

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurant_list, container, false)

        recyclerView = view.findViewById(R.id.restaurantRecyclerView)
        getLocationButton = view.findViewById(R.id.getLocationButton)
        adapter = RestaurantAdapter()

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        getLocationButton.setOnClickListener {
            val locationIntent = Intent(requireActivity(), LocationActivity::class.java)
            startActivityForResult(locationIntent, LOCATION_REQUEST_CODE)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val latitude = data?.getStringExtra("latitude")
            val longitude = data?.getStringExtra("longitude")
            val radiusStr = data?.getStringExtra("radius")

            if (latitude != null && longitude != null && radiusStr != null) {
                location = "$latitude, $longitude"
                radius = radiusStr.toInt()
                getNearbyRestaurants(apiKey, location, radius, adapter)
            }
        }
    }

    private fun getNearbyRestaurants(apiKey: String, location: String, radius: Int, adapter: RestaurantAdapter) {
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$location" +
                "&radius=$radius" +
                "&type=restaurant" +
                "&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        if(::progressBar.isInitialized) progressBar.visibility = View.VISIBLE

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val restaurants = parseRestaurantData(responseData)
                    activity?.runOnUiThread {
                        if(::adapter.isInitialized) adapter.setData(restaurants)
                        if(::progressBar.isInitialized) progressBar.visibility = View.GONE
                    }
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle network or request failure
                activity?.runOnUiThread {
                    if(::progressBar.isInitialized) progressBar.visibility = View.GONE
                }
            }
        })
    }



    private fun parseRestaurantData(responseData: String?): List<Restaurant> {
        val restaurantList = mutableListOf<Restaurant>()
        responseData?.let {
            val jsonObject = JSONObject(it)
            val results = jsonObject.getJSONArray("results")

            for (i in 0 until results.length()) {
                val restaurant = results.getJSONObject(i)
                val name = restaurant.getString("name")
                val vicinity = restaurant.getString("vicinity")

                val newRestaurant = Restaurant(name, vicinity)
                restaurantList.add(newRestaurant)
            }
        }
        return restaurantList
    }
}