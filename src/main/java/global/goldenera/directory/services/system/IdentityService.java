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
package global.goldenera.directory.services.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import global.goldenera.cryptoj.datatypes.Address;
import global.goldenera.cryptoj.datatypes.PrivateKey;
import global.goldenera.directory.exceptions.GEFailedException;
import global.goldenera.directory.properties.PropertiesGeneralConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {

	static final String DEFAULT_IDENTITY_FILE = ".directory_identity";

	PrivateKey privateKey;
	Address nodeIdentityAddress;

	public IdentityService(PropertiesGeneralConfig generalProperties) {
		try {
			String configuredIdentityFile = generalProperties.getIdentityFile();
			if (configuredIdentityFile == null || configuredIdentityFile.trim().isBlank()) {
				configuredIdentityFile = DEFAULT_IDENTITY_FILE;
			}
			Path identityPath = Paths.get(configuredIdentityFile);
			String tempMnemonic;

			if (Files.exists(identityPath)) {
				log.info("Loading existing directory identity from: {}", identityPath.toAbsolutePath());
				tempMnemonic = Files.readString(identityPath).trim();
			} else {
				log.info("No identity found. Generating new directory identity...");
				tempMnemonic = PrivateKey.generateMnemonic();
				Path parentDir = identityPath.getParent();
				if (parentDir != null) {
					Files.createDirectories(parentDir);
				}
				Files.writeString(identityPath, tempMnemonic);
				log.info("New directory identity saved to: {}", identityPath.toAbsolutePath());
			}

			this.privateKey = PrivateKey.load(tempMnemonic, null);
			this.nodeIdentityAddress = this.privateKey.getAddress();
			log.info("DirectoryIdentity Address: {}", this.nodeIdentityAddress);
		} catch (IOException e) {
			log.error("Failed to access identity file", e);
			throw new GEFailedException("I/O error handling directory identity: " + e.getMessage());
		} catch (Exception e) {
			log.error("Crypto error during identity initialization", e);
			throw new GEFailedException("Failed to initialize directory identity: " + e.getMessage());
		}
	}
}