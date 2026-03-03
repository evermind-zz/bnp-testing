package coil3.util;

import android.widget.ImageView;

import org.schabi.newpipe.util.image.CoilHelper;

public final class CoilUtils {
    public static void dispose(ImageView view) {
        if (view != null) {
            CoilHelper.dispose(view);
        }
    }
}
