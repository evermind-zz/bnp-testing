package org.schabi.newpipe.brave.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.schabi.newpipe.brave.BraveConstants;
import org.schabi.newpipe.brave.bus.BraveBus;
import org.schabi.newpipe.brave.bus.events.BraveEvents;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.fragments.list.BaseListInfoFragment;
import org.schabi.newpipe.util.DeviceUtils;

import androidx.annotation.NonNull;

public abstract class BraveEventBusBaseListInfoFragment<I extends InfoItem, L extends ListInfo<I>>
        extends BaseListInfoFragment<I, L>
        implements BraveEvents.PrefEventScrollOnlyBelowPlayer.Handler,
        BraveEvents.EventViewPagersContentHeight.Handler {

    private int originalRecyclerViewHeight = BraveConstants.LAYOUT_LENGTH_UNSET;

    protected BraveEventBusBaseListInfoFragment(
            final UserAction errorUserAction) {
        super(errorUserAction);
    }

    @Override
    public void onViewCreated(
            final @NonNull View rootView,
            final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        BraveBus.getBus().register(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BraveBus.getBus().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handleEventViewPagersContentHeight(
            final BraveEvents.EventViewPagersContentHeight event) {
        doResizeViewPagersContent(event.height);
    }

    @Override
    protected void initListeners() {
        super.initListeners();
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handlePrefEventScrollOnlyBelowPlayer(
            final BraveEvents.PrefEventScrollOnlyBelowPlayer event) {

        if (!DeviceUtils.isLandscape(requireContext()) && event.doScrollInViewPager) {
            itemsList.setNestedScrollingEnabled(false);
        } else {
            itemsList.setNestedScrollingEnabled(true);
            // reset to former layout height
            if (!BraveConstants.Helper.isDimensionUnset(originalRecyclerViewHeight)) {
                storeOriginalRecyclerViewLayoutHeightAndSetNew(originalRecyclerViewHeight);
                originalRecyclerViewHeight = BraveConstants.LAYOUT_LENGTH_UNSET;
            }
        }
    }

    private void storeOriginalRecyclerViewLayoutHeightAndSetNew(
            final int height) {
        final ViewGroup.LayoutParams params = itemsList.getLayoutParams();

        // store original height if not already done
        if (BraveConstants.Helper.isDimensionUnset(originalRecyclerViewHeight)) {
            originalRecyclerViewHeight = params.height;
        }
        if (params.height != height) {
            params.height = height;
            itemsList.setLayoutParams(params);
            itemsList.requestLayout();
        }
    }

    /**
     * Do the resize of the ViewPager's content View.
     *
     * @param height new height the view inside the ViewPager should have
     */
    protected void doResizeViewPagersContent(
            final int height) {
        storeOriginalRecyclerViewLayoutHeightAndSetNew(height);
    }
}
