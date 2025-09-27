
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.metaformsystems.fleet.xregistry.oci.Constants.CONFIG_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.xregistry.oci.Constants.LAYER_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.MANIFEST_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_BLOBS_SHA_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_CONFIG_PATH;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_INDEX_FILE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYOUT_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYOUT_FILE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_MANIFEST_PATH;
import static com.metaformsystems.fleet.xregistry.oci.Constants.SHA_PREFIX;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_LAYER_ARCHIVE_PATH;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Creates the OCI image layout and index files.
 */
public class CreateLayoutAction implements Action<Task> {
    public static final String TASK_NAME = "createOciLayout";

    private static final String DESCRIPTION = "Creates OCI image layout";

    private Project project;
    private Provider<String> artifactNameProvider;
    private Provider<String> versionProvider;
    private ObjectMapper mapper;

    public CreateLayoutAction(Project project,
                              Provider<String> artifactNameProvider,
                              Provider<String> versionProvider,
                              ObjectMapper mapper) {
        this.project = project;
        this.artifactNameProvider = artifactNameProvider;
        this.versionProvider = versionProvider;
        this.mapper = mapper;
    }

    @Override
    public void execute(Task task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        var layoutDir = project.getLayout().getBuildDirectory().dir(OCI_LAYOUT_DIR);
        task.getOutputs().dir(layoutDir);

        task.doLast(t -> {
            try {
                var layoutDirFile = layoutDir.get().getAsFile();
                layoutDirFile.mkdirs();

                var layerDigest = (String) project.getExtensions().getExtraProperties().get(LAYER_DIGEST_PROPERTY);
                var configDigest = (String) project.getExtensions().getExtraProperties().get(CONFIG_DIGEST_PROPERTY);
                var manifestDigest = (String) project.getExtensions().getExtraProperties().get(MANIFEST_DIGEST_PROPERTY);

                createOciLayoutFile(layoutDirFile);
                createIndexFile(layoutDirFile, manifestDigest, artifactNameProvider.get(), versionProvider.get());
                createBlobsStructure(layoutDirFile, layerDigest, configDigest, manifestDigest);

            } catch (Exception e) {
                throw new GradleException("Failed to create OCI layout", e);
            }
        });
    }

    private void createOciLayoutFile(File layoutDir) throws Exception {
        var layoutFile = new File(layoutDir, OCI_LAYOUT_FILE);
        var layout = new LinkedHashMap<String, Object>();
        layout.put("imageLayoutVersion", "1.0.0");
        write(layoutFile.toPath(), mapper.writeValueAsBytes(layout));
    }

    private void createIndexFile(File layoutDir, String manifestDigest, String artifactName, String version) throws Exception {
        var indexFile = new File(layoutDir, OCI_INDEX_FILE);
        var index = new LinkedHashMap<String, Object>();
        index.put("schemaVersion", 2);

        var manifests = new ArrayList<>();
        var manifestDescriptor = new LinkedHashMap<String, Object>();
        manifestDescriptor.put("mediaType", "application/vnd.oci.image.manifest.v1+json");
        manifestDescriptor.put("digest", manifestDigest);

        var annotations = new LinkedHashMap<String, Object>();
        annotations.put("org.opencontainers.image.ref.name", artifactName + ":" + version);
        manifestDescriptor.put("annotations", annotations);

        manifests.add(manifestDescriptor);
        index.put("manifests", manifests);

        write(indexFile.toPath(), mapper.writeValueAsBytes(index));
    }

    private void createBlobsStructure(File layoutDir, String layerDigest, String configDigest, String manifestDigest) throws Exception {
        var blobsDir = new File(layoutDir, OCI_BLOBS_SHA_DIR);
        blobsDir.mkdirs();

        // copy layer blob
        var layerFile = project.getLayout().getBuildDirectory().file(XREGISTRY_LAYER_ARCHIVE_PATH).get().getAsFile();
        var layerBlobFile = new File(blobsDir, layerDigest.replace(SHA_PREFIX, ""));
        copy(layerFile.toPath(), layerBlobFile.toPath(), REPLACE_EXISTING);

        // copy config blob
        var configFile = project.getLayout().getBuildDirectory().file(OCI_CONFIG_PATH).get().getAsFile();
        var configBlobFile = new File(blobsDir, configDigest.replace(SHA_PREFIX, ""));
        copy(configFile.toPath(), configBlobFile.toPath(), REPLACE_EXISTING);

        // copy manifest blob
        var manifestFile = project.getLayout().getBuildDirectory().file(OCI_MANIFEST_PATH).get().getAsFile();
        var manifestBlobFile = new File(blobsDir, manifestDigest.replace(SHA_PREFIX, ""));
        copy(manifestFile.toPath(), manifestBlobFile.toPath(), REPLACE_EXISTING);
    }
}