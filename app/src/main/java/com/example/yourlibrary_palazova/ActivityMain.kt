package com.example.yourlibrary_palazova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.example.yourlibrary_palazova.databinding.ActivityMainBinding

class ActivityMain : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = ViewPagerAdapter(this) // створюємо адаптер для ViewPager
        binding.viewPager.adapter = pagerAdapter // прив'язуємо адаптер до ViewPager

        // оновлення елементів у нижній навігації
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigation.menu[position].isChecked = true
            }
        })

        // обробник натискань на пункти нижнього меню
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_1 -> binding.viewPager.currentItem = 0
                R.id.item_2 -> binding.viewPager.currentItem = 1
            }
            true
        }
    }
}