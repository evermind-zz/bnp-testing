package org.schabi.newpipe.brave.bus;

import org.greenrobot.eventbus.EventBus;
import org.schabi.newpipe.brave.bus.events.BraveEvents;

/**
 * Wrapper class for {@link EventBus}.
 * <p>
 * -> having one common place to change the EventBus behaviour.
 */
public final class BraveBus {

    private BraveBus() { }

    /**
     * The default EventBus.
     * <p>
     * A common method to call for all {@link EventBus} interactions. So later, if a custom
     * implementation is need instead, here we have a common place to do so instead of going
     * through all the code and replace the direct calls there.
     * @return the bus we are currently using
     */
    public static EventBus getBus() {
        return EventBus.getDefault();
    }

    /**
     * Convenience methods for some BraveEvents.
     */
    public static final class Helpers {

        private static BraveEvents.EventViewPagersContentHeight eventViewPagersContentHeight =
                new BraveEvents.EventViewPagersContentHeight();

        private Helpers() { }

        public static boolean isPrefCommentRepliesSameWindowEnabled() {
            final BraveEvents.PrefEventSameWindowCommentReplies setting = getBus()
                    .getStickyEvent(BraveEvents.PrefEventSameWindowCommentReplies.class);
            return setting.doSameWindowCommentReplies;
        }

        public static void postEventViewPagersContentHeightChanged(final int height) {
            eventViewPagersContentHeight.height = height;

            final BraveEvents.PrefEventScrollOnlyBelowPlayer scrollOnlyBelowPlayer = getBus()
                    .getStickyEvent(BraveEvents.PrefEventScrollOnlyBelowPlayer.class);

            if (scrollOnlyBelowPlayer != null && scrollOnlyBelowPlayer.doScrollInViewPager) {
                getBus().postSticky(eventViewPagersContentHeight);
            }
        }
    }
}
