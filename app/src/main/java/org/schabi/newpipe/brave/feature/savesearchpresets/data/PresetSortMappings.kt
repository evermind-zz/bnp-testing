package org.schabi.newpipe.brave.feature.savesearchpresets.data

import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortDirection
import org.schabi.newpipe.brave.feature.savesearchpresets.domain.SortType

object PresetSortMappings {

    val sortToDbKey = mapOf(
        SortType.NAME to EntryDbKeys.SORT_BY_NAME,
        SortType.LAST_USED to EntryDbKeys.SORT_BY_LAST_USED,
        SortType.CREATED to EntryDbKeys.SORT_BY_CREATED,
        SortType.MODIFIED to EntryDbKeys.SORT_BY_MODIFIED//,
    )
    val dbKeyToSort = sortToDbKey.entries.associate { (k, v) -> v to k }

    val sortDirectionToDbKey = mapOf(
        SortDirection.DESC to EntryDbKeys.DIR_DESC,
        SortDirection.ASC to EntryDbKeys.DIR_ASC
    )
    val dbKeyToSortDirection = sortDirectionToDbKey.entries.associate { (k, v) -> v to k }
}