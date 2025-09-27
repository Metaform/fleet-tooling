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

import org.gradle.api.provider.Property;
import org.jspecify.annotations.NonNull;

import static com.metaformsystems.fleet.xregistry.oci.Constants.CONFIG_MEDIA_TYPE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_MEDIA_TYPE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_SOURCE_DIR;

/**
 * Gradle extension for the XRegistry OCI packaging plugin.
 */
@Deprecated
public abstract class XRegistryOciPackagingExtension {

    public abstract Property<@NonNull String> getXRegistrySourceDir();

    public abstract Property<@NonNull String> getOciArtifactName();

    public abstract Property<@NonNull String> getOciArtifactVersion();

    public abstract Property<@NonNull String> getOciRegistry();

    public abstract Property<@NonNull String> getOciMediaType();

    public abstract Property<@NonNull String> getOciConfigMediaType();

    public XRegistryOciPackagingExtension() {
        getXRegistrySourceDir().convention(XREGISTRY_SOURCE_DIR);
        getOciMediaType().convention(OCI_MEDIA_TYPE);
        getOciConfigMediaType().convention(CONFIG_MEDIA_TYPE);
    }
}