package org.schabi.newpipe.brave.fragments;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public abstract class BraveBackStackFragment extends Fragment {
    public static boolean handleBackPressed(
            final FragmentManager fm) {
        if (fm.getFragments() != null) {
            for (final Fragment frag : fm.getFragments()) {
                if (frag != null && frag.isVisible() && frag instanceof BraveBackStackFragment) {
                    if (((BraveBackStackFragment) frag).onBackPressed()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean onBackPressed() {
        final FragmentManager fm = getChildFragmentManager();
        if (handleBackPressed(fm))  {
            return true;
        } else if (getUserVisibleHint() && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return true;
        }
        return false;
    }
}
