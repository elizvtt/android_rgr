package com.example.yourlibrary_palazova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

class FragmentLogin : Fragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_login, container, false)

        emailEditText = view.findViewById(R.id.inputEditTextEmail)
        passwordEditText = view.findViewById(R.id.inputEditTextPassword)
        emailInputLayout = view.findViewById(R.id.textInputLayout5)
        passwordInputLayout = view.findViewById(R.id.textInputLayout6)

        // текстове посилання для переходу до реєстрації
        val sigUpTextView: TextView = view.findViewById(R.id.textViewLink)
        sigUpTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentRegister())
                .addToBackStack(null)
                .commit()
        }

        // текстове посилання для відновлення пароля
        val forgotPassword: TextView = view.findViewById(R.id.forgotPass)
        forgotPassword.setOnClickListener {
            val email = emailEditText.text.toString()

            if (email.isNotEmpty()) {
                // відправляємо лист для скидання пароля через FirebaseAuth
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Лист для скидання пароля надіслано", Toast.LENGTH_LONG).show()
                        } else Log.d("Fragment Login", "Помилка: ${task.exception?.message}")
                    }
            } else {
                Toast.makeText(requireContext(), "Будь ласка, введіть ваш email", Toast.LENGTH_SHORT).show()
            }
        }

        val  loginButton: Button = view.findViewById(R.id.logInButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            emailInputLayout.error = null
            passwordInputLayout.error = null

            // перевірка, що поля не пусті
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // якщо успішно - переходимо в головну активність
                            val intent = Intent(requireContext(), ActivityMain::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                            Toast.makeText(requireContext(), "З поверненням!", Toast.LENGTH_SHORT).show()

                        } else {
                            // виведення повідомлення про помилки
                            val exception = task.exception
                            if (exception is FirebaseAuthInvalidCredentialsException) {
                                val errorMessage = exception.message
                                when {
                                    errorMessage?.contains("The email address is badly formatted") == true -> {
                                        emailInputLayout.error = "Невірний email"
                                    }

                                    errorMessage?.contains("The password is invalid") == true -> {
                                        passwordInputLayout.error = "Невірний пароль"
                                    }

                                }
                            } else if (exception is FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(requireContext(), "Необхідно повторно увійти", Toast.LENGTH_LONG).show()
                            } else Log.d("Fragment Login", "Помилка: ${exception?.message}")
                        }
                    }
            } else {
                // повідомлення про помилки, якщо якісь поля порожні
                if (email.isEmpty()) {
                    emailInputLayout.error = "Будь ласка, введіть email"
                }
                if (password.isEmpty()) {
                    passwordInputLayout.error = "Будь ласка, введіть пароль"
                }
            }
        }
        return view
    }
}