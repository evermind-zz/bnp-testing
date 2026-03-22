package org.schabi.newpipe.brave.feature.savesearchpresets.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortDirection
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortType

class EntryStore(private val db: EntryDatabase) {

    private val _state = MutableStateFlow(db.load())
    val state: StateFlow<DBState> = _state

    fun addEntry(
        name: String,
        service: String,
        contentFilterData: List<Int>,
        sortFilterData: List<Int>
    ) {

        if (name.isBlank()) return

        if (!isNameAvailable(service, name)) return

        val now = System.currentTimeMillis()

        val entry = Entry(
            name,
            service,
            now,
            now,
            now,
            contentFilterData,
            sortFilterData
        )

        val newState = _state.value.copy(
            entries = _state.value.entries + entry
        )

        update(newState)
    }

    /**
     * @return true if update succeeds
     */
    fun updateEntry(
        createdAt: Long,
        service: String,
        contentFilterData: List<Int>,
        sortFilterData: List<Int>
    ): Boolean {
        var previousEntryFound = false

        val updated = _state.value.entries.map {
            if (it.createdAt == createdAt && it.service == service) {
                previousEntryFound = true
                it.copy(
                    modifiedAt = System.currentTimeMillis(),
                    contentFilterData = contentFilterData,
                    sortFilterData = sortFilterData
                )
            } else it
        }

        if (!previousEntryFound) return false

        update(_state.value.copy(entries = updated))
        return true
    }

    fun deleteEntry(createdAt: Long) {

        val newEntries = _state.value.entries.filter {
            it.createdAt != createdAt
        }

        update(_state.value.copy(entries = newEntries))
    }

    fun renameEntry(createdAt: Long, newName: String) {
        val entry = _state.value.entries
            .firstOrNull { it.createdAt == createdAt }
            ?: return

        if (newName.isBlank()) return

        val duplicate = _state.value.entries.any {
            it.service == entry.service &&
                    it.name.equals(newName, true) &&
                    it.createdAt != createdAt
        }

        if (duplicate) return

        val updated = _state.value.entries.map {

            if (it.createdAt == createdAt) {
                it.copy(
                    name = newName,
                    modifiedAt = System.currentTimeMillis()
                )
            } else it
        }

        update(_state.value.copy(entries = updated))
    }

    fun updateLastUsed(createdAt: Long) {

        val updated = _state.value.entries.map {

            if (it.createdAt == createdAt) {
                it.copy(lastUsed = System.currentTimeMillis())
            } else it
        }

        update(_state.value.copy(entries = updated))
    }

    fun setDefault(service: String, createdAt: Long) {

        val filtered = _state.value.defaults.filter {
            it.service != service
        }

        val updated = filtered + DefaultEntry(service, createdAt)

        update(_state.value.copy(defaults = updated))
    }

    fun setSorting(key: SortType) {
        update(_state.value.copy(sorting = key))
    }

    fun setSortingDirection(key: SortDirection) {
        update(_state.value.copy(direction = key))
    }

    private fun update(newState: DBState) {

        _state.value = newState
        db.save(newState)
    }

    fun isNameAvailable(service: String, name: String): Boolean {
        return _state.value.entries.none {
            it.service == service &&
                    it.name.equals(name, true)
        }
    }
}