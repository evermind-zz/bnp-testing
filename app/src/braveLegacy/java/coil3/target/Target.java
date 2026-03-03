package coil3.target;

import coil3.Image;

public interface Target {
    void onError(Image error);

    void onStart(Image placeholder);

    void onSuccess(Image result);
}
