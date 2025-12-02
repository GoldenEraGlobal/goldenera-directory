/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025-2030 The GoldenEraGlobal Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package global.goldenera.directory;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Fork config
    public static final Map<ForkName, Long> FORK_ACTIVATION_BLOCKS = Map.of(ForkName.GENESIS, 0L);
    public static final Map<ForkName, String> REQUIRED_SOFTWARE_VERSION_FOR_FORK = Map.of(ForkName.GENESIS, "0.0.1");

    public enum ForkName {
        GENESIS;
    }

    public static boolean isForkActive(ForkName fork, long blockHeight) {
        Long activationHeight = FORK_ACTIVATION_BLOCKS.get(fork);
        if (activationHeight == null) {
            return false;
        }
        return blockHeight >= activationHeight;
    }

    public static boolean shouldNodeShutdown(long nodeHeight, String nodeVersionStr) {
        try {
            Version nodeVersion = Version.parse(nodeVersionStr);
            for (ForkName fork : ForkName.values()) {
                if (isForkActive(fork, nodeHeight)) {
                    String requiredVersionStr = REQUIRED_SOFTWARE_VERSION_FOR_FORK.get(fork);
                    if (requiredVersionStr != null) {
                        Version requiredVersion = Version.parse(requiredVersionStr);
                        if (nodeVersion.compareTo(requiredVersion) < 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}
