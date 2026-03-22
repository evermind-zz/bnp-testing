package org.schabi.newpipe.fragments.list.search

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.brave.feature.savesearchpresets.data.Entry
import org.schabi.newpipe.brave.feature.savesearchpresets.data.EntryDatabase
import org.schabi.newpipe.brave.feature.savesearchpresets.data.EntryStore
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.filter.FilterItem
import org.schabi.newpipe.fragments.list.search.filter.InjectFilterItem
import org.schabi.newpipe.fragments.list.search.filter.SearchFilterLogic
import org.schabi.newpipe.fragments.list.search.filter.SearchFilterLogic.Factory.Variant
import org.schabi.newpipe.util.ServiceHelper

/**
 * This class hosts the search filters logic. It facilitates
 * the communication with the SearchFragment* and the *DialogFragment
 * based search filter UI's
 */
class SearchViewModel(
    val serviceId: Int,
    logicVariant: Variant,
    userSelectedContentFilterList: List<Int>,
    userSelectedSortFilterList: List<Int>,
    val entryStore: EntryStore
) : ViewModel() {

    // begin -- preset database access stuff -- variables
    private val _activePreset = MutableStateFlow<Entry?>(null)
    val activePreset: StateFlow<Entry?> = _activePreset
    private var lastPresetId: Long = NO_LAST_PRESET_ENTRY
    // end -- preset database access stuff -- variables

    private val selectedContentFilterMutableLiveData: MutableLiveData<MutableList<FilterItem>> =
        MutableLiveData()
    private var selectedSortFilterLiveData: MutableLiveData<MutableList<FilterItem>> =
        MutableLiveData()
    private var userSelectedSortFilterListMutableLiveData: MutableLiveData<ArrayList<Int>> =
        MutableLiveData()
    private var userSelectedContentFilterListMutableLiveData: MutableLiveData<ArrayList<Int>> =
        MutableLiveData()
    private var doSearchMutableLiveData: MutableLiveData<Boolean> = MutableLiveData()

    val selectedContentFilterItemListLiveData: LiveData<MutableList<FilterItem>>
        get() = selectedContentFilterMutableLiveData
    val selectedSortFilterItemListLiveData: LiveData<MutableList<FilterItem>>
        get() = selectedSortFilterLiveData
    val userSelectedContentFilterListLiveData: LiveData<ArrayList<Int>>
        get() = userSelectedContentFilterListMutableLiveData
    val userSelectedSortFilterListLiveData: LiveData<ArrayList<Int>>
        get() = userSelectedSortFilterListMutableLiveData
    val doSearchLiveData: LiveData<Boolean>
        get() = doSearchMutableLiveData

    var searchFilterLogic: SearchFilterLogic

    init {
        // inject before creating SearchFilterLogic
        InjectFilterItem.DividerBetweenYoutubeAndYoutubeMusic.run()

        searchFilterLogic = SearchFilterLogic.Factory.create(
            logicVariant,
            getService().searchQHFactory,
            null
        )
        searchFilterLogic.restorePreviouslySelectedFilters(
            userSelectedContentFilterList,
            userSelectedSortFilterList
        )

        searchFilterLogic.setCallback { userSelectedContentFilter: List<FilterItem?>,
                                        userSelectedSortFilter: List<FilterItem?>
            ->
            selectedContentFilterMutableLiveData.value =
                userSelectedContentFilter as MutableList<FilterItem>
            selectedSortFilterLiveData.value =
                userSelectedSortFilter as MutableList<FilterItem>
            userSelectedContentFilterListMutableLiveData.value =
                searchFilterLogic.selectedContentFilters
            userSelectedSortFilterListMutableLiveData.value =
                searchFilterLogic.selectedSortFilters

            doSearchMutableLiveData.value = true
        }

        restoreFiltersFromActivePreset(userSelectedContentFilterList, userSelectedSortFilterList)
        activateDefaultPresetIfNoneSelectedYet()
    }

    fun weConsumedDoSearchLiveData() {
        doSearchMutableLiveData.value = false
    }

    fun getService(): StreamingService {
        return NewPipe.getService(serviceId)
    }

    fun getFilterTitle(item: FilterItem, context: Context): String {
        return ServiceHelper.getTranslatedFilterString(item.nameId, context)
    }

    companion object {
        private val TAG: String = SearchViewModel::class.java.simpleName
        private const val NO_LAST_PRESET_ENTRY = -1L

        fun getFactory(
            context: Context,
            serviceId: Int,
            logicVariant: Variant,
            userSelectedContentFilterList: ArrayList<Int>,
            userSelectedSortFilterList: ArrayList<Int>
        ) = viewModelFactory {
            initializer {
                SearchViewModel(
                    serviceId,
                    logicVariant,
                    userSelectedContentFilterList,
                    userSelectedSortFilterList,
                    EntryStore(EntryDatabase(context))
                )
            }
        }
    }

    // begin -- preset database access stuff

    fun setActivePreset(entry: Entry) {
        _activePreset.value = entry
    }

    private fun activateDefaultPresetIfNoneSelectedYet() {
        viewModelScope.launch {
            val service = getService().serviceInfo.name
            entryStore.state.collect { db ->
                val default = db.defaults
                    .firstOrNull { it.service == service }
                val entry = db.entries
                    .firstOrNull { it.createdAt == default?.entry }
                if (_activePreset.value == null) {
                    _activePreset.value = entry
                }
            }
        }
    }

    private fun restoreFiltersFromActivePreset(
        userSelectedContentFilterList: List<Int>,
        userSelectedSortFilterList: List<Int>
    ) {
        viewModelScope.launch {
            activePreset.collect { entry ->
                Log.d(TAG, "userSelectedContentFilterList ${userSelectedContentFilterList.size}")
                Log.d(TAG, "userSelectedSortFilterList ${userSelectedSortFilterList.size}")
                if (entry != null) {
                    searchFilterLogic.restorePreviouslySelectedFilters(
                        entry.contentFilterData,
                        entry.sortFilterData,
                    )
                    lastPresetId = entry.createdAt
                } else {
                    lastPresetId = NO_LAST_PRESET_ENTRY
                    Log.d(TAG, "entry is null")
                }
            }
        }
    }

    fun savePreset(
    ): Boolean {
        val currentService = getService().serviceInfo.name
        return entryStore.updateEntry(
            lastPresetId,
            currentService,
            searchFilterLogic.selectedContentFilters,
            searchFilterLogic.selectedSortFilters
        )
    }

    fun savePresetAs(
        name: String,
    ) {
        val currentService = getService().serviceInfo.name
        entryStore.addEntry(
            name,
            currentService,
            searchFilterLogic.selectedContentFilters,
            searchFilterLogic.selectedSortFilters
        )
    }

    fun isPresetNameAvailable(service: String, name: String): Boolean {
        return entryStore.isNameAvailable(service, name)
    }
    // end -- preset database access stuff
}
