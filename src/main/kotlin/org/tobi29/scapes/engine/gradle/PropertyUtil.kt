package org.tobi29.scapes.engine.gradle

import org.gradle.api.Project
import org.gradle.api.internal.provider.AbstractProvider
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

inline fun <reified T> Project.property(): Property<T> =
        objects.property(T::class.java)

inline fun <L : Any, R : Any, reified T : Any> map(
        left: Provider<L>,
        right: Provider<R>,
        noinline fold: (L, R) -> T
): Provider<T> = mapImpl(T::class.java, left, right, fold)

// TODO: Using internal API, how resolve?
@PublishedApi
internal fun <L : Any, R : Any, T : Any> mapImpl(
        type: Class<T>,
        left: Provider<L>,
        right: Provider<R>,
        fold: (L, R) -> T
): Provider<T> = BiTransformProvider(type, fold, left, right)

fun <T> ProviderFactory.provider(value: T): Provider<T> = provider { value }

fun <T> Provider<T>.lazyString(): Provider<String> = map { toString() }

fun <T> Provider<T>.toClosure() = { get() }.toClosure()

/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Local copy of internal Gradle implementation
private class BiTransformProvider<in L : Any, in R : Any, T : Any>(
        private val type: Class<T>,
        private val transformer: (L, R) -> T,
        private val providerLeft: Provider<out L>,
        private val providerRight: Provider<out R>
) : AbstractProvider<T>() {
    override fun getType(): Class<T>? = type

    override fun isPresent(): Boolean =
            providerLeft.isPresent && providerRight.isPresent

    override fun get(): T =
            map(providerLeft.get(), providerRight.get())

    override fun getOrNull(): T? {
        val valueLeft = providerLeft.orNull
        val valueRight = providerRight.orNull
        return if (valueLeft != null && valueRight != null) {
            map(valueLeft, valueRight)
        } else null
    }

    fun map(left: L,
            right: R): T = transformer(left, right)
}
