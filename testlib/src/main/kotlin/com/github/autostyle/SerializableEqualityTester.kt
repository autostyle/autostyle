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

import com.diffplug.common.testing.EqualsTester
import java.io.ObjectInputStream
import java.io.Serializable

abstract class SerializableEqualityTester {
    companion object {
        private inline fun <reified T : Serializable?> reserialize(input: T): T {
            val asBytes = LazyForwardingEquality.toBytes(input)
            val byteInput = asBytes.inputStream()
            return ObjectInputStream(byteInput).use { it.readObject() as T }
        }
    }

    protected abstract fun create(): Serializable

    @Throws(Exception::class)
    protected abstract fun setupTest(api: API)

    interface API {
        fun areDifferentThan()
    }

    fun testEquals() {
        val allGroups = mutableListOf<List<Any>>()
        val api = object : API {
            override fun areDifferentThan() {
                allGroups += listOf(
                    // create two instances, and add them to the group
                    create(),
                    create(),
                    // create two instances using a serialization roundtrip, and add them to the group
                    reserialize(create()),
                    reserialize(create())
                )
            }
        }
        setupTest(api)
        val tester = EqualsTester()
        for (step in allGroups) {
            tester.addEqualityGroup(*step.toTypedArray())
        }
        tester.testEquals()
    }
}
