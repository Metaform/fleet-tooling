/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package com.metaformsystems.fleet.xregistry.oci;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

/**
 * SHA-256 hash utilities.
 */
public class ShaUtils {

    /**
     * Generates a SHA256 hash for the given file.
     */
    public static String generateSha256(File file) {
        try (var stream = new FileInputStream(file)) {
            return generateSha256(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a SHA256 hash for the given stream.
     */
    public static String generateSha256(InputStream stream) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            var hexString = new StringBuilder();
            for (var b : digest.digest()) {
                hexString.append(format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to generate SHA256 hash", e);
        }
    }

}
