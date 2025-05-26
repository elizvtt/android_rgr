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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import me.zhanghai.android.materialratingbar.MaterialRatingBar
import androidx.core.view.isGone
import java.io.File
import androidx.core.net.toUri
import com.makeramen.roundedimageview.RoundedImageView

class ActivityBookPage : AppCompatActivity() {

    private lateinit var bookId: String
    private lateinit var currentBook: Book
    private lateinit var editBookLauncher: ActivityResultLauncher<Intent>

    private var coverUri: Uri? = null

    private var toast: Toast? = null

    // launcher для запроса разрешения камеры
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Дозвіл на камеру відхилено", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher для камеры
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverUri?.let { saveImageFromUriToInternalStorage(it) }
        }
    }

    // Launcher для галереи
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverUri = uri // обязательно сохраняем uri выбранной картинки
            saveImageFromUriToInternalStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_page)

        bookId = intent.getStringExtra("BOOK_ID") ?: ""

        val viewModel = ViewModelProvider(this)[BooksViewModel::class.java]
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

        buttonBack.setOnClickListener {
            finish()
        }

        buttonFav.setOnClickListener {
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

        buttonEdit.setOnClickListener {

            val themedContext = ContextThemeWrapper(this, R.style.PopupMenuStyle)
            val popupMenu = PopupMenu(themedContext, buttonEdit)

            popupMenu.menuInflater.inflate(R.menu.menu_edit, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_input -> {
                        val intent = Intent(this, ActivityAddBook::class.java).apply {
                            putExtra("action", "edit book")
                            putExtra("bookId", bookId)
                            putExtra("title", currentBook.title)
                            putExtra("author", currentBook.author)
                            putExtra("startDate", currentBook.startDate)
                            putExtra("endDate", currentBook.endDate)
                            putExtra("rating", currentBook.rating)
                            putExtra("favorites", currentBook.favorites)
                            putStringArrayListExtra("notes", ArrayList(currentBook.notes ?: listOf()))
                            putStringArrayListExtra("quotes", ArrayList(currentBook.quotes ?: listOf()))
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
                // Книга оновлена — онови дані
                viewModel.loadBookDetails(bookId)
            }
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

        val buttonFav = findViewById<ImageButton>(R.id.buttonFavorite)
        val isFavorite = book.favorites
        buttonFav.tag = isFavorite
        updateFavoriteIcon(buttonFav, isFavorite)

        val imageView = findViewById<RoundedImageView>(R.id.bookCover)
        val uri = book.coverUri?.toUri()
        if (uri != null) {
            val bitmap = fixImageOrientationFromUri(this, uri)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.rounded_background)
            }
        } else {
            imageView.setImageResource(R.drawable.rounded_background)
        }
    }

    private fun updateFavoriteIcon(button: ImageButton, isFavorite: Boolean) {
        button.setImageResource(
            if (isFavorite) R.drawable.icon_heart_filled
            else R.drawable.icon_heart_unfilled
        )
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
            if (quotesContainer.isGone) quotesContainer.visibility = View.VISIBLE else quotesContainer.visibility = View.GONE
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
            if (notesContainer.isGone) {
                notesContainer.visibility = View.VISIBLE
            } else {
                notesContainer.visibility = View.GONE
            }
        }
    }

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

    private fun openCamera() {
        val photoFile = File.createTempFile("cover", ".jpg", this.cacheDir)
        val uri = FileProvider.getUriForFile(this, "${this.packageName}.provider", photoFile)
        coverUri = uri
        cameraLauncher.launch(uri)
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openGallery() {
        // MIME тип изображения
        galleryLauncher.launch("image/*")
    }

    private fun saveImageFromUriToInternalStorage(uri: Uri) {
        val viewModel = ViewModelProvider(this)[BooksViewModel::class.java]
        // Здесь можно сделать копирование изображения в внутреннее хранилище (если нужно)
        // И вызвать updateCover

        viewModel.updateCover(bookId, uri,
            onSuccess = {
                Toast.makeText(this, "Обкладинка оновлена", Toast.LENGTH_SHORT).show()
                viewModel.loadBookDetails(bookId) // обновить UI
            },
            onFailure = {
                Log.d("Activity Book Page", "Не вдалося оновити обкладинку")
            }
        )
    }

    private fun openInternet() {
        // TODO: Реализовать выбор обложки из интернета
        Toast.makeText(this, "Функція вибору обкладинки з Інтернету поки не реалізована", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCover() {
        val viewModel = ViewModelProvider(this)[BooksViewModel::class.java]
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

    fun fixImageOrientationFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // Получаем EXIF-информацию
            val exifStream = context.contentResolver.openInputStream(uri)
            val exif = exifStream?.use { ExifInterface(it) }

            // Получаем ориентацию
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            // Повторно открываем поток для изображения (т.к. первый уже был использован)
            val imageStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = imageStream?.use { BitmapFactory.decodeStream(it) }

            if (originalBitmap == null) return null

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
