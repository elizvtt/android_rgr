package com.example.yourlibrary_palazova

import com.example.yourlibrary_palazova.helpers.ImageUtils
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import androidx.core.graphics.toColorInt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.FileOutputStream


class BottomSheetOptions : BottomSheetDialogFragment() {

    private var imageUri: Uri? = null

    // launcher для запроса разрешения камеры
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(context, "Дозвіл на камеру відхилено", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher для камеры
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { saveImageFromUriToInternalStorage(it) }
        }
    }

    // Launcher для галереи
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { saveImageFromUriToInternalStorage(it) }
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

        view.findViewById<LinearLayout>(R.id.photoAddLayout).setOnClickListener {
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
                (requireContext().resources.displayMetrics.widthPixels * 0.8).toInt(),  // ширина 90% экрана
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        view.findViewById<LinearLayout>(R.id.profileEditLayout).setOnClickListener {
            Toast.makeText(context, "Edit", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        view.findViewById<TextView>(R.id.signOut).setOnClickListener {
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
        // MIME тип изображения
        galleryLauncher.launch("image/*")
    }

    private fun saveImageFromUriToInternalStorage(uri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val context = requireContext()

        // Получаем bitmap из Uri
        val bitmap = ImageUtils.getCorrectlyOrientedBitmap(uri, context)

        if (bitmap == null) {
            Toast.makeText(context, "Не вдалося обробити зображення", Toast.LENGTH_SHORT).show()
            return
        }

        // Имя файла, можно задать по UID пользователя
        val filename = "avatar_${user.uid}.jpg"

        // Сохраняем bitmap в Internal Storage
        val savedPath = saveImageToInternalStorage(context, bitmap, filename)

        // Сохраняем путь к файлу в Firestore
        saveAvatarPathToFirestore(user.uid, savedPath)

        Toast.makeText(context, "Аватар оновлено", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    // Функция сохранения bitmap в Internal Storage
    private fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, filename: String): String {
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        return file.absolutePath
    }

    // Сохраняем путь в Firestore в коллекцию "users"
    private fun saveAvatarPathToFirestore(userId: String, path: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        val data = mapOf("photoUrl" to path)
        userRef.set(data, SetOptions.merge())
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Можно показать объяснение почему нужно разрешение
                Toast.makeText(context, "Для зйомки фото потрібен дозвіл на камеру", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Запрашиваем разрешение напрямую
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun deleteAvatarPhoto() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val context = requireContext()

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        // Сначала получаем текущий путь, чтобы удалить файл локально
        userRef.get().addOnSuccessListener { doc ->
            val photoPath = doc.getString("photoUrl")
            if (photoPath != null) {
                val file = File(photoPath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (!deleted) {
                        Toast.makeText(context, "Не вдалося видалити локальний файл", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Обновляем Firestore, очищая поле photoUrl
            userRef.update("photoUrl", null)
                .addOnSuccessListener {
                    Toast.makeText(context, "Фото профілю видалено", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Помилка при видаленні фото з Firestore", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(context, "Не вдалося отримати дані користувача", Toast.LENGTH_SHORT).show()
        }
    }



}