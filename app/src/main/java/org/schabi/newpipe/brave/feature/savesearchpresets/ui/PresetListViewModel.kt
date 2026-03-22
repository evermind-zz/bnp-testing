package org.schabi.newpipe.brave.feature.savesearchpresets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.schabi.newpipe.brave.feature.savesearchpresets.data.DBState
import org.schabi.newpipe.brave.feature.savesearchpresets.data.Entry
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortDirection
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortType
import org.schabi.newpipe.fragments.list.search.SearchViewModel
import java.util.Locale

/**
 * This ViewModel aggregates the flows:
 *
 * EntryStore.state
 * SearchViewModel.activePreset
 */
class PresetListViewModel(
    private val searchViewModel: SearchViewModel
) : ViewModel() {

    val entryStore = searchViewModel.entryStore
    val service: String = searchViewModel.getService().serviceInfo.name
    val uiState: StateFlow<PresetUiState> =
        combine(
            entryStore.state,
            searchViewModel.activePreset
        ) { dbState, activePreset ->
            val dbStateWrapper = DbStateWrapper(dbState)
            val defaultId = dbState.defaults
                .find { it.service == service }
                ?.entry

            val entriesForService = dbState.entries
                .filter { it.service == service }

            val uiEntries = entriesForService.map { entry ->

                PresetUiEntry(
                    entry = entry,
                    isDefault = entry.createdAt == defaultId,
                    isActive = entry.createdAt == activePreset?.createdAt
                )
            }

            val sorted = sortEntries(uiEntries, dbStateWrapper.sorting, dbStateWrapper.direction)

            PresetUiState(
                entries = sorted,
                sorting = dbStateWrapper.sorting,
                direction = dbStateWrapper.direction
            )

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PresetUiState(emptyList(), SortType.NAME, SortDirection.DESC)
        )

    class DbStateWrapper(dbState: DBState) {
        val direction = dbState.direction
        val sorting = dbState.sorting
    }

    fun selectPreset(entry: Entry) {
        searchViewModel.setActivePreset(entry)
        entryStore.updateLastUsed(entry.createdAt)
    }

    fun deletePreset(entry: Entry) {
        entryStore.deleteEntry(entry.createdAt)
    }

    fun renamePreset(entry: Entry, newName: String) {
        entryStore.renameEntry(entry.createdAt, newName)
    }

    fun setDefault(entry: Entry) {
        entryStore.setDefault(service, entry.createdAt)
    }

    fun onSortSelected(item: SortMenuItem) {
        when (item) {
            is SortMenuItem.SortOption -> {
                entryStore.setSorting(item.sortType)
            }

            SortMenuItem.Direction -> {
                entryStore.setSortingDirection(toggleDirection())
            }
        }
    }

    private fun toggleDirection(): SortDirection =
        if (uiState.value.direction == SortDirection.ASC) {
            SortDirection.DESC
        } else {
            SortDirection.ASC
        }

    private fun sortEntries(
        list: List<PresetUiEntry>,
        sorting: SortType,
        direction: SortDirection
    ): List<PresetUiEntry> {
        val comparator = when (sorting) {
            SortType.LAST_USED ->
                compareBy<PresetUiEntry> { it.entry.lastUsed }

            SortType.CREATED ->
                compareBy { it.entry.createdAt }

            SortType.MODIFIED ->
                compareBy { it.entry.modifiedAt }

            SortType.NAME ->
                compareBy { it.entry.name.lowercase(Locale.getDefault()) }
        }

        val sorted = list.sortedWith(comparator)

        return if (direction == SortDirection.DESC) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    companion object {

        fun getFactory(
            searchViewModel: SearchViewModel
        ) = viewModelFactory {
            initializer {
                PresetListViewModel(
                    searchViewModel
                )
            }
        }
    }
}

// This is the UI model layer so business logic is more hidden.
data class PresetUiEntry(
    val entry: Entry,
    val isDefault: Boolean,
    val isActive: Boolean
)

data class PresetUiState(
    val entries: List<PresetUiEntry>,
    val sorting: SortType,
    val direction: SortDirection
)