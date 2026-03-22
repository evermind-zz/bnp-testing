package org.schabi.newpipe.brave.feature.savesearchpresets.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.schabi.newpipe.R
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortDirection
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortType
import org.schabi.newpipe.brave.feature.savesearchpresets.ui.PresetListDialogFragment.Companion.MENU_GROUP_SORT_DIRECTION
import org.schabi.newpipe.brave.feature.savesearchpresets.ui.PresetListDialogFragment.Companion.MENU_GROUP_SORT_PRESETS
import org.schabi.newpipe.brave.feature.savesearchpresets.ui.PresetListDialogFragment.Companion.MENU_ID_SORT_DIRECTION
import org.schabi.newpipe.databinding.DialogPresetListBinding
import org.schabi.newpipe.fragments.list.search.SearchViewModel

/**
 * RecyclerView + Flow collector.
 */
class PresetListDialogFragment : DialogFragment() {

    companion object {
        val TAG: String = PresetListDialogFragment::class.java.simpleName
        const val MENU_GROUP_SORT_DIRECTION = 100
        const val MENU_ID_SORT_DIRECTION = 1
        const val MENU_GROUP_SORT_PRESETS = 101

        const val MENU_ID_SORT_NAME = 2
        const val MENU_ID_SORT_LAST_USED = 3
        const val MENU_ID_SORT_CREATED = 4
        const val MENU_ID_SORT_MODIFIED = 5
    }

    private val presetListViewModel by viewModels<PresetListViewModel> {
        PresetListViewModel.getFactory(searchViewModel)
    }

    private val searchViewModel by viewModels<SearchViewModel>(
        ownerProducer = { requireParentFragment() }
    )

    private val sortItems = listOf(
        SortMenuItem.Direction,
        SortMenuItem.SortOption(
            MENU_ID_SORT_NAME,
            R.string.sort_by_name,
            SortType.NAME
        ),
        SortMenuItem.SortOption(
            MENU_ID_SORT_LAST_USED,
            R.string.sort_by_last_used,
            SortType.LAST_USED
        ),
        SortMenuItem.SortOption(
            MENU_ID_SORT_CREATED,
            R.string.sort_by_created,
            SortType.CREATED
        ),
        SortMenuItem.SortOption(
            MENU_ID_SORT_MODIFIED,
            R.string.sort_by_modified,
            SortType.MODIFIED
        ),
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val binding = DialogPresetListBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        val presetAdapter = PresetAdapter(presetListViewModel) {
            dismiss()
        }

        binding.presetRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = presetAdapter
        }

        initToolbar(binding.toolbarLayout.toolbar)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                presetListViewModel.uiState.collect {
                    presetAdapter.submit(it.entries)
                }
            }
        }

        return dialog
    }

    private fun initToolbar(toolbar: Toolbar) {
        toolbar.setTitle(R.string.presets)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.setNavigationContentDescription(R.string.cancel)

        toolbar.inflateMenu(R.menu.menu_search_fragment)
        toolbar.menu.findItem(R.id.action_filter)?.setTitle(R.string.sort)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_filter) {
                showSortPopupMenu(toolbar)
            }
            true
        }
    }

    private fun showSortPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        val menu = popup.menu

        setupPopupMenu(menu)

        setupSelectedSortByIcons(menu)
        setupSortDirectionIcon(menu)
        setupClickListener(popup)

        forceShowPopupIcons(popup)
        popup.show()
    }

    private fun setupPopupMenu(menu: Menu) {
        sortItems.forEachIndexed { index, item ->
            menu.add(item.groupId, item.id, index, item.title)
        }
    }

    private fun setupClickListener(popup: PopupMenu) {
        popup.setOnMenuItemClickListener { item ->
            val selectedItem = sortItems.find { it.id == item.itemId }
                ?: return@setOnMenuItemClickListener false
            presetListViewModel.onSortSelected(selectedItem)
            true
        }
    }

    private fun setupSortDirectionIcon(menu: Menu) {
        val iconRes = when (presetListViewModel.uiState.value.direction) {
            SortDirection.ASC -> R.drawable.ic_arrow_drop_up
            SortDirection.DESC -> R.drawable.ic_arrow_drop_down
        }
        menu.findItem(MENU_ID_SORT_DIRECTION)?.setIcon(iconRes)
    }

    private fun setupSelectedSortByIcons(menu: Menu) {
        val sorting = presetListViewModel.uiState.value.sorting

        sortItems.forEach { item ->
            if (item.groupId == MENU_GROUP_SORT_PRESETS) {
                val icon = if ((item as SortMenuItem.SortOption).sortType == sorting) {
                    R.drawable.ic_done
                } else {
                    R.drawable.ic_empty
                }
                menu.findItem(item.id)?.setIcon(icon)
            }
        }
    }

    private fun forceShowPopupIcons(popup: PopupMenu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
            popup.setForceShowIcon(true)
        } else {
            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuPopupHelper = field.get(popup)
                val setForceIcons = menuPopupHelper.javaClass.getDeclaredMethod(
                    "setForceShowIcon",
                    Boolean::class.javaPrimitiveType
                )
                setForceIcons.invoke(menuPopupHelper, true)
            } catch (e: Exception) {
                Log.w(TAG, "Could not force show icons in PopupMenu", e)
            }
        }
    }
}

sealed class SortMenuItem(val id: Int, val title: Int, val groupId: Int) {

    data class SortOption(
        val menuId: Int,
        @StringRes
        val label: Int,
        val sortType: SortType
    ) : SortMenuItem(menuId, label, MENU_GROUP_SORT_PRESETS)

    object Direction : SortMenuItem(
        MENU_ID_SORT_DIRECTION,
        R.string.sort_order,
        MENU_GROUP_SORT_DIRECTION
    )
}