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

package org.tobi29.scapes.engine.gradle

import groovy.lang.Closure

fun <R> (() -> R).toClosure() = KotlinClosure0(
        this)

fun <R, P0> ((P0) -> R).toClosure() = KotlinClosure1(
        this)

fun <R, P0, P1> ((P0, P1) -> R).toClosure() = KotlinClosure2(
        this)

fun <R, P0, P1, P2> ((P0, P1, P2) -> R).toClosure() = KotlinClosure3(
        this)

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