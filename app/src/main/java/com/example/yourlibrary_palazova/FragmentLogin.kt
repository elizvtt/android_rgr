package com.example.yourlibrary_palazova

import android.content.Intent
import android.os.Bundle
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

        val sigUpTextView: TextView = view.findViewById(R.id.textViewLink)
        sigUpTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentRegister())
                .addToBackStack(null)
                .commit()
        }

        val forgotPassword: TextView = view.findViewById(R.id.forgotPass)
        forgotPassword.setOnClickListener {
            val email = emailEditText.text.toString()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Лист для скидання пароля надіслано", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Помилка: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
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

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                            Toast.makeText(requireContext(), "З поверненням!", Toast.LENGTH_SHORT).show()

                        } else {
                            // Выводим сообщение об ошибке
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
                                // Обработка случая, если пользователь должен заново войти для выполнения действия
                                Toast.makeText(requireContext(), "Необхідно повторно увійти", Toast.LENGTH_LONG).show()
                            } else {
                                // Обрабатываем ошибку других типов
                                Toast.makeText(requireContext(), "Помилка 2: ${exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            } else {
                // Если одно из полей пустое
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