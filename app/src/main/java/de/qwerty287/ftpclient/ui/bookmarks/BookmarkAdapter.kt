package de.qwerty287.ftpclient.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.Bookmark

internal class BookmarkAdapter(
    private val bookmarks: List<Bookmark>,
    private val navController: NavController,
    private val fm: FragmentManager
) :
    RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmarks_list, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = bookmarks[position].title
        holder.title.setOnClickListener {
            val options = Bundle()
            options.putInt("connection", bookmarks[position].connection)
            options.putString("directory", bookmarks[position].directory)
            try {
                navController.navigate(R.id.action_BookmarksFragment_to_FilesFragment, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        holder.title.setOnLongClickListener {
            BookmarkActionsBottomSheet.newInstance(bookmarks[position].id).show(fm, "BookmarkActionsBottomSheet")
            true
        }
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }
}