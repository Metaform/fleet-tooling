
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
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Tar;
import org.jspecify.annotations.NonNull;

import static com.metaformsystems.fleet.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.Constants.PLUGIN_EXTENSION_NAME;
import static com.metaformsystems.fleet.Constants.PLUGIN_PARAM_ARTIFACT_NAME;
import static com.metaformsystems.fleet.Constants.PLUGIN_PARAM_ARTIFACT_VERSION;
import static com.metaformsystems.fleet.Constants.PLUGIN_PARAM_SOURCE_DIR;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A plugin responsible for creating an OCI-compliant package from xRegistry source files.
 */
public class XRegistryOciPackagingPlugin implements Plugin<@NonNull Project> {
    public static final String BUILD_X_REGISTRY_TASK = "buildXRegistryOci";

    private static final String ARTFACT_SUFFIX = "-xregistry";

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create(PLUGIN_EXTENSION_NAME, XRegistryOciPackagingExtension.class);

        // create lazy providers for configuration values
        var sourceLocationProvider = createSourceLocationProvider(project, extension);
        var artifactNameProvider = createArtifactNameProvider(project);
        var versionProvider = createVersionProvider(project);

        createTasks(project, sourceLocationProvider, artifactNameProvider, versionProvider);
    }

    private Provider<String> createSourceLocationProvider(Project project, XRegistryOciPackagingExtension extension) {
        return project.provider(() -> {
            var customSourceDir = (String) project.findProperty(PLUGIN_PARAM_SOURCE_DIR);
            var sourceFile = project.getLayout().getProjectDirectory()
                    .file(requireNonNullElseGet(customSourceDir, () -> extension.getXRegistrySourceDir().get()))
                    .getAsFile();

            if (!sourceFile.exists()) {
                throw new GradleException(
                        "XRegistry source directory does not exist: " + sourceFile.getAbsolutePath() +
                        ". Please create the directory or specify a valid source directory using the '" +
                        PLUGIN_PARAM_SOURCE_DIR + "' property."
                );
            }

            if (!sourceFile.isDirectory()) {
                throw new GradleException(
                        "XRegistry source path is not a directory: " + sourceFile.getAbsolutePath() +
                        ". Please ensure the path points to a valid directory."
                );
            }

            return sourceFile.getAbsolutePath();
        });
    }

    private void createTasks(Project project,
                             Provider<String> sourceLocationProvider,
                             Provider<String> artifactNameProvider,
                             Provider<String> versionProvider) {

        var prepareFiles = project.getTasks()
                .register(PrepareAction.TASK_NAME, Copy.class, task -> {
                    new PrepareAction(project, sourceLocationProvider).execute(task);
                });

        var createLayer = project.getTasks()
                .register(CreateRegistryLayerAction.TASK_NAME, Tar.class, task -> {
                    new CreateRegistryLayerAction(project).execute(task);
                    task.dependsOn(prepareFiles);
                });

        var generateDigest = project.getTasks()
                .register(GenerateLayerDigestAction.TASK_NAME, task -> {
                    new GenerateLayerDigestAction(project).execute(task);
                    task.dependsOn(createLayer);
                });

        var createConfig = project.getTasks()
                .register(CreateConfigAction.TASK_NAME, task -> {
                    new CreateConfigAction(project, mapper).execute(task);
                    task.dependsOn(generateDigest);
                });

        var createManifest = project.getTasks()
                .register(CreateManifestAction.TASK_NAME, task -> {
                    new CreateManifestAction(project, artifactNameProvider, versionProvider, mapper).execute(task);
                    task.dependsOn(createConfig);
                });

        var createLayout = project.getTasks()
                .register(CreateLayoutAction.TASK_NAME, task -> {
                    new CreateLayoutAction(project, artifactNameProvider, versionProvider, mapper).execute(task);
                    task.dependsOn(createManifest);
                });

        var packageArtifact = project.getTasks()
                .register(PackageArtifactAction.TASK_NAME, Tar.class, task -> {
                    new PackageArtifactAction(project, artifactNameProvider, versionProvider).execute(task);
                    task.dependsOn(createLayout);
                });

        project.getTasks().register(BUILD_X_REGISTRY_TASK, task -> {
            task.setDescription("Builds an xRegistry as an OCI distribution artifact");
            task.setGroup(GRADLE_TASK_GROUP);
            task.dependsOn(packageArtifact);
        });
    }

    private Provider<String> createArtifactNameProvider(Project project) {
        return project.provider(() -> {
            var artifactName = (String) project.findProperty(PLUGIN_PARAM_ARTIFACT_NAME);
            if (artifactName == null) {
                artifactName = project.getName() + ARTFACT_SUFFIX;
            }
            return artifactName;
        });
    }

    private Provider<String> createVersionProvider(Project project) {
        return project.provider(() -> {
            var version = (String) project.findProperty(PLUGIN_PARAM_ARTIFACT_VERSION);
            if (version == null) {
                version = project.getVersion().toString();
            }
            return version;
        });
    }

}