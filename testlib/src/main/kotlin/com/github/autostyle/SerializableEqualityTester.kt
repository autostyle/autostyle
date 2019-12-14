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

import com.diffplug.common.base.Box
import com.diffplug.common.testing.EqualsTester
import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.*

abstract class SerializableEqualityTester {
    companion object {
        private fun <T : Serializable?> reserialize(input: T): T {
            val asBytes = LazyForwardingEquality.toBytes(input)
            val byteInput = asBytes.inputStream()
            try {
                ObjectInputStream(byteInput).use { objectInput -> return objectInput.readObject() as T }
            } catch (e: IOException) {
                throw ThrowingEx.asRuntime(e)
            } catch (e: ClassNotFoundException) {
                throw ThrowingEx.asRuntime(e)
            }
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
        val currentGroup = Box.of<MutableList<Any>>(ArrayList())
        val api = object : API {
            override fun areDifferentThan() {
                currentGroup.modify { current: MutableList<Any> ->
                    // create two instances, and add them to the group
                    current.add(create())
                    current.add(create())
                    // create two instances using a serialization roundtrip, and add them to the group
                    current.add(reserialize(create()))
                    current.add(reserialize(create()))
                    // add this group to the list of all groups
                    allGroups.add(current)
                    mutableListOf()
                }
            }
        }
        try {
            setupTest(api)
        } catch (e: Exception) {
            throw AssertionError("Error during setupTest", e)
        }
        val lastGroup: List<Any> = currentGroup.get()
        require(lastGroup.isEmpty()) { "Looks like you forgot to make a final call to 'areDifferentThan()'." }
        val tester = EqualsTester()
        for (step in allGroups) {
            tester.addEqualityGroup(*step.toTypedArray())
        }
        tester.testEquals()
    }
}
