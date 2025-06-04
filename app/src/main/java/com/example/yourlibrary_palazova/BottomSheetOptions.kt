package com.example.yourlibrary_palazova

import com.example.yourlibrary_palazova.helpers.ImageUtils
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.FileOutputStream
import java.io.File


class BottomSheetOptions : BottomSheetDialogFragment() {

    private var imageUri: Uri? = null
    private var listener: AvatarUpdateListener? = null

    // launcher для запиту дозволу камери
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else Toast.makeText(context, "Дозвіл на камеру відхилено", Toast.LENGTH_SHORT).show()
        }

    // launcher для камери
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { saveImageFromUriToInternalStorage(it) }
        }
    }

    // launcher для галереи
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { saveImageFromUriToInternalStorage(it) }
    }

    interface AvatarUpdateListener {
        fun onAvatarDeleted()
    }

    // встановлення слухача для повідомлень про оновлення аватару
    fun setAvatarUpdateListener(listener: AvatarUpdateListener) {
        this.listener = listener
    }

    override fun getTheme(): Int = R.style.TransparentBottomSheetTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoAdd = view.findViewById<LinearLayout>(R.id.photoAddLayout)
        val editProfile = view.findViewById<LinearLayout>(R.id.profileEditLayout)
        val signOut = view.findViewById<TextView>(R.id.signOut)

        // обробник натискання кнопки "додаи фото"
        photoAdd.setOnClickListener {
            val options = arrayOf("Зробити фото", "Обрати фото", "Видалити фото")
            val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogStyle)
                .setTitle("Додати фото профілю")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndOpen()
                        1 -> openGallery()
                        2 -> deleteAvatarPhoto()
                    }
                }
                .create()
            dialog.show()

            val window = dialog.window
            window?.setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // обробник редагування профілю
        editProfile.setOnClickListener {
            Toast.makeText(context, "Редагуання профілю поки недоступне, чекайте згодом!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        // обробник виходу з акаунту
        signOut.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogStyle)
                .setTitle("Підтвердити вихід")
                .setMessage("Ви впевнені, що хочете вийти з облікового запису?")
                .setCancelable(false)
                .setPositiveButton("Так") { _, _ ->
                    FirebaseAuth.getInstance().signOut()

                    val intent = requireActivity().intent
                    requireActivity().finish()
                    startActivity(intent)

                    Toast.makeText(context, "Ви вийшли з аккаунту", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .setNegativeButton("Ні") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                setTextColor("#241203".toColorInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }
            val window = dialog.window
            window?.setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.85).toInt(),  // ширина 90% экрана
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("avatar", ".jpg", requireContext().cacheDir)
        imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        cameraLauncher.launch(imageUri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // збереження фото у внутрішне сховище
    private fun saveImageFromUriToInternalStorage(uri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val context = requireContext()

        // отримуємо bitmap з Uri
        val bitmap = ImageUtils.getCorrectlyOrientedBitmap(uri, context)

        if (bitmap == null) {
            Log.d("Bottom Sheet Options", "Не вдалося обробити зображення")
            Toast.makeText(context, "Не вдалося обробити зображення", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "avatar_${user.uid}.jpg" // ім'я файлу для аватару базується на uid користувача

        // збереження bitmap у внутрішнє сховище
        val savedPath = saveImageToInternalStorage(context, bitmap, filename)

        // збереження шляху до фото у Firestore
        saveAvatarPathToFirestore(user.uid, savedPath)

        Toast.makeText(context, "Аватар оновлено", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    // функція збереження bitmap у внутрішнє сховище
    private fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, filename: String): String {
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        return file.absolutePath
    }

    // Збереження шляху зображення у Firestore
    private fun saveAvatarPathToFirestore(userId: String, path: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val data = mapOf("photoUrl" to path)
        userRef.set(data, SetOptions.merge())
    }

    // перевірка дозволу на камеру
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA) // Запрашиваем разрешение напрямую
        }
    }

    // видалення аватарки
    private fun deleteAvatarPhoto() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val context = requireContext()

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        // отримуємо поточний шлях, щоб видалити локальний файл
        userRef.get().addOnSuccessListener { doc ->
            val photoPath = doc.getString("photoUrl")
            if (photoPath != null) {
                val file = File(photoPath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (!deleted) Log.d("Bottom Sheet Options", "Не вдалося видалити локальний файл")
                }
            }

            // очищаємо поле photoUrl у Firestore
            userRef.update("photoUrl", null)
                .addOnSuccessListener {
                    Toast.makeText(context, "Фото профілю видалено", Toast.LENGTH_SHORT).show()
                    listener?.onAvatarDeleted()
                    dismiss()
                }
                .addOnFailureListener {
                    Log.d("Bottom Sheet Options", "Помилка при видаленні фото з Firestore")
                }
        }.addOnFailureListener {
            Log.d("Bottom Sheet Options", "Не вдалося отримати дані користувача")
        }
    }

}