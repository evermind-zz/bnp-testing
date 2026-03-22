package org.schabi.newpipe.brave.feature.savesearchpresets.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortDirection
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortType

/**
 * this is Preset database based just on a JSON String that is stored into SharedPreferences.
 *
 * Here we have the JSON example database structure:
 * {
 *  "version" : 1,
 *  "sorting" : "byName",
 *  "direction" : "desc",
 *  "defaults" : [ {
 *    "service" : "YouTube",
 *    "entry" : 1774216721895
 *  } ],
 *  "entries" : [ {
 *    "name" : "My Preset",
 *    "service" : "YouTube",
 *    "created_at" : 1774216721895, // this timestamp is used as unique id for each entry
 *    "modified_at" : 1774216753902,
 *    "last_used" : 1774216823270,
 *    "sort_filter_data" : [ 12, 17, 24 ],
 *    "content_filter_data" : [ 3 ]
 *  } ]
 *}
 */
class EntryDatabase(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun load(): DBState {
        val jsonString = prefs.getString(EntryDbKeys.PREF_KEY_JSON_DB, null)
            ?: return emptyState()

        val root = JsonParser.`object`().from(jsonString)

        val version = root.getInt(EntryDbKeys.VERSION)
        val sorting = PresetSortMappings.dbKeyToSort[root.getString(EntryDbKeys.SORTING)]!!
        val direction =
            PresetSortMappings.dbKeyToSortDirection[root.getString(EntryDbKeys.DIRECTION)]!!

        val defaults = mutableListOf<DefaultEntry>()
        val defaultsArray = root.getArray(EntryDbKeys.DEFAULTS)

        if (defaultsArray != null) {
            for (i in 0 until defaultsArray.size) {
                val obj = defaultsArray.getObject(i)

                defaults.add(
                    DefaultEntry(
                        obj.getString(EntryDbKeys.SERVICE),
                        obj.getLong(EntryDbKeys.ENTRY)
                    )
                )
            }
        }

        val entries = mutableListOf<Entry>()
        val entriesArray = root.getArray(EntryDbKeys.ENTRIES)

        if (entriesArray != null) {

            for (i in 0 until entriesArray.size) {
                val obj = entriesArray.getObject(i)

                val contentFilterArray = obj.getArray(EntryDbKeys.CONTENT_FILTER_DATA)
                val sortFilterArray = obj.getArray(EntryDbKeys.SORT_FILTER_DATA)
                val contentFilterData = mutableListOf<Int>()
                val sortFilterData = mutableListOf<Int>()

                for (j in 0 until contentFilterArray.size) {
                    contentFilterData.add(contentFilterArray.getInt(j))
                }

                for (j in 0 until sortFilterArray.size) {
                    sortFilterData.add(sortFilterArray.getInt(j))
                }

                entries.add(
                    Entry(
                        obj.getString(EntryDbKeys.NAME),
                        obj.getString(EntryDbKeys.SERVICE),
                        obj.getLong(EntryDbKeys.CREATED_AT),
                        obj.getLong(EntryDbKeys.MODIFIED_AT),
                        obj.getLong(EntryDbKeys.LAST_USED),
                        contentFilterData,
                        sortFilterData,
                    )
                )
            }
        }

        return DBState(version, sorting, direction, defaults, entries)
    }

    fun save(state: DBState) {
        val root = JsonObject()

        root[EntryDbKeys.VERSION] = state.version
        root[EntryDbKeys.SORTING] = PresetSortMappings.sortToDbKey[state.sorting]!!
        root[EntryDbKeys.DIRECTION] = PresetSortMappings.sortDirectionToDbKey[state.direction]!!

        val defaultsArray = JsonArray()

        for (d in state.defaults) {
            val obj = JsonObject()
            obj[EntryDbKeys.SERVICE] = d.service
            obj[EntryDbKeys.ENTRY] = d.entry
            defaultsArray.add(obj)
        }

        root[EntryDbKeys.DEFAULTS] = defaultsArray

        val entriesArray = JsonArray()

        for (e in state.entries) {
            val obj = JsonObject()

            obj[EntryDbKeys.NAME] = e.name
            obj[EntryDbKeys.SERVICE] = e.service
            obj[EntryDbKeys.CREATED_AT] = e.createdAt
            obj[EntryDbKeys.MODIFIED_AT] = e.modifiedAt
            obj[EntryDbKeys.LAST_USED] = e.lastUsed

            val sortFilterArray = JsonArray()
            val contentFilterArray = JsonArray()

            for (v in e.sortFilterData) {
                sortFilterArray.add(v)
            }
            obj[EntryDbKeys.SORT_FILTER_DATA] = sortFilterArray

            for (v in e.contentFilterData) {
                contentFilterArray.add(v)
            }
            obj[EntryDbKeys.CONTENT_FILTER_DATA] = contentFilterArray

            entriesArray.add(obj)
        }

        root[EntryDbKeys.ENTRIES] = entriesArray

        val json = JsonWriter.string(root)

        prefs.edit().putString(EntryDbKeys.PREF_KEY_JSON_DB, json).apply()
    }

    private fun emptyState(): DBState {
        return DBState(
            version = 1,
            sorting = PresetSortMappings.dbKeyToSort[EntryDbKeys.SORT_BY_NAME]!!,
            direction = PresetSortMappings.dbKeyToSortDirection[EntryDbKeys.DIR_DESC]!!,
            defaults = emptyList(),
            entries = emptyList()
        )
    }
}

data class Entry(
    val name: String,
    val service: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val lastUsed: Long,
    val contentFilterData: List<Int>,
    val sortFilterData: List<Int>
)

data class DefaultEntry(
    val service: String,
    val entry: Long
)

data class DBState(
    val version: Int,
    val sorting: SortType,
    val direction: SortDirection,
    val defaults: List<DefaultEntry>,
    val entries: List<Entry>
)