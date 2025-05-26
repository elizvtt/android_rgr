package com.example.yourlibrary_palazova

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.makeramen.roundedimageview.RoundedImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class FragmentAccount : Fragment(), BottomSheetOptions.AvatarUpdateListener {

    private lateinit var emptyText1: TextView
    private lateinit var emptyText2: TextView
    private lateinit var signInButton: Button
    private lateinit var logInButton: Button

    private lateinit var accountPhoto: RoundedImageView
    private lateinit var accountName: TextView
    private lateinit var textReadAll: TextView
    private lateinit var textReadLatest: TextView
    private lateinit var textReading: TextView

    private lateinit var editProfileButton: ImageButton

    private lateinit var favoritesBlock: LinearLayout

    private lateinit var  booksViewModel: BooksViewModel

    private fun isUserLoggedIn(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user != null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        emptyText1 = view.findViewById(R.id.emptyText1)
        emptyText2 = view.findViewById(R.id.emptyText2)
        signInButton = view.findViewById(R.id.signInButton)
        logInButton = view.findViewById(R.id.logInButton)

        accountPhoto = view.findViewById(R.id.accountPhoto)
        accountName = view.findViewById(R.id.accountName)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        textReadAll = view.findViewById(R.id.textReadAll)
        textReadLatest = view.findViewById(R.id.textReadLatest)
        textReading = view.findViewById(R.id.textReading)
        favoritesBlock = view.findViewById(R.id.favoritesBlock)

        // Проверяем, авторизован ли пользователь
        val isLoggedIn = isUserLoggedIn()

        booksViewModel = ViewModelProvider(requireActivity())[BooksViewModel::class.java]
        booksViewModel.books.observe(viewLifecycleOwner) { books ->
            updateUI(isLoggedIn, books)
        }

        if (!isUserLoggedIn()) {
            // обработчик для кнопки регистрации
            signInButton.setOnClickListener {
                val intent = Intent(activity, ActivityAuth::class.java)
                intent.putExtra("action", "signUp")
                startActivity(intent)
            }

            // обработчик для кнопки входа
            logInButton.setOnClickListener {
                val intent = Intent(activity, ActivityAuth::class.java)
                intent.putExtra("action", "logIn")
                startActivity(intent)
            }

        } else {
            editProfileButton.setOnClickListener {
                val bottomSheet = BottomSheetOptions()
                bottomSheet.setAvatarUpdateListener(this)
                bottomSheet.show(parentFragmentManager, "BottomSheetOptions")
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        booksViewModel.loadBooks()
    }


    private fun updateUI(isLoggedIn: Boolean, books: List<Book>? = null) {
        val notLoggedInViews = listOf(emptyText1, emptyText2, signInButton, logInButton)
        val loggedInViews = listOf(accountName, editProfileButton, textReadAll, textReadLatest, textReading, favoritesBlock, accountPhoto)

        if (!isLoggedIn) {
            // Показываем тексты и кнопки для неавторизованного пользователя
            notLoggedInViews.forEach { it.visibility = View.VISIBLE }
            loggedInViews.forEach { it.visibility = View.INVISIBLE }
        } else {
            // Прячем тексты и кнопки для авторизованного пользователя
            notLoggedInViews.forEach { it.visibility = View.INVISIBLE }
            loggedInViews.forEach { it.visibility = View.VISIBLE }

            val user = FirebaseAuth.getInstance().currentUser
            accountName.text = user?.displayName ?: "Username"

            user?.uid?.let { userId ->
                loadAvatar(userId)
            }

            if (books != null) {
                val totalBooks = books.count { it.endDate != null }
                textReadAll.text = "Прочитано всього: $totalBooks"

                val twoMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -2)
                }.time

                val booksWithinTwoMonths = books.filter { book ->
                    val endDate = book.endDate?.let {
                        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                        dateFormat.parse(it)
                    }
                    endDate != null && endDate.after(twoMonthsAgo)
                }

                val booksWithoutEndDate = books.filter { it.endDate == null }

                textReadLatest.text = "Прочитано за останній час: ${booksWithinTwoMonths.size}"
                textReading.text = "Читаю: ${booksWithoutEndDate.size}"

                val favoriteBooks = books.filter { it.favorites == true }
                setupFavoriteBooks(favoriteBooks)
            }
        }

    }

    private fun loadAvatar(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val path = doc.getString("photoUrl")
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        accountPhoto.setImageBitmap(bitmap)
                        accountPhoto.borderColor = "#EDE9DF".toColorInt()
                    } else showDefaultAvatar()
                } else showDefaultAvatar()
            }
            // ошибка загрузки - показываем плейсхолдер
            .addOnFailureListener { showDefaultAvatar() }
    }

    override fun onAvatarDeleted() {
        showDefaultAvatar()
    }

    private fun showDefaultAvatar() {
        accountPhoto.setImageResource(R.drawable.icon_profile)
        accountPhoto.borderColor = "#241203".toColorInt()
    }

    private fun setupFavoriteBooks(favoriteBooks: List<Book>?) {
        val favoritesContainer = view?.findViewById<LinearLayout>(R.id.favoritesContainer)
        val favoritesScrollView = view?.findViewById<ScrollView>(R.id.favoritesScrollView)
        val toggleButton = view?.findViewById<ImageButton>(R.id.buttonToggleFavorites)

        favoritesContainer?.removeAllViews()

        if (favoriteBooks.isNullOrEmpty()) {
            favoritesContainer?.visibility = View.GONE
            favoritesScrollView?.visibility = View.GONE
        } else {

            favoriteBooks.forEach { book ->
                val textView = TextView(requireContext())
                textView.text = "• «${book.title}» - ${book.author}"
                textView.setPadding(8, 4, 8, 4)
                favoritesContainer?.addView(textView)
            }
        }

        toggleButton?.setOnClickListener {
            val isVisible = favoritesScrollView?.visibility == View.VISIBLE
            val newVisibility = if (isVisible) View.GONE else View.VISIBLE
            favoritesScrollView?.visibility = newVisibility
        }
    }

}
