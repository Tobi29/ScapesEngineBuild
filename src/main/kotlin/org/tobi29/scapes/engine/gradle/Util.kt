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

import org.gradle.kotlin.dsl.KotlinClosure0
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.KotlinClosure2

fun <R : Any> (() -> R?).toClosure() = KotlinClosure0(this)

fun <R : Any, P0> ((P0) -> R?).toClosure() = KotlinClosure1(this)

fun <R : Any, P0, P1> ((P0, P1) -> R?).toClosure() = KotlinClosure2(this)
