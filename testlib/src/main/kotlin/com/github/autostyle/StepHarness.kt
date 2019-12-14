/*
 * Copyright 2016 DiffPlug
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

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.util.function.Consumer

/** An api for adding test cases.  */
class StepHarness(private val formatter: FormatterFunc) : AutoCloseable {
    companion object {
        /** Creates a harness for testing steps which don't depend on the file.  */
        @JvmStatic
        fun forStep(step: FormatterStep): StepHarness {
            // We don't care if an individual FormatterStep is misbehaving on line-endings, because
            // Formatter fixes that.  No reason to care in tests either.  It's likely to pop up when
            // running tests on Windows from time-to-time
            return StepHarness(
                FormatterFunc.Closeable.of(
                    {
                        if (step is FormatterStepImpl.Standard<*>) {
                            step.cleanupFormatterFunc()
                        }
                    }
                ) { input ->
                    LineEnding.toUnix(
                        step.format(input, File(""))
                    )
                }
            )
        }

        /** Creates a harness for testing a formatter whose steps don't depend on the file.  */
        fun forFormatter(formatter: Formatter) = StepHarness(
            FormatterFunc.Closeable.of(
                { formatter.close() }
            ) { input ->
                formatter.compute(input, File(""))
            }
        )
    }

    /** Asserts that the given element is transformed as expected, and that the result is idempotent.  */
    fun test(before: String, after: String): StepHarness {
        val actual = formatter.apply(before)
        assertThat(actual).isEqualTo(after)
            .`as`("before: [%s]", before)
        return testUnaffected(after)
    }

    /** Asserts that the given element is idempotent w.r.t the step under test.  */
    fun testUnaffected(idempotentElement: String): StepHarness {
        val actual = formatter.apply(idempotentElement)
        assertThat(actual).isEqualTo(idempotentElement)
            .`as`("formatter should be idempotent")
        return this
    }

    /** Asserts that the given elements in  the resources directory are transformed as expected.  */
    fun testResource(resourceBefore: String, resourceAfter: String): StepHarness {
        val before = ResourceHarness.getTestResource(resourceBefore)
        val after = ResourceHarness.getTestResource(resourceAfter)
        return test(before, after)
    }

    /** Asserts that the given elements in the resources directory are transformed as expected.  */
    fun testResourceUnaffected(resourceIdempotent: String): StepHarness {
        val idempotentElement = ResourceHarness.getTestResource(resourceIdempotent)
        return testUnaffected(idempotentElement)
    }

    /** Asserts that the given elements in the resources directory are transformed as expected.  */
    fun testException(
        resourceBefore: String,
        exceptionAssertion: Consumer<AbstractThrowableAssert<*, out Throwable?>?>
    ): StepHarness {
        val before = ResourceHarness.getTestResource(resourceBefore)
        try {
            formatter.apply(before)
            Assertions.fail("Formatter should fail on $before")
        } catch (t: Throwable) {
            val abstractAssert = assertThat(t)
            exceptionAssertion.accept(abstractAssert)
        }
        return this
    }

    override fun close() {
        if (formatter is FormatterFunc.Closeable) {
            formatter.close()
        }
    }
}
