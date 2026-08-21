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
import java.util.List;
import java.util.Map;

import global.goldenera.cryptoj.enums.Network;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final Map<Network, List<SoftwareVersionPolicy>> SOFTWARE_VERSION_POLICIES = Map.of(
            Network.MAINNET, List.of(
                    new SoftwareVersionPolicy(0L, "0.0.1"),
                    new SoftwareVersionPolicy(731_503L, "0.1.1")),
            Network.TESTNET, List.of(
                    new SoftwareVersionPolicy(0L, "0.0.1"),
                    new SoftwareVersionPolicy(716_824L, "0.1.1")));

    public static String requiredSoftwareVersion(Network network, long nodeHeight) {
        List<SoftwareVersionPolicy> policies = SOFTWARE_VERSION_POLICIES.get(network);
        if (policies == null || nodeHeight < 0) {
            throw new IllegalArgumentException("No software version policy configured for network: " + network);
        }
        return policies.stream()
                .filter(policy -> nodeHeight >= policy.activationHeight())
                .reduce((previous, current) -> current)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active software version policy for network " + network + " at height " + nodeHeight))
                .requiredVersion();
    }

    public static boolean shouldRejectNodeVersion(Network network, long nodeHeight, String nodeVersionStr) {
        try {
            Version nodeVersion = Version.parse(nodeVersionStr);
            Version requiredVersion = Version.parse(requiredSoftwareVersion(network, nodeHeight));
            return nodeVersion.compareTo(requiredVersion) < 0;
        } catch (Exception e) {
            return true;
        }
    }

    public record SoftwareVersionPolicy(long activationHeight, String requiredVersion) {
    }
}
