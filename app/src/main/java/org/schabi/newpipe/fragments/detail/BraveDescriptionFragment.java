package org.schabi.newpipe.fragments.detail;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.schabi.newpipe.brave.bus.BraveBus;
import org.schabi.newpipe.brave.BraveConstants;
import org.schabi.newpipe.brave.bus.events.BraveEvents;
import org.schabi.newpipe.util.DeviceUtils;

import androidx.annotation.NonNull;

public abstract class BraveDescriptionFragment extends BaseDescriptionFragment
        implements BraveEvents.PrefEventScrollOnlyBelowPlayer.Handler,
        BraveEvents.EventViewPagersContentHeight.Handler {

    private int originalScrollViewHeight = BraveConstants.LAYOUT_LENGTH_UNSET;

    @Override
    public void onViewCreated(
            final @NonNull View rootView,
            final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        BraveBus.getBus().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BraveBus.getBus().unregister(this);
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handleEventViewPagersContentHeight(
            final BraveEvents.EventViewPagersContentHeight event) {
        storeOriginalScrollViewLayoutHeightAndSetNew(event.height);
    }

    private void storeOriginalScrollViewLayoutHeightAndSetNew(
            final int height) {
        final ViewGroup.LayoutParams params = binding.scrollView.getLayoutParams();

        // store original height if not already done
        if (BraveConstants.Helper.isDimensionUnset(originalScrollViewHeight)) {
            originalScrollViewHeight = params.height;
        }

        if (params.height != height) {
            params.height = height;
            binding.scrollView.setLayoutParams(params);
            binding.scrollView.requestLayout();
        }
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handlePrefEventScrollOnlyBelowPlayer(
            final BraveEvents.PrefEventScrollOnlyBelowPlayer event) {
        if (!DeviceUtils.isLandscape(requireContext()) && event.doScrollInViewPager) {
            binding.scrollView.setNestedScrollingEnabled(false);
        } else {
            binding.scrollView.setNestedScrollingEnabled(true);
            if (!BraveConstants.Helper.isDimensionUnset(originalScrollViewHeight)) {
                storeOriginalScrollViewLayoutHeightAndSetNew(originalScrollViewHeight);
                originalScrollViewHeight = BraveConstants.LAYOUT_LENGTH_UNSET;
            }
        }
    }
}
