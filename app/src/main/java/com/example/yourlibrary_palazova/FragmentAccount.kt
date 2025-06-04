package com.example.yourlibrary_palazova

import android.content.Intent
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
import com.makeramen.roundedimageview.RoundedImageView
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

        // перевіряємо авторизацію користувача
        val isLoggedIn = isUserLoggedIn()

        booksViewModel = ViewModelProvider(requireActivity())[BooksViewModel::class.java]
        booksViewModel.books.observe(viewLifecycleOwner) { books ->
            updateUI(isLoggedIn, books)
        }

        booksViewModel.avatarBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                accountPhoto.setImageBitmap(bitmap)
                accountPhoto.borderColor = "#EDE9DF".toColorInt()
            } else {
                showDefaultAvatar()
            }
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            booksViewModel.loadAvatar(it)
        }

        if (!isUserLoggedIn()) {
            // обробка натискань кнопок для неавторизованого користувача
            signInButton.setOnClickListener {
                val intent = Intent(activity, ActivityAuth::class.java)
                intent.putExtra("action", "signUp")
                startActivity(intent)
            }

            logInButton.setOnClickListener {
                val intent = Intent(activity, ActivityAuth::class.java)
                intent.putExtra("action", "logIn")
                startActivity(intent)
            }

        } else {
            // відкриття BottomSheet для редагування профілю
            editProfileButton.setOnClickListener {
                val bottomSheet = BottomSheetOptions()
                bottomSheet.setAvatarUpdateListener(this)
                bottomSheet.show(parentFragmentManager, "BottomSheetOptions")
            }
        }
        return view
    }

    // завантаження книг після повернення на екран
    override fun onResume() {
        super.onResume()
        booksViewModel.loadBooks()
    }

    // Оновлення інтерфейсу залежно від статусу авторизації
    private fun updateUI(isLoggedIn: Boolean, books: List<Book>? = null) {
        val notLoggedInViews = listOf(emptyText1, emptyText2, signInButton, logInButton)
        val loggedInViews = listOf(
            accountName,
            editProfileButton,
            textReadAll,
            textReadLatest,
            textReading,
            favoritesBlock,
            accountPhoto
        )

        if (!isLoggedIn) {
            // показуємо текст та кнопки для неавторизованого користувача
            notLoggedInViews.forEach { it.visibility = View.VISIBLE }
            loggedInViews.forEach { it.visibility = View.INVISIBLE }
        } else {
            // показуємо інші текст та кнопки для авторизованого користувача
            notLoggedInViews.forEach { it.visibility = View.INVISIBLE }
            loggedInViews.forEach { it.visibility = View.VISIBLE }

            val user = FirebaseAuth.getInstance().currentUser
            accountName.text = user?.displayName ?: "Username"

            user?.uid?.let { userId ->
                booksViewModel.loadAvatar(userId)
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

    // обробка видалення аватара
    override fun onAvatarDeleted() {
        showDefaultAvatar()
    }

    // встановлення дефолтного аватара
    private fun showDefaultAvatar() {
        accountPhoto.setImageResource(R.drawable.icon_profile)
        accountPhoto.borderColor = "#241203".toColorInt()
    }

    // блок обраних книг
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
