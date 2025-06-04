package com.example.yourlibrary_palazova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ActivityAuth : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth) // підключаємо xml

        val action = intent.getStringExtra("action") // отримуємо action

        if (action == "signUp") {
            // якщо signUp, то переходимо до екрану реєстрації
            supportFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentRegister())
                .commit()
        } else if (action == "logIn") {
            // якщо logIn, то переходимо до екрану входу
            supportFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentLogin())
                .commit()
        }
    }
}














//    // Метод для переключения между фрагментами
//    fun showRegisterFragment() {
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.authFragmentContainer, FragmentLogin())
//            .addToBackStack(null)
//            .commit()
//    }
//
//    fun showLoginFragment() {
//        supportFragmentManager.popBackStack() // если был переход назад
//    }
