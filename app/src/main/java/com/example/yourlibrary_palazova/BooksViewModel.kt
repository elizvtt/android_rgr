package com.example.yourlibrary_palazova

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


    fun addBook(book: Book, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
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


    fun deleteBook(bookId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
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

}
