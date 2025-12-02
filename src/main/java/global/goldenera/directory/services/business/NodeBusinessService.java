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
package global.goldenera.directory.services.business;

import org.apache.tuweni.bytes.Bytes;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import global.goldenera.cryptoj.datatypes.Address;
import global.goldenera.cryptoj.datatypes.Hash;
import global.goldenera.cryptoj.datatypes.Signature;
import global.goldenera.cryptoj.enums.Network;
import global.goldenera.directory.Constants;
import global.goldenera.directory.api.v1.node.dtos.NodeInfoDtoV1;
import global.goldenera.directory.api.v1.node.dtos.NodePingDtoV1;
import global.goldenera.directory.api.v1.node.dtos.NodePongDtoV1;
import global.goldenera.directory.api.v1.node.dtos.NodePongPayloadDtoV1;
import global.goldenera.directory.exceptions.GEAuthenticationException;
import global.goldenera.directory.exceptions.GEValidationException;
import global.goldenera.directory.properties.PropertiesGeneralConfig;
import global.goldenera.directory.services.system.IdentityService;
import global.goldenera.directory.utils.RlpEncoderUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
public class NodeBusinessService {

	private static final long MAX_TIMESTAMP_SKEW_SECONDS = 60;

	IdentityService identityService;

	Cache<Address, NodeInfo> activeNodeCache;

	public NodeBusinessService(PropertiesGeneralConfig propertiesGeneralConfig, IdentityService identityService) {
		this.identityService = identityService;
		this.activeNodeCache = Caffeine.newBuilder()
				.expireAfterWrite(propertiesGeneralConfig.getDeleteInactiveNodeAfterSeconds(), TimeUnit.SECONDS)
				.maximumSize(100_000)
				.build();
	}

	public NodePongDtoV1 handlePing(NodePingDtoV1 request) {
		Bytes pingInRlpBytes = RlpEncoderUtil.encodePingV1(request);
		Hash calculatedHash = Hash.hash(pingInRlpBytes);

		if (!calculatedHash.equals(Hash.fromHexString(request.getHash()))) {
			log.warn("Hash mismatch for incoming ping. Client: {}, Server: {}",
					request.getHash(), calculatedHash);
			throw new GEAuthenticationException("Hash mismatch. Client data inconsistent.");
		}

		Address nodeIdentity = Address.fromHexString(request.getNodeIdentity());
		BigInteger totalDifficulty = new BigInteger(request.getTotalDifficulty());
		Hash headHash = Hash.fromHexString(request.getHeadHash());
		Signature signature = Signature.wrap(Bytes.fromHexString(request.getSignature()));
		if (!signature.validate(calculatedHash, nodeIdentity)) {
			log.warn("Signature mismatch for incoming ping. Client: {}, Server: {}",
					request.getSignature(), signature);
			throw new GEAuthenticationException("Signature mismatch. Client data inconsistent.");
		}

		validateTimestamp(request.getTimestamp());
		validateVersion(request.getSoftwareVersion(), request.getHeadHeight());

		activeNodeCache.put(nodeIdentity,
				new NodeInfo(
						nodeIdentity,
						request.getP2pListenHost(),
						request.getP2pListenPort(),
						request.getNetwork(),
						request.getSoftwareVersion(),
						totalDifficulty,
						headHash,
						request.getHeadHeight(),
						Instant.now().getEpochSecond()));

		return buildPong(request.getNetwork());
	}

	private void validateTimestamp(long timestamp) {
		long now = Instant.now().getEpochSecond();
		long skew = Math.abs(now - timestamp);
		if (skew > MAX_TIMESTAMP_SKEW_SECONDS) {
			log.warn("Ping rejected due to timestamp skew. NodeTs: {}, ServerTs: {}, Skew: {}", timestamp, now, skew);
			throw new GEValidationException("Timestamp skew too large. Client data inconsistent.");
		}
	}

	private void validateVersion(String nodeVersion, long nodeHeight) {
		if (Constants.shouldNodeShutdown(nodeHeight, nodeVersion)) {
			log.warn("Node rejected due to version mismatch.");
			throw new GEAuthenticationException("Node tried to ping with software version code below minimum.");
		}
	}

	public NodePongDtoV1 buildPong(Network network) {
		Collection<NodeInfo> allNodes = List.copyOf(activeNodeCache.asMap().values()).stream()
				.filter(item -> network == null ? true : item.getNetwork().equals(network))
				.collect(Collectors.toList());
		NodePongPayloadDtoV1 pongPayload = new NodePongPayloadDtoV1(
				allNodes.stream()
						.map(node -> new NodeInfoDtoV1(
								node.nodeIdentity.toHexString(),
								node.p2pListenHost,
								node.p2pListenPort,
								node.network,
								node.softwareVersion,
								node.totalDifficulty.toString(),
								node.headHash.toHexString(),
								node.headHeight,
								node.updatedAt))
						.collect(Collectors.toList()),
				Instant.now().getEpochSecond());
		Bytes pongInRlpBytes = RlpEncoderUtil.encodePongV1(pongPayload);
		Hash calculatedPongHash = Hash.hash(pongInRlpBytes);
		Signature calculatedPongSignature = identityService.getPrivateKey().sign(calculatedPongHash);
		NodePongDtoV1 pong = new NodePongDtoV1(pongPayload, calculatedPongHash.toHexString(),
				calculatedPongSignature.toHexString());
		return pong;
	}

	@Data
	@AllArgsConstructor
	public static class NodeInfo {
		Address nodeIdentity;
		String p2pListenHost;
		Integer p2pListenPort;
		Network network;
		String softwareVersion;
		BigInteger totalDifficulty;
		Hash headHash;
		long headHeight;
		long updatedAt;
	}
}
