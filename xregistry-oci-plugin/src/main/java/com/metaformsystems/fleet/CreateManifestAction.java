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

package com.metaformsystems.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.metaformsystems.fleet.Constants.CONFIG_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.CONFIG_MEDIA_TYPE;
import static com.metaformsystems.fleet.Constants.CONFIG_SIZE_PROPERTY;
import static com.metaformsystems.fleet.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.Constants.LAYER_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.LAYER_SIZE_PROPERTY;
import static com.metaformsystems.fleet.Constants.MANIFEST_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.OCI_MANIFEST_PATH;
import static com.metaformsystems.fleet.Constants.OCI_MEDIA_TYPE;
import static com.metaformsystems.fleet.Constants.SHA_PREFIX;
import static com.metaformsystems.fleet.ShaUtils.generateSha256;
import static java.nio.file.Files.write;

/**
 * Creates the OCI manifest file.
 */
public class CreateManifestAction implements Action<Task> {
    public static final String TASK_NAME = "createOciManifest";

    private static final String DESCRIPTION = "Creates OCI image manifest";

    private Project project;
    private Provider<String> artifactNameProvider;
    private Provider<String> versionProvider;
    private ObjectMapper mapper;

    public CreateManifestAction(Project project,
                                Provider<String> artifactNameProvider,
                                Provider<String> versionProvider,
                                ObjectMapper mapper) {
        this.project = project;
        this.artifactNameProvider = artifactNameProvider;
        this.versionProvider = versionProvider;
        this.mapper = mapper;
    }

    // Backward compatibility constructor
    public CreateManifestAction(Project project, String artifactName, String version, ObjectMapper mapper) {
        this.project = project;
        this.artifactNameProvider = project.provider(() -> artifactName);
        this.versionProvider = project.provider(() -> version);
        this.mapper = mapper;
    }

    @Override
    public void execute(Task task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        var manifestFile = project.getLayout().getBuildDirectory().file(OCI_MANIFEST_PATH);
        task.getOutputs().file(manifestFile);

        task.doLast(t -> {
            try {
                manifestFile.get().getAsFile().getParentFile().mkdirs();

                var layerDigest = (String) project.getExtensions().getExtraProperties().get(LAYER_DIGEST_PROPERTY);
                var layerSize = (Long) project.getExtensions().getExtraProperties().get(LAYER_SIZE_PROPERTY);
                var configDigest = (String) project.getExtensions().getExtraProperties().get(CONFIG_DIGEST_PROPERTY);
                var configSize = (Long) project.getExtensions().getExtraProperties().get(CONFIG_SIZE_PROPERTY);

                var manifest = createOciManifest(layerDigest, layerSize, configDigest, configSize, artifactNameProvider.get(), versionProvider.get());

                write(manifestFile.get().getAsFile().toPath(), mapper.writeValueAsBytes(manifest));

                var digest = generateSha256(manifestFile.get().getAsFile());
                project.getExtensions().getExtraProperties().set(MANIFEST_DIGEST_PROPERTY, SHA_PREFIX + digest);

            } catch (Exception e) {
                throw new GradleException("Failed to create manifest", e);
            }
        });
    }

    /**
     * Creates OCI manifest structure
     */
    public Map<String, Object> createOciManifest(String layerDigest, Long layerSize,
                                                 String configDigest, Long configSize,
                                                 String artifactName, String version) {
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put("schemaVersion", 2);
        manifest.put("mediaType", "application/vnd.oci.image.manifest.v1+json");

        // config descriptor
        var config = new LinkedHashMap<String, Object>();
        config.put("mediaType", CONFIG_MEDIA_TYPE);
        config.put("digest", configDigest);
        config.put("size", configSize);
        manifest.put("config", config);

        // layer descriptors
        var layers = new ArrayList<>();
        var layer = new LinkedHashMap<String, Object>();
        layer.put("mediaType", OCI_MEDIA_TYPE);
        layer.put("digest", layerDigest);
        layer.put("size", layerSize);
        layers.add(layer);
        manifest.put("layers", layers);

        // annotations
        var annotations = new LinkedHashMap<String, Object>();
        annotations.put("org.opencontainers.image.title", artifactName);
        annotations.put("org.opencontainers.image.version", version);
        manifest.put("annotations", annotations);

        return manifest;
    }
}