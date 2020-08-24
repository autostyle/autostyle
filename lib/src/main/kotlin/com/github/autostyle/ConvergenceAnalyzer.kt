/*
 * Copyright 2020 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
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
package com.github.autostyle

import java.io.File
import java.util.logging.Logger

private val listComparator = compareBy<String>({ it.length }, { it })

sealed class ConvergenceResult(open val formatted: String) {
    object Clean : ConvergenceResult("") {
        override val formatted: String
            get() = throw IllegalStateException("The formatting result is not known")
    }

    data class Convergence(val cycle: List<String>) : ConvergenceResult(cycle.last())
    data class Cycle(val cycle: List<String>) : ConvergenceResult(cycle.minWith(listComparator)!!)
    data class Divergence(val cycle: List<String>) : ConvergenceResult("") {
        override val formatted: String
            get() = throw IllegalStateException("The formatting result diverges")
    }
}

private val logger = Logger.getLogger(ConvergenceAnalyzer::class.java.name)

class ConvergenceAnalyzer(
    private val formatter: Formatter,
    private val maxAttempts: Int = 10
) {
    fun analyze(file: File): ConvergenceResult {
        logger.fine { "Applying format to $file" }
        val formatted = formatter.formatOrNull(file) ?: return ConvergenceResult.Clean
        val onceMore = formatter.compute(formatted, file)
        if (onceMore == formatted) {
            return ConvergenceResult.Convergence(listOf(formatted))
        }
        logger.fine { "The formatter should keep the file intact on second formatting, however the result differs for $file." }

        // Maps the result to the number of formatter calls
        val cycle = mutableMapOf<String, Int>()
        cycle[formatted] = 1
        cycle[onceMore] = 2
        var current = onceMore
        for (i in 3..maxAttempts) {
            val next = formatter.compute(current, file)
            if (next == current) {
                return ConvergenceResult.Convergence(cycle.keys.toList())
            }
            current = next
            val prev = cycle.put(current, i) ?: continue
            return ConvergenceResult.Cycle(cycle.mapNotNull { e -> e.key.takeIf { e.value > prev } })
        }
        return ConvergenceResult.Divergence(cycle.keys.toList())
    }
}
