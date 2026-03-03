package coil3.network.okhttp

import okhttp3.Call

class OkHttpNetworkFetcherFactory(
    callFactory: Call.Factory? = null,
) : Factory
