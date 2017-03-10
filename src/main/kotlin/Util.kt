/*
 * Copyright 2012-2017 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.lang.Closure
import org.gradle.api.Project
import java.io.File

fun <R> Function0<R>.toClosure() = KotlinClosure0(this)
fun <R, P0> Function1<R, P0>.toClosure() = KotlinClosure1(this)
fun <R, P0, P1> Function2<R, P0, P1>.toClosure() = KotlinClosure2(this)
fun <R, P0, P1, P2> Function3<R, P0, P1, P2>.toClosure() = KotlinClosure3(this)

class KotlinClosure0<R>(val function: () -> R,
                        owner: Any? = null,
                        thisObject: Any? = null) : Closure<R>(owner,
        thisObject) {

    @Suppress("unused")
    fun doCall(): R = function()
}

class KotlinClosure1<R, in P0>(val function: (P0) -> R,
                               owner: Any? = null,
                               thisObject: Any? = null) : Closure<R>(owner,
        thisObject) {

    @Suppress("unused")
    fun doCall(p0: P0): R = function(p0)
}

class KotlinClosure2<R, in P0, in P1>(val function: (P0, P1) -> R,
                                      owner: Any? = null,
                                      thisObject: Any? = null) : Closure<R>(
        owner,
        thisObject) {

    @Suppress("unused")
    fun doCall(p0: P0,
               p1: P1): R = function(p0, p1)
}

class KotlinClosure3<R, in P0, in P1, in P2>(val function: (P0, P1, P2) -> R,
                                             owner: Any? = null,
                                             thisObject: Any? = null) : Closure<R>(
        owner,
        thisObject) {

    @Suppress("unused")
    fun doCall(p0: P0,
               p1: P1,
               p2: P2): R = function(p0, p1, p2)
}

fun Any?.resolve(): Any? {
    var current = this
    while (true) {
        when (current) {
            is Function0<*> -> current = current().resolve()
            is Closure<*> -> current = current.call().resolve()
            else -> return current
        }
    }
}

inline fun <reified E> Any?.resolveTo() = resolve() as E

fun Any?.resolveToString() = resolve().toString()

class Ref<out T>(val get: () -> T) : Function0<T> {
    override fun invoke() = get()

    override fun equals(other: Any?) = get() == other

    override fun hashCode() = get()?.hashCode() ?: 0

    override fun toString() = get().toString()
}

fun <T> T.asRef() = Ref { this }

operator fun <T> Ref<T>?.invoke() = this?.invoke()

fun Project.file(file: Ref<File>): File = file(file())
