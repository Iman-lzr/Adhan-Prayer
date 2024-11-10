package com.example.salat


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewpager :ViewPager2 = findViewById(R.id.viewpager)
        val bottomNav : BottomNavigationView = findViewById(R.id.navigation)

        val adapter = ViewPagerAdapter(this)
        viewpager.adapter = adapter

        bottomNav.setOnItemSelectedListener {item ->
            when(item.itemId){
                R.id.priere ->viewpager.setCurrentItem(0,true)
                R.id.boussole -> viewpager.setCurrentItem(1,true)

            }
            true

        }
        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
    }
}

