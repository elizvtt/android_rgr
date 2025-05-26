package com.example.yourlibrary_palazova

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BooksViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> get() = _books

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> get() = _selectedBook


    init {
        loadBooks()
    }


    fun loadBooks() {
        val user = auth.currentUser
        if (user == null) {
            _books.value = emptyList() // Відразу показуємо пустий список
            return
        }
        db.collection("users")
            .document(user.uid)
            .collection("books")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val bookList = result.mapNotNull { it.toObject(Book::class.java) }
                _books.value = bookList
            }
            .addOnFailureListener { e ->
                Log.e("BookViewModel", "Помилка завантаження книг: ${e.message}", e)
            }
    }


    fun addBook(
        book: Book,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val booksRef = db.collection("users")
            .document(user.uid)
            .collection("books")

        // Создаём новый документ с заранее сгенерированным ID
        val newDocRef = booksRef.document()
        val bookWithId = book.copy(id = newDocRef.id)

        newDocRef.set(bookWithId)
            .addOnSuccessListener {
                loadBooks()  // Обновляем список книг
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }


    fun deleteBook(
        bookId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val bookDocRef = db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)

        bookDocRef.delete()
            .addOnSuccessListener {
                loadBooks()  // Обновляем список книг после удаления
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateBook (
        bookId: String,
        updatedBook: Book,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val bookDocRef = db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)

        bookDocRef.set(updatedBook)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loadBookDetails(bookId: String) {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val book = document.toObject(Book::class.java)
                    _selectedBook.value = book
                } else {
                    _selectedBook.value = null
                    Log.d("BooksViewModel", "Книга не знайдена")
                }
            }
            .addOnFailureListener {
                _selectedBook.value = null
                Log.d("BooksViewModel", "Помилка завантаження книги: ${it.message}")
            }
    }

    fun updateFavoriteStatus(
        bookId: String,
        isFavorite: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)
            .update("favorites", isFavorite)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCover(
        bookId: String,
        coverUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val bookDocRef = db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)

        val updateData = mapOf(
            "coverUri" to coverUri?.toString()
        )

        bookDocRef.update(updateData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }

    }


}
