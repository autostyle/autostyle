/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    id("build.java-published-platform")
}

dependencies {
    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    constraints {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        api("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")
        api("com.googlecode.concurrent-trees:concurrent-trees:2.6.1")
        api("org.slf4j:slf4j-api:1.7.36")
        api("org.slf4j:slf4j-log4j12:1.7.36")
    }
}
