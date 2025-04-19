package org.schabi.newpipe.fragments.list.comments;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.schabi.newpipe.R;
import org.schabi.newpipe.brave.bus.events.BraveEvents;
import org.schabi.newpipe.brave.fragments.BraveEventBusBaseListInfoFragment;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.brave.bus.BraveBus;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.util.Localization;

public abstract class BraveCommentRepliesFragment
        extends BraveEventBusBaseListInfoFragment<CommentsInfoItem, CommentRepliesInfo> {

    protected BraveCommentRepliesFragment(
            final UserAction errorUserAction) {
        super(errorUserAction);
    }

    protected void braveInitReplyTitleAndCustomBackButton(
            final View rootView,
            final CommentsInfoItem commentsInfoItem) {

        final LinearLayout layout = rootView.findViewById(R.id.replies_layout);
        if (BraveBus.Helpers.isPrefCommentRepliesSameWindowEnabled()) {
            final Button backButton = rootView.findViewById(R.id.back_button);
            backButton.setText(
                    Localization.replyCount(requireContext(), commentsInfoItem.getReplyCount()));
            backButton.setOnClickListener(v -> BraveBus.getBus()
                    .post(new BraveEvents.EventBackPressedInCommentRepliesFragment()));
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void doResizeViewPagersContent(final int height) {
        final int backButtonHeight =
                getActivity().findViewById(R.id.back_button).getLayoutParams().height;
        final ViewGroup.LayoutParams params = itemsList.getLayoutParams();
        params.height = height -  backButtonHeight;
        itemsList.setLayoutParams(params);
        itemsList.requestLayout();
    }

    @Override
    public void setTitle(
            final String title) {
        if (!BraveBus.Helpers.isPrefCommentRepliesSameWindowEnabled()) {
            super.setTitle(title);
        }
    }
}
