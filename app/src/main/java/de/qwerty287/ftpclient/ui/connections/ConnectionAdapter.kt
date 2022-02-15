package de.qwerty287.ftpclient.ui.connections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.Connection

internal class ConnectionAdapter(
    private val connections: List<Connection>,
    private val navController: NavController,
    private val fm: FragmentManager
) :
    RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

    internal inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_list, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = connections[position].title
        holder.title.setOnClickListener {
            val options = Bundle()
            options.putInt("connection", connections[position].id)
            try {
                navController.navigate(R.id.action_ConnectionsFragment_to_FilesFragment, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        holder.title.setOnLongClickListener {
            ConnectionActionsBottomSheet.newInstance(connections[position].id).show(fm, "ConnectionActionsBottomSheet")
            true
        }
    }

    override fun getItemCount(): Int {
        return connections.size
    }
}