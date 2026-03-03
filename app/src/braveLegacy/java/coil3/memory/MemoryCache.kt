package coil3.memory

import coil3.FakeCoilImageBitmap
import coil3.Image
import coil3.bravePipeLegacy.PicassoHelper

class MemoryCache {
    class Value(val fakeCoilImageBitmap: FakeCoilImageBitmap) {
        fun getImage(): Image = fakeCoilImageBitmap
    }

    class Key(val url: String, objectObjectMap: Map<Any, Any>)

    // the code might work
    fun get(key: Key): Value {
        val bitmap = PicassoHelper.getImageFromCacheIfPresent(key.url)
        return Value(FakeCoilImageBitmap(bitmap))
    }

    fun clear() {}
}
