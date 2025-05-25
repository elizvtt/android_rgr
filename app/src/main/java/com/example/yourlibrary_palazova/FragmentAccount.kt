package com.example.yourlibrary_palazova

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.getstream.avatarview.AvatarView
import io.getstream.avatarview.coil.loadImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class FragmentAccount : Fragment() {

    private lateinit var emptyText1: TextView
    private lateinit var emptyText2: TextView
    private lateinit var signInButton: Button
    private lateinit var logInButton: Button

    private lateinit var accountPhoto: ImageView
    private lateinit var accountPhoto2: AvatarView
    private lateinit var accountName: TextView
    private lateinit var textReadAll: TextView
    private lateinit var textReadLatest: TextView
    private lateinit var textReading: TextView
    private lateinit var textLinkFav: TextView

    private lateinit var editProfileButton: ImageButton


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
        accountPhoto2 = view.findViewById(R.id.accountPhoto2)
        accountName = view.findViewById(R.id.accountName)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        textReadAll = view.findViewById(R.id.textReadAll)
        textReadLatest = view.findViewById(R.id.textReadLatest)
        textReading = view.findViewById(R.id.textReading)
        textLinkFav = view.findViewById(R.id.textLinkFav)


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
        if (!isLoggedIn) {
            // Показываем тексты и кнопки для неавторизованного пользователя
            emptyText1.visibility = View.VISIBLE
            emptyText2.visibility = View.VISIBLE
            signInButton.visibility = View.VISIBLE
            logInButton.visibility = View.VISIBLE

//            accountPhoto.visibility = View.INVISIBLE
            accountName.visibility = View.INVISIBLE
            editProfileButton.visibility = View.INVISIBLE
            textReadAll.visibility = View.INVISIBLE
            textReadLatest.visibility = View.INVISIBLE
            textReading.visibility = View.INVISIBLE
            textLinkFav.visibility = View.INVISIBLE

        } else {
            // Прячем тексты и кнопки для авторизованного пользователя
            emptyText1.visibility = View.INVISIBLE
            emptyText2.visibility = View.INVISIBLE
            signInButton.visibility = View.INVISIBLE
            logInButton.visibility = View.INVISIBLE

//            accountPhoto.visibility = View.VISIBLE
            accountName.visibility = View.VISIBLE
            editProfileButton.visibility = View.VISIBLE
            textReadAll.visibility = View.VISIBLE
            textReadLatest.visibility = View.VISIBLE
            textReading.visibility = View.VISIBLE
            textLinkFav.visibility = View.VISIBLE

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
                        accountPhoto2.visibility = View.VISIBLE
                        accountPhoto.visibility = View.GONE
                        accountPhoto2.loadImage(bitmap)
                    } else {
                        // Файл локально не найден — поставить плейсхолдер
                        accountPhoto2.visibility = View.GONE
                        accountPhoto.visibility = View.VISIBLE
                        accountPhoto.setImageResource(R.drawable.icon_profile)
                    }
                } else {
                    // фото отсутствует, показываем плейсхолдер
                    accountPhoto2.visibility = View.GONE
                    accountPhoto.visibility = View.VISIBLE
                    accountPhoto.setImageResource(R.drawable.icon_profile)
                }
            }
            .addOnFailureListener {
                // ошибка загрузки - показываем плейсхолдер
                accountPhoto2.visibility = View.GONE
                accountPhoto.visibility = View.VISIBLE
                accountPhoto.setImageResource(R.drawable.icon_profile)
            }
    }
}
