package com.example.yourlibrary_palazova

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.yourlibrary_palazova.databinding.ActivityAddBookBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

class ActivityAddBook : AppCompatActivity() {

    private lateinit var binding: ActivityAddBookBinding
    private val booksViewModel: BooksViewModel by viewModels()

    private var favoritesStatus: Boolean = false
    private var coverUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDatePickers() // відкриття Date Pickers

        val action = intent.getStringExtra("action")
        val bookId = intent.getStringExtra("bookId")

        if (action == "edit book") {
            Log.d("BookEdit", "Action: $action")
            Log.d("BookEdit", "BookId: $bookId")
            Log.d("BookEdit", "Title: ${intent.getStringExtra("title")}")
            Log.d("BookEdit", "Author: ${intent.getStringExtra("author")}")

            binding.pageTitle.text = "Редагувати"
            binding.addBook.text = "Зберегти зміни"

            // Заблокировать неизменяемые поля
            binding.textInputTitle.isEnabled = false
            binding.textInputAuthor.isEnabled = false
            binding.textInputStart.isEnabled = false

            // Установить значения в поля
            binding.textInputTitle.setText(intent.getStringExtra("title"))
            binding.textInputAuthor.setText(intent.getStringExtra("author"))
            binding.textInputStart.setText(intent.getStringExtra("startDate"))
            binding.textInputFinish.setText(intent.getStringExtra("endDate"))

            val rating = intent.getIntExtra("rating", 0)
            binding.ratingBar.rating = rating / 2f

            val quotes = intent.getStringArrayListExtra("quotes")?.joinToString("\n") ?: ""
            val notes = intent.getStringArrayListExtra("notes")?.joinToString("\n") ?: ""

            binding.textInputDescription.setText(quotes)
            binding.textInputNotes.setText(notes)

            favoritesStatus = intent.getBooleanExtra("favorites", false)
            coverUri = intent.getStringExtra("coverUri")?.toUri()
        }

        binding.addBook.setOnClickListener {
            val title = binding.textInputTitle.text.toString().trim()
            val author = binding.textInputAuthor.text.toString().trim()
            val startDate = binding.textInputStart.text.toString().trim()
            val finishDate = binding.textInputFinish.text.toString().trim()
            val ratingValue = (binding.ratingBar.rating * 2).toInt()
            val quotesList = binding.textInputDescription.text.toString()
                .lines().map { it.trim() }.filter { it.isNotBlank() }
            val notesList = binding.textInputNotes.text.toString()
                .lines().map { it.trim() }.filter { it.isNotBlank() }

            // Очистка предыдущих ошибок
            binding.textInputLayout5.error = null
            binding.textInputLayout6.error = null
            binding.textInputLayout7.error = null

            var hasError = false

            if (author.isBlank()) {
                binding.textInputLayout5.error = "Введіть автора"
                hasError = true
            }
            if (title.isBlank()) {
                binding.textInputLayout6.error = "Введіть назву книги"
                hasError = true
            }
            if (startDate.isBlank()) {
                binding.textInputLayout7.error = "Введіть дату початку"
                hasError = true
            }

            if (hasError) return@setOnClickListener


            if (action == "edit book" && bookId != null) {
                val updatedBook = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    startDate = startDate,
                    endDate = finishDate.ifEmpty { null },
                    rating = ratingValue,
                    favorites = favoritesStatus,
                    quotes = quotesList,
                    notes = notesList,
                    timestamp = System.currentTimeMillis(),
                    coverUri = coverUri.toString()
                )
                booksViewModel.updateBook(
                    bookId = bookId,
                    updatedBook = updatedBook,
                    onSuccess = {
                        Log.d("UpdateBook", "Книга ${bookId}  успішно оновлена")
                        Toast.makeText(this, "Книгу оновлено", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onFailure = { e ->
                        Log.d("Activity Add Book - Edit Book", "Помилка: ${e.message}")
                    }
                )
            } else {
                val newBook = Book(
                    title = title,
                    author = author,
                    startDate = startDate,
                    endDate = finishDate.ifEmpty { null },
                    rating = ratingValue,
                    favorites = false,
                    quotes = quotesList,
                    notes = notesList,
                    timestamp = System.currentTimeMillis(),
                    coverUri = null
                )
                booksViewModel.addBook(
                    book = newBook,
                    onSuccess = {
                        Toast.makeText(this, "Книгу додано", Toast.LENGTH_SHORT).show()
                        finish() // Закрываем активность
                    },
                    onFailure = {
                        Toast.makeText(this, "Помилка: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun setupDatePickers() {
        val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        fun showDatePicker(onDateSelected: (String) -> Unit) {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_App_DatePicker)
                .setTitleText("Оберіть дату")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.show(supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { selection ->
                val formattedDate = dateFormatter.format(Date(selection))
                onDateSelected(formattedDate)
            }
        }

        binding.textInputStart.setOnClickListener {
            showDatePicker { date -> binding.textInputStart.setText(date) }
        }

        binding.textInputFinish.setOnClickListener {
            showDatePicker { selectedDate ->
                val startText = binding.textInputStart.text.toString()
                if (startText.isNotBlank()) {
                    try {
                        val startDate = dateFormatter.parse(startText)
                        val endDate = dateFormatter.parse(selectedDate)

                        if (startDate != null && endDate != null && endDate.before(startDate)) {
                            Toast.makeText(
                                this,
                                "Дата завершення не може бути раніше дати початку",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@showDatePicker
                        }
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }
                }
                binding.textInputFinish.setText(selectedDate)
            }
        }

        // перехід actionNext з попереднього поля
        binding.textInputStart.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker { date -> binding.textInputStart.setText(date) }
            }
        }
    }
}