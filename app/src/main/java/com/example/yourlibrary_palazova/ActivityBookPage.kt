package com.example.yourlibrary_palazova

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.isGone
import com.example.yourlibrary_palazova.helpers.ImageUtils
import me.zhanghai.android.materialratingbar.MaterialRatingBar
import com.makeramen.roundedimageview.RoundedImageView
import java.io.File
import java.io.FileOutputStream

class ActivityBookPage : AppCompatActivity() {

    private lateinit var viewModel: BooksViewModel

    private lateinit var bookId: String
    private lateinit var currentBook: Book
    private lateinit var editBookLauncher: ActivityResultLauncher<Intent>

    private var coverUri: Uri? = null

    private var toast: Toast? = null

    // launcher для запиту дозволу на використання камери
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera() // якщо дозвіл отримано відкриваємо камеру
            } else {
                Toast.makeText(this, "Дозвіл на камеру відхилено", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher для фотографування
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverUri?.let { uri -> saveBookCoverFromUriToInternalStorage(uri, bookId) }
        }
    }

    // Launcher для вибору зображення з галереї
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            coverUri = selectedUri
            saveBookCoverFromUriToInternalStorage(selectedUri, bookId) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_page)

        // отримуємо ID книги з інтенту
        bookId = intent.getStringExtra("BOOK_ID") ?: ""

        viewModel = ViewModelProvider(this)[BooksViewModel::class.java]
        val buttonEdit = findViewById<ImageButton>(R.id.imageButtonEdit)
        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val buttonFav = findViewById<ImageButton>(R.id.buttonFavorite)

        if (bookId.isEmpty()) {
            Log.d("Activity Book Page", "Book ID не задан")
            finish()
            return
        }

        viewModel.selectedBook.observe(this) { book ->
            if (book != null) {
                currentBook = book
                bindBookDetails(book)
            } else {
                Log.d("Activity Book Page", "Книга не знайдена")
                finish()
            }
        }

        viewModel.loadBookDetails(bookId)

        // кнопка назад
        buttonBack.setOnClickListener {
            finish()
        }

        // додавання до "обраного"
        buttonFav.setOnClickListener {
            // анімація натискання
            val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                buttonFav,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f)
            ).apply {
                duration = 150
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = 1
            }

            scaleUp.start()

            val currentIsFavorite = buttonFav.tag as? Boolean == true
            val newFavoriteStatus = !currentIsFavorite

            viewModel.updateFavoriteStatus(
                bookId = bookId,
                isFavorite = newFavoriteStatus,
                onSuccess = {
                    buttonFav.tag = newFavoriteStatus
                    updateFavoriteIcon(buttonFav, newFavoriteStatus)
                    toast?.cancel()
                    toast = Toast.makeText(this,
                        if (newFavoriteStatus) "Додано до «Обраного»"
                        else "Видалено з «Обраного»", Toast.LENGTH_SHORT)
                    toast?.show()
                },
                onFailure = {
                    Log.d("Activity Book Page", "Не вдалося оновити поле favorite: ${it.message}")
                }
            )
        }

        // кнопка редагувати
        buttonEdit.setOnClickListener {

            val themedContext = ContextThemeWrapper(this, R.style.PopupMenuStyle)
            val popupMenu = PopupMenu(themedContext, buttonEdit)

            popupMenu.menuInflater.inflate(R.menu.menu_edit, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_input -> {
                        val intent = Intent(this, ActivityAddBook::class.java).apply {
                            // передаємо action edit book
                            putExtra("action", "edit book")
                            putExtra("bookId", bookId)
                            putExtra("title", currentBook.title)
                            putExtra("author", currentBook.author)
                            putExtra("startDate", currentBook.startDate)
                            putExtra("endDate", currentBook.endDate)
                            putExtra("rating", currentBook.rating)
                            putExtra("favorites", currentBook.favorites)
                            putStringArrayListExtra("notes", ArrayList(currentBook.notes))
                            putStringArrayListExtra("quotes", ArrayList(currentBook.quotes))
                            putExtra("coverUri", currentBook.coverUri)
                        }

                        editBookLauncher.launch(intent)
                        true
                    }
                    R.id.add_cover -> {
                        showCoverDialog()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
            true
        }

        editBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loadBookDetails(bookId) // оновлюємо дані
            }
        }
    }

    private fun bindBookDetails(book: Book) {
        currentBook = book
        findViewById<TextView>(R.id.bookTitle).text = book.title
        findViewById<TextView>(R.id.bookAuthor).text = book.author
        findViewById<TextView>(R.id.startDate).text = "Почато: ${book.startDate}"
        findViewById<TextView>(R.id.endDate).text = "Прочитано: ${book.endDate ?: "-"}"
        setupRatingBar(book.rating)
        setupQuotes(book.quotes)
        setupNotes(book.notes)

        val buttonFav = findViewById<ImageButton>(R.id.buttonFavorite)
        val isFavorite = book.favorites
        buttonFav.tag = isFavorite
        updateFavoriteIcon(buttonFav, isFavorite)

        val path = book.coverUri
        val imageView = findViewById<RoundedImageView>(R.id.bookCover)
        if (!path.isNullOrEmpty()) {
            val file = File(path)

            if (file.exists()) {
                val bitmap = fixImageOrientationFromFile(file)  // або просто decodeFile + rotate, як у вашій утиліті
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else imageView.setImageResource(R.drawable.rounded_background)
            } else imageView.setImageResource(R.drawable.rounded_background)
        } else imageView.setImageResource(R.drawable.rounded_background)

    }

    // оновлення іконки "обраного"
    private fun updateFavoriteIcon(button: ImageButton, isFavorite: Boolean) {
        button.setImageResource(
            if (isFavorite) R.drawable.icon_heart_filled
            else R.drawable.icon_heart_unfilled
        )
    }

    // налаштування відображення RatingBar-у
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
        if (quotes.isNullOrEmpty()) { // Якщо список цитат пустий ховаємо quotesBlock
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

        // Кнопка розгортання/згортання
        findViewById<ImageButton>(R.id.buttonToggleQuotes).setOnClickListener {
            if (quotesContainer.isGone) quotesContainer.visibility = View.VISIBLE
            else quotesContainer.visibility = View.GONE
        }
    }

    private fun setupNotes(notes: List<String>?) {
        val notesContainer = findViewById<LinearLayout>(R.id.notesContainer)
        val notesBlock = findViewById<LinearLayout>(R.id.notesBlock)
        notesContainer.removeAllViews()
        if (notes.isNullOrEmpty()) { // якщо нотатки пусті  ховаємо notesBlock
            notesBlock.visibility = View.GONE
        } else {
            notesBlock.visibility = View.VISIBLE
            notes.forEach { note ->
                val textView = TextView(this)
                textView.text = note
                textView.setPadding(8, 4, 8, 4)
                notesContainer.addView(textView)
            }
        }

        findViewById<ImageButton>(R.id.buttonToggleNotes).setOnClickListener {
            if (notesContainer.isGone) notesContainer.visibility = View.VISIBLE
            else notesContainer.visibility = View.GONE
        }
    }

    // діалогове вікно при додаванні обкладинки
    private fun showCoverDialog() {
        val options = arrayOf("Зробити фото", "Обрати з галереї", "Обрати з Інтернету", "Видалити обкладинку")
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Додати обкладинку")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                    2 -> openInternet()
                    3 -> deleteCover()
                }
            }
            .create()

        dialog.show()

        val window = dialog.window
        window?.setLayout(
            (this.resources.displayMetrics.widthPixels * 0.8).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun openInternet() {
        Toast.makeText(this, "Функція вибору обкладинки з Інтернету поки не реалізована", Toast.LENGTH_SHORT).show()
    }

    // відкриття камери
    private fun openCamera() {
        val photoFile = File.createTempFile("cover", ".jpg", this.cacheDir)
        val uri = FileProvider.getUriForFile(this, "${this.packageName}.provider", photoFile)
        coverUri = uri
        cameraLauncher.launch(uri)
    }

    // перевірка дозволу на камеру
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openGallery() { galleryLauncher.launch("image/*") }

    private fun saveBookCoverFromUriToInternalStorage(uri: Uri, bookId: String) {
        val context = this

        // отримуємо bitmap з Uri
        val bitmap = ImageUtils.getCorrectlyOrientedBitmap(uri, context)
        if (bitmap == null) {
            Log.d("Activity Book Page", "Не вдалося обробити зображення")
            Toast.makeText(context, "Не вдалося обробити зображення", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "cover_${bookId}.jpg" // ім'я файлу для обкладинки

        // збереження bitmap у внутрішнє сховище
        val savedPath = saveImageToInternalStorage(context, bitmap, filename)

        // збереження шляху до обкладинки у Firestore
        saveCoverPathToFirestore(bookId, savedPath)

        Toast.makeText(context, "Обкладинку оновлено", Toast.LENGTH_SHORT).show()
    }

    private fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, filename: String): String {
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        return file.absolutePath
    }

    private fun saveCoverPathToFirestore(bookId: String, path: String) {
        viewModel.saveCoverPath(
            bookId = bookId,
            path = path,
            onSuccess = {
                Toast.makeText(this, "Обкладинку оновлено", Toast.LENGTH_SHORT).show()
                viewModel.loadBookDetails(bookId)
            },
            onFailure = { error ->
                Log.e("ActivityBookPage", "Не вдалося зберегти обкладинку: ${error.message}")
                Toast.makeText(this, "Помилка збереження обкладинки", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun deleteCover() {
        viewModel.updateCover(bookId, null,
            onSuccess = {
                Toast.makeText(this, "Обкладинку видалено", Toast.LENGTH_SHORT).show()
                viewModel.loadBookDetails(bookId)
            },
            onFailure = {
                Log.d("Activity Book Page", "Не вдалося видалити обкладинку: ${it.message}")
            }
        )
    }

    fun fixImageOrientationFromFile(file: File): Bitmap? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.width, originalBitmap.height,
                matrix, true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
