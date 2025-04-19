package org.schabi.newpipe.fragments.detail;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.schabi.newpipe.brave.bus.events.BraveEvents;
import org.schabi.newpipe.brave.fragments.BraveBackStackFragment;
import org.schabi.newpipe.brave.fragments.BraveHostFragment;
import org.schabi.newpipe.databinding.FragmentVideoDetailBinding;
import org.schabi.newpipe.brave.bus.BraveBus;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.fragments.BaseStateFragment;
import org.schabi.newpipe.fragments.list.comments.CommentRepliesFragment;
import org.schabi.newpipe.brave.views.BraveInterceptTouchRelativeLayout;
import org.schabi.newpipe.player.ui.VideoPlayerUi;
import org.schabi.newpipe.util.DeviceUtils;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static org.schabi.newpipe.ktx.ViewUtils.animateRotation;

/**
 * Handle BravePipe specific features.
 * <p>
 * It receives below events:
 * <ul>
 * <li>{@link BraveEvents.PrefEventScrollOnlyBelowPlayer}</li>
 * <li>{@link BraveEvents.EventBackPressedInCommentRepliesFragment}</li>
 * <li>{@link BraveEvents.EventShowCommentRepliesFragment}</li>
 * </ul>
 * <p>
 * It sends this events:
 * <ul>
 * <li>{@link BraveEvents.EventViewPagersContentHeight}</li>
 * </ul>
 */
public abstract class BraveVideoDetailFragment extends BaseStateFragment<StreamInfo>
        implements BraveEvents.EventShowCommentRepliesFragment.Handler,
        BraveEvents.EventBackPressedInCommentRepliesFragment.Handler,
        BraveEvents.PrefEventScrollOnlyBelowPlayer.Handler {

    private FragmentVideoDetailBinding binding;
    private int lastHeight = -1;
    private View.OnLayoutChangeListener onChangeLayoutListener = null;

    private void hideTitleAndSecondaryControls() {
        if (binding.detailContentRootHiding.getVisibility() == View.GONE) {
            binding.detailVideoTitleView.setMaxLines(10);
            animateRotation(binding.detailToggleSecondaryControlsView,
                    VideoPlayerUi.DEFAULT_CONTROLS_DURATION, 180);
            binding.detailContentRootHiding.setVisibility(View.VISIBLE);
        } else {
            binding.detailVideoTitleView.setMaxLines(1);
            animateRotation(binding.detailToggleSecondaryControlsView,
                    VideoPlayerUi.DEFAULT_CONTROLS_DURATION, 0);
            binding.detailContentRootHiding.setVisibility(View.GONE);
        }
        // view pager height has changed, update the tab layout
        updateTabLayoutVisibility();
        addGlobalLayoutListener();
    }

    protected void addGlobalLayoutListener() {
        final View rootLayout = binding.detailContentRootLayout;
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        BraveBus.Helpers.postEventViewPagersContentHeightChanged(
                                recalcPossibleViewPagersContentHeight());
                    }
                });
    }

    private int recalcPossibleViewPagersContentHeight() {

        int height = 800; // assume as default height
        try {
            height = binding.detailMainContent.getHeight()
                    - binding.playerPlaceholder.getHeight()
                    - binding.detailContentRootLayout.getHeight()
                    - binding.tabLayout.getHeight();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return height;
    }

    protected void braveSetBinding(final FragmentVideoDetailBinding binder) {
        this.binding = binder;
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handlePrefEventScrollOnlyBelowPlayer(
            final BraveEvents.PrefEventScrollOnlyBelowPlayer event) {

        if (!DeviceUtils.isLandscape(requireContext()) && event.doScrollInViewPager) {
            binding.detailContentRootLayout.setOnInterceptTouchEventListener(
                    new BraveInterceptTouchRelativeLayout
                            .NoOuterScrollingWhileInlineCommentsEnabledListener());
            binding.detailToggleSecondaryControlsView.setOnClickListener(v ->
                    hideTitleAndSecondaryControls());
            binding.viewPager.addOnLayoutChangeListener(createLayoutListener());
        } else {
            binding.detailContentRootLayout.setOnInterceptTouchEventListener(null);
            binding.detailToggleSecondaryControlsView.setOnClickListener(null);
            // set to false as only the parent View should handle the click events now
            binding.detailToggleSecondaryControlsView.setClickable(false);

            if (null != onChangeLayoutListener) {
                binding.viewPager.removeOnLayoutChangeListener(onChangeLayoutListener);
            }
        }
    }

    /**
     * The {@link android.view.ViewTreeObserver.OnGlobalLayoutListener} is not enough.
     * <p>
     * We need to listen on layout changes of the ViewPager. It only sends a new event
     * in case the height actually changed.
     * @return the listener
     */
    private View.OnLayoutChangeListener createLayoutListener() {
        if (onChangeLayoutListener != null) {
            return onChangeLayoutListener;
        }

        onChangeLayoutListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    final View v,
                    final int left,
                    final int top,
                    final int right,
                    final int bottom,
                    final int oldLeft,
                    final int oldTop,
                    final int oldRight,
                    final int oldBottom) {
                final int actualHeight = recalcPossibleViewPagersContentHeight(
                );
                if (lastHeight != actualHeight) {
                    lastHeight = actualHeight;
                    BraveBus.Helpers.postEventViewPagersContentHeightChanged(actualHeight);
                }
            }
        };

        return onChangeLayoutListener;
    }

    // This method is already present in VideoDetailFragment but as we need to call
    // it here too. We sneak it into this class here.
    public abstract void updateTabLayoutVisibility();

    /**
     * Handle android back button pressed.
     * @return true if it was handled here
     */
    protected boolean braveOnBackPressed() {
        if (BraveBus.Helpers.isPrefCommentRepliesSameWindowEnabled()) {
            return BraveBackStackFragment.handleBackPressed(getChildFragmentManager());
        } else {
            return false;
        }
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handleEventBackPressedInCommentRepliesFragment(
            final BraveEvents.EventBackPressedInCommentRepliesFragment event) {
        BraveBackStackFragment.handleBackPressed(getChildFragmentManager());
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void handleEventShowCommentRepliesFragment(
            final BraveEvents.EventShowCommentRepliesFragment event) {
        replaceCommentsFragmentInViewPager(
                event.item,
                binding.viewPager.getAdapter(),
                binding.viewPager);
    }

    public void replaceCommentsFragmentInViewPager(
            @NonNull final CommentsInfoItem comment,
            final PagerAdapter pagerAdapter,
            final ViewPager viewPager) {
        final TabAdapter customPagerAdapter = (TabAdapter) pagerAdapter;
        final BraveHostFragment hostFragment =
                (BraveHostFragment) customPagerAdapter.getItem(viewPager.getCurrentItem());
        hostFragment.replaceFragment(new CommentRepliesFragment(comment), true);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(
            @NonNull final View rootView,
            final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        BraveBus.getBus().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BraveBus.getBus().unregister(this);
    }
}
