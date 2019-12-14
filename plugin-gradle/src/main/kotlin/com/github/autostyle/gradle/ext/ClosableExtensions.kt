/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
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
package com.github.autostyle.gradle.ext

import java.io.Closeable

inline fun <T1 : Closeable?, T2 : Closeable?, R> T1.use(wrap1: (T1) -> T2, block: (T2) -> R): R =
    use { t1 ->
        wrap1(t1).use(block)
    }

inline fun <T1 : Closeable?, T2 : Closeable?, T3 : Closeable?, R> T1.use(
    wrap1: (T1) -> T2,
    wrap2: (T2) -> T3,
    block: (T3) -> R
): R =
    use(wrap1) { t2 ->
        wrap2(t2).use(block)
    }

inline fun <T1 : Closeable?, T2 : Closeable?, T3 : Closeable?, T4 : Closeable?, R> T1.use(
    wrap1: (T1) -> T2,
    wrap2: (T2) -> T3,
    wrap3: (T3) -> T4,
    block: (T4) -> R
): R =
    use(wrap1, wrap2) {
        wrap3(it).use(block)
    }

inline fun <T1 : Closeable?, T2 : Closeable?, T3 : Closeable?, T4 : Closeable?, T5 : Closeable?, R> T1.use(
    wrap1: (T1) -> T2,
    wrap2: (T2) -> T3,
    wrap3: (T3) -> T4,
    wrap4: (T4) -> T5,
    block: (T5) -> R
): R =
    use(wrap1, wrap2, wrap3) {
        wrap4(it).use(block)
    }
