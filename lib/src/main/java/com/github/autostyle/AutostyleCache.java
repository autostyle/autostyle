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
package com.github.autostyle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Autostyle global cache. {@link AutostyleCache#clear()} should be called
 * when Autostyle is no longer in use to release any resources it has grabbed.
 */
public final class AutostyleCache {
  /** Allows comparing keys based on their serialization. */
  static final class SerializedKey {
    final byte[] serialized;
    final int hashCode;

    SerializedKey(Serializable key) {
      Objects.requireNonNull(key);
      serialized = LazyForwardingEquality.toBytes(key);
      hashCode = Arrays.hashCode(serialized);
    }

    @Override
    public final boolean equals(Object other) {
      return other instanceof SerializedKey
          && Arrays.equals(serialized, ((SerializedKey) other).serialized);
    }

    @Override
    public final int hashCode() {
      return hashCode;
    }
  }

  final Map<SerializedKey, URLClassLoader> cache = new HashMap<>();

  @SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
  synchronized ClassLoader classloader(JarState state) {
    return classloader(state, state);
  }

  @SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
  synchronized ClassLoader classloader(Serializable key, JarState state) {
    SerializedKey serializedKey = new SerializedKey(key);
    return cache
        .computeIfAbsent(serializedKey, k -> new FeatureClassLoader(state.jarUrls(), this.getClass().getClassLoader()));
  }

  static AutostyleCache instance() {
    return instance;
  }

  /** Closes all cached classloaders. */
  public static void clear() {
    List<URLClassLoader> toDelete;
    synchronized (instance) {
      toDelete = new ArrayList<>(instance.cache.values());
      instance.cache.clear();
    }
    for (URLClassLoader classLoader : toDelete) {
      try {
        classLoader.close();
      } catch (IOException e) {
        throw ThrowingEx.asRuntime(e);
      }
    }
  }

  private static final AutostyleCache instance = new AutostyleCache();
}
