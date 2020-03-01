package com.github.adamantcheese.chan.core.loader

import io.reactivex.Single

abstract class OnDemandContentLoader(
        val loaderType: LoaderType
) {
    abstract fun isAlreadyCached(postLoaderData: PostLoaderData): Boolean
    abstract fun startLoading(postLoaderData: PostLoaderData): Single<LoaderResult>
    abstract fun cancelLoading(postLoaderData: PostLoaderData)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is OnDemandContentLoader) {
            return false
        }

        if (loaderType != other.loaderType) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return loaderType.hashCode()
    }

}