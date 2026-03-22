package org.schabi.newpipe.brave.feature.savesearchpresets.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.schabi.newpipe.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresetAdapter(
    private val viewModel: PresetListViewModel,
    private val onPresetSelected: () -> Unit
) : RecyclerView.Adapter<PresetAdapter.VH>() {

    private var items: List<PresetUiEntry> = emptyList()

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm.ss", Locale.getDefault())

    init {
        setHasStableIds(true)
    }

    fun submit(newList: List<PresetUiEntry>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos].entry.createdAt ==
                        newList[newPos].entry.createdAt
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos] == newList[newPos]
            }
        })

        items = newList
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_preset_item, parent, false)
        return VH(view)
    }

    override fun getItemId(position: Int): Long {
        return items[position].entry.createdAt
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val entry = item.entry

        val prefix = buildString {
            if (item.isActive) append("● ")
            if (item.isDefault) append("★ ")
        }

        holder.name.text = "${prefix}${entry.name}"
        holder.subtitle.text = holder.itemView.context.getString(
            R.string.last_used,
            dateFormat.format(Date(entry.lastUsed))
        )

        holder.itemView.setOnClickListener {
            viewModel.selectPreset(entry)
            onPresetSelected()
        }

        holder.itemView.setOnLongClickListener {
            showMenu(it, item)
            true
        }
    }

    private fun showMenu(anchor: View, item: PresetUiEntry) {
        val popup = PopupMenu(anchor.context, anchor)

        val MENU_GROUP_PRESETS = 0
        val MENU_ID_RENAME = 0
        val MENU_ID_DELETE = 1
        val MENU_ID_SET_DEFAULT = 2

        popup.menu.apply {
            add(
                MENU_GROUP_PRESETS,
                MENU_ID_RENAME,
                0,
                R.string.rename
            )
            add(
                MENU_GROUP_PRESETS,
                MENU_ID_DELETE,
                0,
                R.string.delete
            )

            if (!item.isDefault) {
                add(
                    MENU_GROUP_PRESETS,
                    MENU_ID_SET_DEFAULT,
                    0,
                    R.string.set_as_default
                )
            }
        }

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                MENU_ID_RENAME -> showRenameDialog(anchor, item)
                MENU_ID_DELETE -> viewModel.deletePreset(item.entry)
                MENU_ID_SET_DEFAULT -> viewModel.setDefault(item.entry)
            }
            true
        }

        popup.show()
    }

    private fun showRenameDialog(anchor: View, item: PresetUiEntry) {
        val context = anchor.context
        val edit = android.widget.EditText(context)
        edit.setText(item.entry.name)

        AlertDialog.Builder(context)
            .setTitle(R.string.rename_preset)
            .setView(edit)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newName = edit.text.toString()
                viewModel.renamePreset(item.entry, newName)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.presetName)
        val subtitle: TextView = view.findViewById(R.id.presetSubtitle)
    }
}
