package org.tobi29.scapes.engine.gradle

import org.gradle.api.Project
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider

inline fun <reified T> Project.property(): PropertyState<T> =
        property(T::class.java)

fun <T, M> Provider<T>.map(block: (T) -> M): Provider<M> = object : Provider<M> {
    override fun isPresent() = this@map.isPresent()

    override fun get() = block(this@map.get())

    override fun getOrNull() = this@map.getOrNull()?.let { block(it) }
}

fun <L, R, M> map(left: Provider<L>,
                  right: Provider<R>,
                  fold: (L, R) -> M): Provider<M> = object : Provider<M> {
    override fun isPresent() = left.isPresent() && right.isPresent()

    override fun get() = fold(left.get(), right.get())

    override fun getOrNull() = left.getOrNull()?.let { l ->
        right.getOrNull()?.let {
            fold(l, it)
        }
    }
}

fun <T> provider(value: T) = object : Provider<T> {
    override fun isPresent() = true

    override fun get() = value

    override fun getOrNull() = value
}

fun <T> provider(block: () -> T) = object : Provider<T> {
    override fun isPresent() = true

    override fun get() = block()

    override fun getOrNull() = block()
}

fun <T> Provider<T>.lazyString(): Provider<String> = object : Provider<String> {
    override fun isPresent() = this@lazyString.isPresent()

    override fun get() = this@lazyString.get().toString()

    override fun getOrNull() = this@lazyString.getOrNull()?.toString()

    override fun toString() = get()
}

// TODO: Implement without Ref
fun <T> Provider<T>.toClosure() = { get() }.toClosure()
