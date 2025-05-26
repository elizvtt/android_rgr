package com.example.yourlibrary_palazova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import me.zhanghai.android.materialratingbar.MaterialRatingBar

class ActivityBookPage : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var bookId: String
    private lateinit var buttonEdit: ImageButton
    private lateinit var buttonBack: ImageButton
    private lateinit var currentBook: Book
    private lateinit var editBookLauncher: ActivityResultLauncher<Intent>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_page)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bookId = intent.getStringExtra("BOOK_ID") ?: ""
        buttonEdit = findViewById(R.id.imageButtonEdit)
        buttonBack = findViewById(R.id.buttonBack)

        if (bookId.isNotEmpty()) {
            loadBookDetails(bookId)
        } else {
            Log.d("Activity Book Page", "Book ID не задан")
            finish()
        }

        editBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Книга оновлена — онови дані
                loadBookDetails(bookId)
            }
        }

        buttonEdit.setOnClickListener {
            val intent = Intent(this, ActivityAddBook::class.java).apply {
                putExtra("action", "edit book")
                putExtra("bookId", bookId)
                putExtra("title", currentBook.title)
                putExtra("author", currentBook.author)
                putExtra("startDate", currentBook.startDate)
                putExtra("endDate", currentBook.endDate)
                putExtra("rating", currentBook.rating)
                putStringArrayListExtra("notes", ArrayList(currentBook.notes ?: listOf()))
                putStringArrayListExtra("quotes", ArrayList(currentBook.quotes ?: listOf()))
            }

            editBookLauncher.launch(intent)
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun loadBookDetails(bookId: String) {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val book = document.toObject(Book::class.java)
                    book?.let { bindBookDetails(it) }
                } else {
                    Log.d("Activity Book Page", "Книга не знайдена")
                    finish()
                }
            }
            .addOnFailureListener {
                Log.d("Activity Book Page", "Помилка завантаження книги")
            }
    }

    private fun bindBookDetails(book: Book) {
        currentBook = book
        findViewById<TextView>(R.id.bookTitle).text = book.title
        findViewById<TextView>(R.id.bookAuthor).text = book.author
        findViewById<TextView>(R.id.startDate).text = "Почато: ${book.startDate ?: "-"}"
        findViewById<TextView>(R.id.endDate).text = "Прочитано: ${book.endDate ?: "-"}"
        setupRatingBar(book.rating)
        setupQuotes(book.quotes)
        setupNotes(book.notes)
    }

    private fun setupRatingBar(rating: Int) {
        val ratingBar2 = findViewById<MaterialRatingBar>(R.id.ratingBar2)
        ratingBar2.numStars = 5
        ratingBar2.rating = rating / 2f
        ratingBar2.stepSize = 0.5f
    }

    private fun setupQuotes(quotes: List<String>?) {
        val quotesContainer = findViewById<LinearLayout>(R.id.quotesContainer)
        val quotesBlock = findViewById<LinearLayout>(R.id.quotesBlock)
        quotesContainer.removeAllViews()
        if (quotes.isNullOrEmpty()) {
            quotesBlock.visibility = View.GONE
        } else {
            quotesBlock.visibility = View.VISIBLE
            quotes.forEach { quote ->
                val textView = TextView(this)
                textView.text = "«$quote»"
                textView.setPadding(8, 4, 8, 4)
                quotesContainer.addView(textView)
            }
        }

        // Кнопка сворачивания/разворачивания
        findViewById<ImageButton>(R.id.buttonToggleQuotes).setOnClickListener {
            if (quotesContainer.visibility == View.GONE) {
                quotesContainer.visibility = View.VISIBLE
            } else {
                quotesContainer.visibility = View.GONE
            }
        }
    }

    private fun setupNotes(notes: List<String>?) {
        val notesContainer = findViewById<LinearLayout>(R.id.notesContainer)
        val notesBlock = findViewById<LinearLayout>(R.id.notesBlock)
        notesContainer.removeAllViews()
        if (notes.isNullOrEmpty()) {
            notesBlock.visibility = View.GONE
        } else {
            notesBlock.visibility = View.VISIBLE
            notes.forEach { note ->
                val textView = TextView(this)
                textView.text = "$note"
                textView.setPadding(8, 4, 8, 4)
                notesContainer.addView(textView)
            }
        }

        findViewById<ImageButton>(R.id.buttonToggleNotes).setOnClickListener {
            if (notesContainer.visibility == View.GONE) {
                notesContainer.visibility = View.VISIBLE
            } else {
                notesContainer.visibility = View.GONE
            }
        }
    }

}
