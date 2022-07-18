package de.qwerty287.ftpclient.ui.files

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.ui.files.providers.File


internal class FilesAdapter(
    private val files: List<File>,
    private val onClick: (File) -> Unit,
    private val onLongClick: (File) -> Unit
) :
    RecyclerView.Adapter<FilesAdapter.ViewHolder>() {


    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val file: TextView = view.findViewById(R.id.file)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_files_list, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.file.text = files[position].name
        holder.file.setOnClickListener {
            onClick(files[position])
        }
        holder.file.setOnLongClickListener {
            onLongClick(files[position])
            true
        }
        holder.file.setCompoundDrawablesRelative(getDrawableIcon(files[position], holder.file.context), null, null, null)
    }

    override fun getItemCount(): Int {
        return files.size
    }

    /**
     * Return the [Drawable] for use with [TextView.setCompoundDrawablesRelative] or related methods, matching to the file format of a [File]
     * @param file The [File]
     * @return The [Drawable] icon
     */
    private fun getDrawableIcon(file: File, context: Context): Drawable? {
        val icon = AppCompatResources.getDrawable(context, FileExtensions.getDrawableFromFTPFile(file)) ?: return null
        val h = icon.intrinsicHeight
        val w = icon.intrinsicWidth
        icon.setBounds(0, 0, w, h)
        return icon
    }
}