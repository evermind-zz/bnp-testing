package org.schabi.newpipe.brave.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.R;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Host another Fragment that might be replaced dynamically on runtime.
 */
public class BraveHostFragment extends BraveBackStackFragment {
    private Fragment fragment;

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.brave_host_fragment, container, false);
        if (fragment != null) {
            replaceFragment(fragment, false);
        }
        return view;
    }

    public void replaceFragment(
            final Fragment frag,
            final boolean addToBackstack) {
        if (addToBackstack) {
            getChildFragmentManager().beginTransaction().replace(R.id.hosted_fragment, frag)
                    .addToBackStack(null).commit();
        } else {
            getChildFragmentManager().beginTransaction().replace(R.id.hosted_fragment, frag)
                    .commit();
        }
    }

    public static BraveHostFragment newInstance(
            final Fragment fragment) {
        final BraveHostFragment braveHostFragment = new BraveHostFragment();
        braveHostFragment.fragment = fragment;
        return braveHostFragment;
    }
}
