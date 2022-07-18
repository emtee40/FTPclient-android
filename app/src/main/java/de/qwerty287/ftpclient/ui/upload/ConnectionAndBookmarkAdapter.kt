package de.qwerty287.ftpclient.ui.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.Bookmark
import de.qwerty287.ftpclient.data.Connection

internal class ConnectionAndBookmarkAdapter(
    private val connections: List<Connection>,
    private val bookmarks: List<Bookmark>,
    private val navController: NavController,
    private val uri: String?,
    private val text: String?
) :
    RecyclerView.Adapter<ConnectionAndBookmarkAdapter.ViewHolder>() {

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_list, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= bookmarks.size) {
            holder.title.text = connections[position - bookmarks.size].title
            holder.title.setOnClickListener {
                val options = Bundle()
                options.putInt("connection", connections[position - bookmarks.size].id)
                options.putString("uri", uri)
                options.putString("text", text)
                try {
                    navController.navigate(R.id.action_UploadFileIntentFragment_to_FilesFragment, options)
                    UploadFileIntentFragment.exit = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            holder.title.text = bookmarks[position].title
            holder.title.setOnClickListener {
                val options = Bundle()
                options.putInt("connection", bookmarks[position].connection)
                options.putString("directory", bookmarks[position].directory)
                options.putString("uri", uri)
                options.putString("text", text)
                try {
                    navController.navigate(R.id.action_UploadFileIntentFragment_to_FilesFragment, options)
                    UploadFileIntentFragment.exit = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val icon = AppCompatResources.getDrawable(holder.title.context, R.drawable.ic_baseline_bookmark_24) ?: return
            val h = icon.intrinsicHeight
            val w = icon.intrinsicWidth
            icon.setBounds(0, 0, w, h)
            holder.title.setCompoundDrawablesRelative(icon, null, null, null)
        }
    }

    override fun getItemCount(): Int {
        return bookmarks.size + connections.size
    }
}