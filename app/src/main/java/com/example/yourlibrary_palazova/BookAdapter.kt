package com.example.yourlibrary_palazova


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class BookAdapter(
    private val books: MutableList<Book>,
    private val headerClickListener: HeaderClickListener,
    private val bookClickListener: OnBookClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface HeaderClickListener {
        fun onFilterClicked()
        fun onSortClicked()
    }

    interface OnBookClickListener {
        fun onBookClick(book: Book)
    }


    private var activeFiltersList: List<String> = emptyList()


    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.bookName)
        val dateText: TextView = itemView.findViewById(R.id.bookData)
        val ratingText: TextView = itemView.findViewById(R.id.bookRating)
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val filterButton: ImageButton = itemView.findViewById(R.id.buttonFilter)
        private val sortButton: ImageButton = itemView.findViewById(R.id.buttonSort)
        private val activeFiltersLayout: LinearLayout = itemView.findViewById(R.id.activeFiltersLayout)


        init {
            if (!isUserLoggedIn()) {
                filterButton.visibility = View.GONE
                sortButton.visibility = View.GONE
            } else {
                filterButton.setOnClickListener {
                    headerClickListener.onFilterClicked()
                }

                sortButton.setOnClickListener {
                    headerClickListener.onSortClicked()
                }
            }
        }

        fun bindFilters(filters: List<String>) {
            activeFiltersLayout.removeAllViews()

            if (!isUserLoggedIn()) {
                activeFiltersLayout.visibility = View.GONE
                return
            }

            if (filters.isEmpty()) {
                activeFiltersLayout.visibility = View.GONE
            } else {
                activeFiltersLayout.visibility = View.VISIBLE
                val context = itemView.context

                filters.forEach { filterText ->
                    val filterView = TextView(context).apply {
                        text = filterText
                        setTextColor(Color.parseColor("#F1EDE3"))
                        setPadding(20, 10, 20, 10)
                        // Копия, чтоб не менять цвет других окон
                        val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.rounded_background)?.mutate()
                        backgroundDrawable?.setTint(Color.parseColor("#95557E5A"))
                        background = backgroundDrawable
                        textSize = 16f
                        typeface = ResourcesCompat.getFont(context, R.font.oswald_medium)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 10, 0)
                        layoutParams = params
                    }
                    activeFiltersLayout.addView(filterView)
                }
            }
        }

    }

    override fun getItemCount(): Int = books.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.
                    item_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item, parent, false)
                BookViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0 && holder is HeaderViewHolder) {
            holder.bindFilters(activeFiltersList)
            // кнопки и другие элементы, если нужно, тоже можно обновить
        } else if (holder is BookViewHolder) {
            val book = books[position - 1]
            holder.titleText.text = book.title

            val dateText = if (!book.endDate.isNullOrEmpty()) {
                "${book.startDate} – ${book.endDate}"
            } else {
                book.startDate ?: ""
            }

            holder.dateText.text = dateText
            holder.ratingText.text = "${book.rating}/10"

            // натискання на елемент списку
            holder.itemView.setOnClickListener {
                bookClickListener.onBookClick(book)
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun updateActiveFilters(filters: List<String>) {
        activeFiltersList = filters
        notifyItemChanged(0)
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

}
