package com.example.yourlibrary_palazova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val action = intent.getStringExtra("action")

        if (action == "signUp") {
            // Переход на экран регистрации
            supportFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentRegister())
                .commit()
        } else if (action == "logIn") {
            // Переход на экран входа
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
