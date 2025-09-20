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

/**
 * Plugin constants.
 */
public interface Constants {
    String GRADLE_TASK_GROUP = "oci";

    String PLUGIN_EXTENSION_NAME = "xRegistryOCIPackaging";

    String OCI_MEDIA_TYPE = "application/vnd.dspace.xregistry.layer.v1+json";
    String CONFIG_MEDIA_TYPE = "application/vnd.oci.image.config.v1+json";

    String XREGISTRY_SOURCE_DIR = "src" + File.separator + "main" + File.separator + "xregistry";
    String XREGISTRY_STAGING_DIR = "xregistry-staging";

    String OCI_LAYOUT_DIR = "oci-layout";
    String OCI_LAYOUT_FILE = "oci-layout";
    String OCI_INDEX_FILE = "index.json";
    String OCI_BLOBS_SHA_DIR = "blobs" + File.separator + "sha256";

    String OCI_LAYERS_DIR = "oci-layers";
    String XREGISTRY_LAYER_ARCHIVE = "xregistry-layer.tar";
    String XREGISTRY_LAYER_ARCHIVE_PATH = OCI_LAYERS_DIR + File.separator + "xregistry-layer.tar";
    String XREGISTRY_LAYER_SHA_PATH = OCI_LAYERS_DIR + File.separator + "xregistry-layer.sha256";

    String LAYER_DIGEST_PROPERTY = "layerDigest";
    String CONFIG_DIGEST_PROPERTY = "configDigest";
    String MANIFEST_DIGEST_PROPERTY = "manifestDigest";
    String CONFIG_SIZE_PROPERTY = "configSize";
    String LAYER_SIZE_PROPERTY = "layerSize";

    String OCI_CONFIG_DIR = "oci-config";
    String OCI_CONFIG = "config.json";
    String OCI_CONFIG_PATH = OCI_CONFIG_DIR + File.separator + OCI_CONFIG;

    String OCI_MANIFEST_DIR = "oci-manifest";
    String OCI_MANIFEST_PATH = OCI_MANIFEST_DIR + File.separator + "manifest.json";

    String[] ARTIFACT_EXTENSIONS = new String[]{ "**/*.json", "**/*.yaml", "**/*.yml" };

    String SHA_PREFIX = "sha256:";

    String PLUGIN_PARAM_SOURCE_DIR = "xRegistrySourceDir";
    String PLUGIN_PARAM_ARTIFACT_NAME = "ociArtifactName";
    String PLUGIN_PARAM_ARTIFACT_VERSION = "ociArtifactVersion";
}