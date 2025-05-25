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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class FragmentRegister : Fragment() {

    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button

    private lateinit var fullNameLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Поля
        fullNameEditText = view.findViewById(R.id.inputEditTextName)
        usernameEditText = view.findViewById(R.id.inputEditTextNickname)
        emailEditText = view.findViewById(R.id.inputEditTextEmail)
        passwordEditText = view.findViewById(R.id.inputEditTextPassword)
        registerButton = view.findViewById(R.id.registerButton)

        // Layouts
        fullNameLayout = view.findViewById(R.id.textInputLayout5)
        usernameLayout = view.findViewById(R.id.textInputLayout6)
        emailLayout = view.findViewById(R.id.textInputLayout3)
        passwordLayout = view.findViewById(R.id.textInputLayout4)


        registerButton.setOnClickListener {
            clearErrors()

            val fullName = fullNameEditText.text.toString()
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            var isValid = true

            // Проверка fullName
            val nameParts = fullName.split(" ")
            val nameHasDigits = fullName.any { it.isDigit() }
            val nameHasInvalidChars = fullName.any { !it.isLetter() && it != '-' && it != ' ' }

            when {
                nameParts.size < 2 -> {
                    fullNameLayout.error = "Введіть прізвище і ім’я через пробіл"
                    isValid = false
                }
                nameHasDigits -> {
                    fullNameLayout.error = "Прізвище та ім’я не повинні містити цифри"
                    isValid = false
                }
                nameHasInvalidChars -> {
                    fullNameLayout.error = "Невірний формат"
                    isValid = false
                }
            }

            // Проверка username
            val usernameRegex = "^[a-zA-Z0-9@\$*()_\\-]+$".toRegex()
            if (!username.matches(usernameRegex)) {
                usernameLayout.error = "Дозволені лише латинські літери, цифри та символи @\$*()_-"
                isValid = false
            }

            // Проверка email
            val emailRegex = "^[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]+$".toRegex()
            if (!email.matches(emailRegex)) {
                emailLayout.error = "Невірний формат email"
                isValid = false
            }

            // Проверка пароля
            if (password.length < 6) {
                passwordLayout.error = "Пароль має бути не менше 6 символів"
                isValid = false
            } else if (!password.any { it.isLowerCase() }) {
                passwordLayout.error = "Пароль має містити хоча б одну малу літеру"
                isValid = false
            } else if (!password.any { it.isUpperCase() }) {
                passwordLayout.error = "Пароль має містити хоча б одну велику літеру"
                isValid = false
            } else if (!password.contains('.') && !password.contains('_')) {
                passwordLayout.error = "Пароль має містити хоча б один символ"
                isValid = false
            }

            if (isValid) {
                registerUser(fullName, username, email, password)
            }

        }

        val loginTextView: TextView = view.findViewById(R.id.textViewLink)
        loginTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, FragmentLogin()) // замените FragmentLogin() на ваш логин-фрагмент
                .addToBackStack(null)
                .commit()
        }


        return view
    }

    private fun clearErrors() {
        fullNameLayout.error = null
        usernameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
    }


    private fun registerUser(fullName: String, username: String, email: String, password: String) {
        // Регистрируем пользователя с почтой и паролем
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveUserData(it, fullName, username)
                    }
                }
            }
            .addOnFailureListener { e ->
                when {
                    e is FirebaseAuthUserCollisionException -> {
                        emailLayout.error = "Електронна адреса вже використовується"
                    }
                    else -> {
                        Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun saveUserData(user: FirebaseUser, fullName: String, username: String) {
        val userData = hashMapOf(
            "fullName" to fullName,
            "username" to username,
            "email" to user.email,
            "photoUrl" to user.photoUrl?.toString()
        )

        // Сохраняем данные в Firestore
        db.collection("users").document(user.uid).set(userData)
            .addOnSuccessListener {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()

                user.updateProfile(profileUpdates).addOnCompleteListener {
                    val intent = Intent(requireActivity(), ActivityMain::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                    Toast.makeText(context, "Вітаємо!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





}