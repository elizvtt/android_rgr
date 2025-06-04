package com.example.yourlibrary_palazova

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File

class BooksViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LiveData зі списком книг
    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> get() = _books

    // LiveData для вибраної книги
    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> get() = _selectedBook

    // LiveData для аватарки користувача
    private val _avatarBitmap = MutableLiveData<Bitmap?>()
    val avatarBitmap: LiveData<Bitmap?> get() = _avatarBitmap

    init {
        loadBooks()
    }

    // завантаження списку книг з Firestore
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

    // додаємо нову книгу до Firestore та оновлюємо список
    fun addBook(
        book: Book,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val booksRef = db.collection("users")
            .document(user.uid)
            .collection("books")

        // Генеруємо унікальний ID
        val newDocRef = booksRef.document()
        val bookWithId = book.copy(id = newDocRef.id)

        newDocRef.set(bookWithId)
            .addOnSuccessListener {
                loadBooks()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // видаляємо книгу за ID та оновлюємо список
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
                loadBooks()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // оновлення інформації про книгу
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

    // завантажуємо деталі конкретної книги за її ID
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
                    _selectedBook.value = book // оновлюємо LiveData зі вибраною книгою
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

    // оновлюємо статус обраної книги
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

    // оновлюємо URI обкладинки книги у Firestore
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

    // збереження шляху обкладинки
    fun saveCoverPath(
        bookId: String,
        path: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val bookRef = db.collection("users")
            .document(user.uid)
            .collection("books")
            .document(bookId)

        val data = mapOf("coverUri" to path)
        bookRef.set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loadAvatar(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val path = doc.getString("photoUrl")
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        _avatarBitmap.value = bitmap
                    } else {
                        _avatarBitmap.value = null
                    }
                } else {
                    _avatarBitmap.value = null
                }
            }
            .addOnFailureListener {
                _avatarBitmap.value = null
            }
    }
}
