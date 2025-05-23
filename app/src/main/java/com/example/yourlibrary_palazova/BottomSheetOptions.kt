package com.example.yourlibrary_palazova

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class BottomSheetOptions : BottomSheetDialogFragment() {

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
            Toast.makeText(context, "Photo", Toast.LENGTH_SHORT).show()
            dismiss()
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
                setTextColor(Color.parseColor("#241203"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }
        }

    }
}