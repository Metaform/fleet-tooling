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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ShaUtilsTest {

    @Test
    void testGenerateSha256WithInputStream_emptyContent() {
        var emptyStream = new ByteArrayInputStream(new byte[0]);
        var hash = ShaUtils.generateSha256(emptyStream);
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void testGenerateSha256WithInputStream_knownContent() {
        var content = "hello world";
        var stream = new ByteArrayInputStream(content.getBytes());
        var hash = ShaUtils.generateSha256(stream);
        assertThat(hash).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }

}