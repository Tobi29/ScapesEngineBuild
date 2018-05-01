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

@file:Suppress("NOTHING_TO_INLINE")

package org.tobi29.scapes.engine.gradle

import groovy.util.BuilderSupport

inline operator fun BuilderSupport.invoke(
    method: String,
    block: MutableMap<String, String>.() -> Unit
) = invoke(method, argMap(block))

inline operator fun BuilderSupport.invoke(
    method: String,
    args: Map<String, String>
): Any? = invokeMethod(method, args)

inline operator fun BuilderSupport.invoke(
    method: String,
    arg: String
): Any? = invokeMethod(method, arg)

inline operator fun BuilderSupport.invoke(
    method: String,
    block: MutableMap<String, String>.() -> Unit,
    crossinline closure: BuilderSupport.() -> Unit
) = invoke(method, argMap(block), closure)

inline operator fun BuilderSupport.invoke(
    method: String,
    args: Map<String, String>,
    crossinline closure: BuilderSupport.() -> Unit
): Any? = invokeMethod(method, listOf(args, { closure(this) }.toClosure()))

inline fun argMap(
    block: MutableMap<String, String>.() -> Unit
) = map(block)

inline fun <K : Any, V> map(
    block: MutableMap<K, V>.() -> Unit
) = HashMap<K, V>().apply { block(this) }