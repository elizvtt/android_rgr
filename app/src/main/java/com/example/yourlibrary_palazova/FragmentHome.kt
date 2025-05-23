package com.example.yourlibrary_palazova

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlin.text.lowercase
import androidx.core.graphics.toColorInt
import com.example.yourlibrary_palazova.helpers.SwipeHelper

class FragmentHome : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var booksViewModel: BooksViewModel

    private val addBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            booksViewModel.loadBooks() // обновляем список книг после успешного добавления
        }
    }

    private val bookList = mutableListOf<Book>()
    private val allBooks = mutableListOf<Book>()

    private var currentOnlyFavorites = false
    private var currentOnlyCompleted = false
    private var currentOnlyQuotes = false
    private var currentOnlyNotes = false
    private var currentMinRating = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        booksViewModel = ViewModelProvider(requireActivity())[BooksViewModel::class.java]

        bookAdapter = BookAdapter(bookList, object : BookAdapter.HeaderClickListener {
            override fun onFilterClicked() {
                val dialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter, null)

                val checkFavorites = dialogView.findViewById<CheckBox>(R.id.checkFavorites)
                val checkCompleted = dialogView.findViewById<CheckBox>(R.id.checkCompleted)
                val checkQuotes = dialogView.findViewById<CheckBox>(R.id.checkQuotes)
                val checkNotes = dialogView.findViewById<CheckBox>(R.id.checkNotes)
                val seekRating = dialogView.findViewById<SeekBar>(R.id.seekRating)
                val textRating = dialogView.findViewById<TextView>(R.id.textRatingValue)

                seekRating.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        textRating.text = "$progress/10"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

                val dialog =
                    AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_FilterDialog)
                        .setView(dialogView)
                        .setPositiveButton("Застосувати") { _, _ ->
                            currentOnlyFavorites = checkFavorites.isChecked
                            currentOnlyCompleted = checkCompleted.isChecked
                            currentOnlyQuotes = checkQuotes.isChecked
                            currentOnlyNotes = checkNotes.isChecked
                            currentMinRating = seekRating.progress

                            val noFiltersSelected =
                                !currentOnlyFavorites &&
                                        !currentOnlyCompleted &&
                                        !currentOnlyQuotes &&
                                        !currentOnlyNotes &&
                                        currentMinRating == 0

                            val filtersList = buildActiveFiltersList()

                            if (noFiltersSelected) {
                                filterBooks() // Показать все книги
                                bookAdapter.updateActiveFilters(filtersList)
                            } else {
                                filterBooks(
                                    onlyFavorites = currentOnlyFavorites,
                                    onlyCompleted = currentOnlyCompleted,
                                    onlyWithQuotes = currentOnlyQuotes,
                                    onlyWithNotes = currentOnlyNotes,
                                    minRating = currentMinRating
                                )

                                bookAdapter.updateActiveFilters(filtersList)

                                Toast.makeText(context, "Фільтри застосовані", Toast.LENGTH_SHORT).show()
                            }
                        }

                        .setNegativeButton("Скасувати") { dialogInterface: DialogInterface, _ ->
                            currentOnlyFavorites = false
                            currentOnlyCompleted = false
                            currentOnlyQuotes = false
                            currentOnlyNotes = false
                            currentMinRating = 0

                            filterBooks()
                            val filtersList = buildActiveFiltersList()
                            bookAdapter.updateActiveFilters(filtersList)

                        }
                        .create()

                checkFavorites.isChecked = currentOnlyFavorites
                checkCompleted.isChecked = currentOnlyCompleted
                checkQuotes.isChecked = currentOnlyQuotes
                checkNotes.isChecked = currentOnlyNotes
                seekRating.progress = currentMinRating
                textRating.text = "$currentMinRating/10"

                dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_background)
                dialog.show()
                val width =
                    (resources.displayMetrics.widthPixels - (2 * resources.getDimensionPixelSize(R.dimen.dialog_horizontal_margin)))
                dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            }

            override fun onSortClicked() {
                setupSortPopup()
            }
        },
            object : BookAdapter.OnBookClickListener {
                override fun onBookClick(book: Book) {
                    val intent = Intent(requireContext(), BookPageActivity::class.java)
                    intent.putExtra("BOOK_ID", book.id)
                    startActivity(intent)
                }
            }
        )

        applySavedSortPreferences()
        bookAdapter.notifyDataSetChanged()


        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = bookAdapter


        booksViewModel.books.observe(viewLifecycleOwner) { books ->
            allBooks.clear()
            allBooks.addAll(books)

            filterBooks(
                onlyFavorites = currentOnlyFavorites,
                onlyCompleted = currentOnlyCompleted,
                onlyWithQuotes = currentOnlyQuotes,
                onlyWithNotes = currentOnlyNotes,
                minRating = currentMinRating
            )
        }

        val fabAddBook = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)

        fabAddBook.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                user.reload().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (auth.currentUser != null) {
                            val intent = Intent(requireContext(), AddBookActivity::class.java)
//                            startActivity(intent)
                            addBookLauncher.launch(intent)
                        }
                    } else Log.d("FRAGMENT-HOME", "Помилка: ${task.exception?.message}")
                }
            } else showAuthRequiredDialog()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeHelper = object : SwipeHelper(recyclerView) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {

                return listOf(
                        UnderlayButton(
                            requireContext(),
                            "Delete",
                            14f,
                            android.R.color.transparent,
                            R.drawable.icon_delete, // замените на свою иконку
                            70,
                            object : UnderlayButtonClickListener {

                                override fun onClick() {
                                    val listIndex = position - 1

                                    if (listIndex >= 0 && listIndex < bookList.size) {
                                        val book = bookList[listIndex]

                                        // Удаление книги
                                        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogStyle)
                                            .setTitle("Видалити")
                                            .setMessage("Ви впевнені що хочете видалити запис про книгу назавжди?")
                                            .setPositiveButton("Так") { _, _ ->
                                                booksViewModel.deleteBook(
                                                    book.id,
                                                    onSuccess = {
                                                        Log.d("SwipeHelper", "Успішно видалено книгу з id=${book.id}")
                                                        bookList.removeAt(listIndex)
                                                        Log.d("SwipeHelper", "Список залишившихся книг: ${bookList.map { it.title }}")

                                                        bookAdapter.notifyItemRemoved(position)
                                                        bookAdapter.notifyItemRangeChanged(position, bookList.size - listIndex)
                                                        Toast.makeText(requireContext(), "Книгу видалено", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onFailure = { e ->
                                                        Log.d("FRAGMENT-HOME", "Помилка видалення: ${e.message}")
                                                    }
                                                )
                                            }
                                            .setNegativeButton("Ні") { dialog, _ -> dialog.dismiss() }
                                            .create()

                                        dialog.show()
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                                            setTextColor(Color.RED)
                                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                        }
//
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                                            setTextColor("#241203".toColorInt())
                                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                        }

                                    } else Log.d("FRAGMENT-HOME", "Помилка: невірна позиція ${position}")
                                }
                            }
                        )
                )

            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onResume() {
        super.onResume()
        booksViewModel.loadBooks() // Перезавантаження списку
        filterBooks(
            onlyFavorites = currentOnlyFavorites,
            onlyCompleted = currentOnlyCompleted,
            onlyWithQuotes = currentOnlyQuotes,
            onlyWithNotes = currentOnlyNotes,
            minRating = currentMinRating
        )
    }


    private fun showAuthRequiredDialog() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_FilterDialog)
            .setTitle("Не авторизовані")
            .setMessage("Ви не авторизовані.\nЩоб додати книгу, спершу потрібно увійти.")
            .setPositiveButton("Продовжити") { dialog, _ ->
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("action", "signUp")
                startActivity(intent)
                dialog.dismiss()
            }
        dialog.show()
    }


    private fun sortBooksByTitle(ascending: Boolean) {
        if (ascending) {
            bookList.sortBy { it.title.lowercase() }
        } else bookList.sortByDescending { it.title }

    }

    private fun sortBooksByAuthor(ascending: Boolean) {
        if (ascending) {
        bookList.sortBy { it.author.lowercase() }
        } else bookList.sortByDescending { it.author }
    }

    private fun sortBooksByRating(ascending: Boolean) {
        if (ascending) {
            bookList.sortBy { it.rating }
        } else bookList.sortByDescending { it.rating }
    }

    private fun sortBooksByDate(ascending: Boolean) {
        if (ascending) {
            bookList.sortBy { it.timestamp }
        } else bookList.sortByDescending { it.timestamp }
    }

    private fun applySavedSortPreferences() {
        val prefs = requireContext().getSharedPreferences("SortPrefs", Context.MODE_PRIVATE)
        val sortOption = prefs.getInt("sortOption", -1)
        val sortOrder = prefs.getInt("sortOrder", 0)

        when (sortOption) {
            0 -> sortBooksByTitle(sortOrder == 0)
            1 -> sortBooksByAuthor(sortOrder == 0)
            2 -> sortBooksByRating(sortOrder == 0)
            3 -> sortBooksByDate(sortOrder == 0)
        }
    }

    private fun filterBooks(
        onlyFavorites: Boolean = false,
        onlyWithQuotes: Boolean = false,
        onlyWithNotes: Boolean = false,
        onlyCompleted: Boolean = false,
        authorName: String? = null,
        minRating: Int = 0
    ) {
        val filteredList = allBooks.filter { book ->
            (!onlyFavorites || book.favorites) &&
                    (book.rating >= minRating) &&
                    (!onlyWithQuotes || book.quotes.isNotEmpty()) &&
                    (!onlyWithNotes || book.notes.isNotEmpty()) &&
                    (!onlyCompleted || book.endDate != null) &&
                    (authorName.isNullOrBlank() || book.author.contains(authorName, ignoreCase = true))
        }

        bookList.clear()
        bookList.addAll(filteredList)

        applySavedSortPreferences()
        bookAdapter.notifyDataSetChanged()
    }

    private fun buildActiveFiltersList(): List<String> {
        val filters = mutableListOf<String>()

        if (currentOnlyFavorites) filters.add("Обрані")
        if (currentOnlyCompleted) filters.add("Прочитані")
        if (currentOnlyQuotes) filters.add("З цитатами")
        if (currentOnlyNotes) filters.add("З нотатками")
        if (currentMinRating > 0) filters.add("Рейтинг від $currentMinRating")

        return filters
    }

    private fun setupSortPopup() {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.PopupMenuStyle)
        val popup = PopupMenu(themedContext, requireView().findViewById(R.id.buttonSort))
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val prefs = requireContext().getSharedPreferences("SortPrefs", Context.MODE_PRIVATE)
        val sortOption = prefs.getInt("sortOption", -1)
        val sortOrder = prefs.getInt("sortOrder", 0)

        val iconRes = if (sortOrder == 0) R.drawable.icon_arrow_down else R.drawable.icon_arrow_up

        when (sortOption) {
            0 -> popup.menu.findItem(R.id.sortByTitle)?.icon = ContextCompat.getDrawable(requireContext(), iconRes)
            1 -> popup.menu.findItem(R.id.sortByAuthor)?.icon = ContextCompat.getDrawable(requireContext(), iconRes)
            2 -> popup.menu.findItem(R.id.sortByRating)?.icon = ContextCompat.getDrawable(requireContext(), iconRes)
            3 -> popup.menu.findItem(R.id.sortByDate)?.icon = ContextCompat.getDrawable(requireContext(), iconRes)
        }

        popup.setOnMenuItemClickListener { item ->
            val currentSortOption = prefs.getInt("sortOption", -1)
            var sortOrder = prefs.getInt("sortOrder", 0)

            val selectedOption = when (item.itemId) {
                R.id.sortByTitle -> 0
                R.id.sortByAuthor -> 1
                R.id.sortByRating -> 2
                R.id.sortByDate -> 3
                else -> return@setOnMenuItemClickListener false
            }

            sortOrder = if (selectedOption == currentSortOption) {
                1 - sortOrder
            } else {
                0
            }

            prefs.edit()
                .putInt("sortOption", selectedOption)
                .putInt("sortOrder", sortOrder)
                .apply()

            when (selectedOption) {
                0 -> sortBooksByTitle(sortOrder == 0)
                1 -> sortBooksByAuthor(sortOrder == 0)
                2 -> sortBooksByRating(sortOrder == 0)
                3 -> sortBooksByDate(sortOrder == 0)
            }

            bookAdapter.notifyDataSetChanged()
            true
        }
        popup.show()
    }

}