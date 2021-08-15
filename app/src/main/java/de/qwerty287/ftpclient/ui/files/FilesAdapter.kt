package de.qwerty287.ftpclient.ui.files

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.qwerty287.ftpclient.R
import org.apache.commons.net.ftp.FTPFile

internal class FilesAdapter(
    private val context: Context,
    private val files: Array<FTPFile>,
    private val onClick: (FTPFile) -> Unit,
    private val onLongClick: (FTPFile) -> Unit
) :
    RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val file: TextView = view.findViewById(R.id.file)
        val directory: TextView = view.findViewById(R.id.directory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_files_list, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (files[position].isFile) {
            holder.directory.isVisible = false
            holder.file.text = files[position].name
            holder.file.setOnClickListener { 
                onClick(files[position])
            }
            holder.file.setOnLongClickListener {
                onLongClick(files[position])
                true
            }
        } else if (files[position].isDirectory) {
            holder.file.isVisible = false
            holder.directory.text = files[position].name
            holder.directory.setOnClickListener { 
                onClick(files[position])
            }
            holder.directory.setOnLongClickListener {
                onLongClick(files[position])
                true
            }
        } else {
            holder.directory.isVisible = false
            holder.file.text = context.getString(R.string.error_occurred)
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}