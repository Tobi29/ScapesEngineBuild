package org.tobi29.scapes.engine.gradle

import org.gradle.api.Project
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

inline fun <reified T> Project.property(): PropertyState<T> =
        property(T::class.java)

// TODO: Using internal API, how resolve?
inline fun <T, reified M> Provider<T>.map(
        crossinline block: (T) -> M
): Provider<M> = object : ProviderInternal<M> {
    override fun getType() = M::class.java

    override fun isPresent() = this@map.isPresent()

    override fun get() = block(this@map.get())

    override fun getOrNull() = this@map.getOrNull()?.let { block(it) }
}

// TODO: Using internal API, how resolve?
inline fun <L, R, reified M> map(
        left: Provider<L>,
        right: Provider<R>,
        crossinline fold: (L, R) -> M
): Provider<M> = object : ProviderInternal<M> {
    override fun getType() = M::class.java

    override fun isPresent() = left.isPresent() && right.isPresent()

    override fun get() = fold(left.get(), right.get())

    override fun getOrNull() = left.getOrNull()?.let { l ->
        right.getOrNull()?.let {
            fold(l, it)
        }
    }
}

fun <T> ProviderFactory.provider(value: T): Provider<T> = provider { value }

fun <T> Provider<T>.lazyString(): Provider<String> = map { toString() }

fun <T> Provider<T>.toClosure() = { get() }.toClosure()
