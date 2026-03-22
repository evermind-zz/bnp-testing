// Created by evermind-zz 2022, licensed GNU GPL version 3 or later
package org.schabi.newpipe.fragments.list.search.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import org.schabi.newpipe.R
import org.schabi.newpipe.fragments.list.search.SearchViewModel

/**
 * Base dialog class for [DialogFragment] based search filter dialogs.
 */
abstract class BaseSearchFilterDialogFragment : DialogFragment() {
    protected var dialogGenerator: BaseSearchFilterUiGenerator? = null

    @JvmField
    protected var searchViewModel: SearchViewModel? = null

    private fun createSearchFilterUi() {
        dialogGenerator = createSearchFilterDialogGenerator()
        dialogGenerator!!.createSearchUI()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        // Avoid multiple instances of the dialog that could be triggered by multiple taps
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag)
        }
    }

    protected abstract fun createSearchFilterDialogGenerator(): BaseSearchFilterUiGenerator

    /**
     * As we have different bindings we need to get this sorted in a method.
     *
     * @return the [Toolbar] null if there is no toolbar available.
     */
    protected abstract val toolbar: Toolbar?

    protected abstract fun getRootView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure that the first parameter is pointing to instance of SearchFragment otherwise
        // another SearchViewModel object will be created instead of the existing one used.
        // -> the SearchViewModel is first instantiated in SearchFragment. Here we just use it.
        searchViewModel =
            ViewModelProvider(requireParentFragment()).get<SearchViewModel>(SearchViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = getRootView(inflater, container)
        createSearchFilterUi()
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = this.toolbar
        if (toolbar != null) {
            initToolbar(toolbar)
        }
    }

    /**
     * Initialize the toolbar.
     *
     * This method is only called if [.getToolbar] is implemented to return a toolbar.
     *
     * @param toolbar the actual toolbar for this dialog fragment
     */
    protected open fun initToolbar(toolbar: Toolbar) {
        toolbar.setTitle(R.string.filter)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.inflateMenu(R.menu.menu_search_filter_dialog_fragment)
        toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> dismiss() })
        toolbar.setNavigationContentDescription(R.string.cancel)

        val okButton = toolbar.findViewById<View>(R.id.search)
        okButton.isEnabled = true

        val resetButton = toolbar.findViewById<View>(R.id.reset)
        resetButton.isEnabled = true

        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item: MenuItem? ->
            if (item!!.itemId == R.id.search) {
                searchViewModel!!.searchFilterLogic.prepareForSearch()
                dismiss()
                return@OnMenuItemClickListener true
            } else if (item.itemId == R.id.reset) {
                searchViewModel!!.searchFilterLogic.reset()
                return@OnMenuItemClickListener true
            }
            false
        })
    }
}
