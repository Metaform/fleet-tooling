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

import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.metaformsystems.fleet.xregistry.oci.Constants.CONFIG_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.CONFIG_MEDIA_TYPE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.LAYER_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.MANIFEST_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_BLOBS_SHA_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_CONFIG;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_CONFIG_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_INDEX_FILE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYERS_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYOUT_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYOUT_FILE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_MANIFEST_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_MANIFEST_PATH;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_MEDIA_TYPE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.PLUGIN_EXTENSION_NAME;
import static com.metaformsystems.fleet.xregistry.oci.Constants.PLUGIN_PARAM_ARTIFACT_NAME;
import static com.metaformsystems.fleet.xregistry.oci.Constants.PLUGIN_PARAM_ARTIFACT_VERSION;
import static com.metaformsystems.fleet.xregistry.oci.Constants.PLUGIN_PARAM_SOURCE_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_LAYER_ARCHIVE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_LAYER_ARCHIVE_PATH;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_LAYER_SHA_PATH;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_SOURCE_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_STAGING_DIR;
import static com.metaformsystems.fleet.xregistry.oci.XRegistryOciPackagingPlugin.BUILD_X_REGISTRY_TASK;
import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.assertThat;

class XRegistryOciPackagingPluginTest {
    private static final String PLUGIN_ID = "com.metaformsystems.xregistry-oci-packaging";
    private static final String GRADLE_DISTRIBUTIONS = "distributions";

    private Project project;

    @TempDir
    Path tempDir;

    @Test
    void testPluginAppliesSuccessfully() {
        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getPluginManager().hasPlugin(PLUGIN_ID)).isTrue();
    }

    @Test
    void testPluginCreatesExtension() {
        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getExtensions().findByName(PLUGIN_EXTENSION_NAME))
                .isNotNull()
                .isInstanceOf(XRegistryOciPackagingExtension.class);
    }

    @Test
    void testExtensionHasDefaultValues() {
        project.getPluginManager().apply(PLUGIN_ID);
        var extension = (XRegistryOciPackagingExtension) project.getExtensions().findByName(PLUGIN_EXTENSION_NAME);

        //noinspection DataFlowIssue
        assertThat(extension.getXRegistrySourceDir().get()).isEqualTo(XREGISTRY_SOURCE_DIR);
        assertThat(extension.getOciMediaType().get()).isEqualTo(OCI_MEDIA_TYPE);
        assertThat(extension.getOciConfigMediaType().get()).isEqualTo(CONFIG_MEDIA_TYPE);
    }

    @Test
    void testPluginCreatesAllTasks() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getTasks().findByName(PrepareAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(CreateRegistryLayerAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(GenerateLayerDigestAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(CreateConfigAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(CreateManifestAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(CreateLayoutAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(PackageArtifactAction.TASK_NAME)).isNotNull();
        assertThat(project.getTasks().findByName(BUILD_X_REGISTRY_TASK)).isNotNull();
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testTaskDependencies() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var createLayer = project.getTasks().findByName(CreateRegistryLayerAction.TASK_NAME);
        var generateDigest = project.getTasks().findByName(GenerateLayerDigestAction.TASK_NAME);
        var createConfig = project.getTasks().findByName(CreateConfigAction.TASK_NAME);
        var createManifest = project.getTasks().findByName(CreateManifestAction.TASK_NAME);
        var createLayout = project.getTasks().findByName(CreateLayoutAction.TASK_NAME);
        var packageArtifact = project.getTasks().findByName(PackageArtifactAction.TASK_NAME);
        var buildOci = project.getTasks().findByName(BUILD_X_REGISTRY_TASK);

        // Verify the dependency chain by checking task dependencies
        assertThat(createLayer.getTaskDependencies().getDependencies(createLayer))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(PrepareAction.TASK_NAME));
        assertThat(generateDigest.getTaskDependencies().getDependencies(generateDigest))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(CreateRegistryLayerAction.TASK_NAME));
        assertThat(createConfig.getTaskDependencies().getDependencies(createConfig))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(GenerateLayerDigestAction.TASK_NAME));
        assertThat(createManifest.getTaskDependencies().getDependencies(createManifest))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(CreateConfigAction.TASK_NAME));
        assertThat(createLayout.getTaskDependencies().getDependencies(createLayout))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(CreateManifestAction.TASK_NAME));
        assertThat(packageArtifact.getTaskDependencies().getDependencies(packageArtifact))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(CreateLayoutAction.TASK_NAME));
        assertThat(buildOci.getTaskDependencies().getDependencies(buildOci))
                .anySatisfy(task -> assertThat(task.getName()).isEqualTo(PackageArtifactAction.TASK_NAME));
    }

    @Test
    void testPluginUsesProjectProperties() throws IOException {
        var customSourceDir = new File(project.getProjectDir(), "custom/source");
        customSourceDir.mkdirs();
        write(new File(customSourceDir, "test.json").toPath(), "{}".getBytes());

        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_SOURCE_DIR, "custom" + File.separator + "source");
        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_ARTIFACT_NAME, "custom-name");
        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_ARTIFACT_VERSION, "2.0");

        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getPluginManager().hasPlugin(PLUGIN_ID)).isTrue();
    }

    @Test
    void testPluginUsesDefaultsWhenPropertiesNotSet() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getPluginManager().hasPlugin(PLUGIN_ID)).isTrue();
    }

    @Test
    void testPrepareXRegistryFilesTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());
        write(new File(sourceDir, "test.yaml").toPath(), "key: value".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var prepareTask = (Copy) project.getTasks().findByName(PrepareAction.TASK_NAME);

        assertThat(prepareTask).isNotNull();
        assertThat(prepareTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testCreateXRegistryLayerTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var tarTask = (Tar) project.getTasks().findByName(CreateRegistryLayerAction.TASK_NAME);

        assertThat(tarTask).isNotNull();
        assertThat(tarTask.getArchiveFileName().get()).isEqualTo(XREGISTRY_LAYER_ARCHIVE);
        assertThat(tarTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testPackageOciArtifactTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var packageTask = (Tar) project.getTasks().findByName(PackageArtifactAction.TASK_NAME);

        assertThat(packageTask).isNotNull();
        assertThat(packageTask.getArchiveFileName().get()).endsWith(".tar");
        assertThat(packageTask.getGroup()).isEqualTo("oci");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testPluginWithCustomProjectName() throws IOException {
        var customProject = ProjectBuilder.builder()
                .withName("custom-project")
                .withProjectDir(tempDir.resolve("custom").toFile())
                .build();

        var sourceDir = new File(customProject.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        customProject.getPluginManager().apply(PLUGIN_ID);

        assertThat(customProject.getPluginManager().hasPlugin(PLUGIN_ID)).isTrue();

        // verify the default artifact name includes the custom project name
        var packageTask = (Tar) customProject.getTasks().findByName(PackageArtifactAction.TASK_NAME);
        assertThat(packageTask.getArchiveFileName().get()).startsWith("custom-project-xregistry");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testPluginWithCustomVersion() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.setVersion("3.0.0");
        project.getPluginManager().apply(PLUGIN_ID);

        assertThat(project.getPluginManager().hasPlugin(PLUGIN_ID)).isTrue();

        // verify that the custom version is used in the artifact name
        var packageTask = (Tar) project.getTasks().findByName(PackageArtifactAction.TASK_NAME);
        assertThat(packageTask.getArchiveFileName().get()).contains("3.0.0");
    }

    @Test
    void testGenerateLayerDigestTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var digestTask = project.getTasks().findByName(GenerateLayerDigestAction.TASK_NAME);

        assertThat(digestTask).isNotNull();
        assertThat(digestTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testCreateConfigTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var configTask = project.getTasks().findByName(CreateConfigAction.TASK_NAME);

        assertThat(configTask).isNotNull();
        assertThat(configTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testCreateManifestTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var manifestTask = project.getTasks().findByName(CreateManifestAction.TASK_NAME);

        assertThat(manifestTask).isNotNull();
        assertThat(manifestTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testCreateLayoutTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var layoutTask = project.getTasks().findByName(CreateLayoutAction.TASK_NAME);

        assertThat(layoutTask).isNotNull();
        assertThat(layoutTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testBuildXRegistryOciTaskConfiguration() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        var buildTask = project.getTasks().findByName(BUILD_X_REGISTRY_TASK);

        assertThat(buildTask).isNotNull();
        assertThat(buildTask.getGroup()).isEqualTo("oci");
    }

    @Test
    void testExecuteAllPluginTasks() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test-schema.json").toPath(),
                "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}".getBytes());

        project.setVersion("1.0.0");
        project.getPluginManager().apply(PLUGIN_ID);

        var buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
        new File(buildDir, OCI_LAYERS_DIR).mkdirs();
        var configDir = new File(buildDir, OCI_CONFIG_DIR);
        configDir.mkdirs();

        new File(buildDir, OCI_MANIFEST_DIR).mkdirs();
        new File(buildDir, GRADLE_DISTRIBUTIONS).mkdirs();

        // execute tasks in the correct dependency order
        var prepareFiles = (Copy) project.getTasks().findByName(PrepareAction.TASK_NAME);
        var createLayer = (Tar) project.getTasks().findByName(CreateRegistryLayerAction.TASK_NAME);
        var generateDigest = project.getTasks().findByName(GenerateLayerDigestAction.TASK_NAME);
        var createConfig = project.getTasks().findByName(CreateConfigAction.TASK_NAME);
        var createManifest = project.getTasks().findByName(CreateManifestAction.TASK_NAME);
        var createLayout = project.getTasks().findByName(CreateLayoutAction.TASK_NAME);
        var packageArtifact = (Tar) project.getTasks().findByName(PackageArtifactAction.TASK_NAME);
        var buildTask = project.getTasks().findByName(BUILD_X_REGISTRY_TASK);

        assertThat(prepareFiles).isNotNull();
        assertThat(createLayer).isNotNull();
        assertThat(generateDigest).isNotNull();
        assertThat(createConfig).isNotNull();
        assertThat(createManifest).isNotNull();
        assertThat(createLayout).isNotNull();
        assertThat(packageArtifact).isNotNull();
        assertThat(buildTask).isNotNull();

        prepareFiles.getActions().forEach(action -> action.execute(prepareFiles));

        createLayer.getActions().forEach(action -> action.execute(createLayer));

        generateDigest.getActions().forEach(action -> action.execute(generateDigest));
        createConfig.getActions().forEach(action -> action.execute(createConfig));
        createManifest.getActions().forEach(action -> action.execute(createManifest));
        createLayout.getActions().forEach(action -> action.execute(createLayout));
        packageArtifact.getActions().forEach(action -> action.execute(packageArtifact));
        buildTask.getActions().forEach(action -> action.execute(buildTask));

        var stagingDir = new File(buildDir, XREGISTRY_STAGING_DIR);
        assertThat(stagingDir).exists();
        assertThat(new File(stagingDir, "test-schema.json")).exists();

        assertThat(new File(buildDir, XREGISTRY_LAYER_ARCHIVE_PATH)).exists();
        assertThat(new File(buildDir, XREGISTRY_LAYER_SHA_PATH)).exists();

        assertThat(new File(configDir, OCI_CONFIG)).exists();

        assertThat(new File(buildDir, OCI_MANIFEST_PATH)).exists();

        var layoutDir = new File(buildDir, OCI_LAYOUT_DIR);
        assertThat(layoutDir).exists();
        assertThat(new File(layoutDir, OCI_LAYOUT_FILE)).exists();
        assertThat(new File(layoutDir, OCI_INDEX_FILE)).exists();
        assertThat(new File(layoutDir, OCI_BLOBS_SHA_DIR)).exists();

        assertThat(new File(buildDir, GRADLE_DISTRIBUTIONS)).exists();

        var distributionFiles = new File(buildDir, GRADLE_DISTRIBUTIONS).listFiles();
        assertThat(distributionFiles).isNotNull().isNotEmpty();

        // verify project properties were set during execution
        assertThat(project.getExtensions().getExtraProperties().has(LAYER_DIGEST_PROPERTY)).isTrue();
        assertThat(project.getExtensions().getExtraProperties().has(CONFIG_DIGEST_PROPERTY)).isTrue();
        assertThat(project.getExtensions().getExtraProperties().has(MANIFEST_DIGEST_PROPERTY)).isTrue();
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testPluginWithCustomProperties() throws IOException {
        var customSourceDir = new File(project.getProjectDir(), "custom/source");
        customSourceDir.mkdirs();
        write(new File(customSourceDir, "test.json").toPath(), "{}".getBytes());

        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_SOURCE_DIR, "custom" + File.separator + "source");
        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_ARTIFACT_NAME, "my-custom-artifact");
        project.getExtensions().getExtraProperties().set(PLUGIN_PARAM_ARTIFACT_VERSION, "2.1.0");

        project.getPluginManager().apply(PLUGIN_ID);

        var packageTask = (Tar) project.getTasks().findByName(PackageArtifactAction.TASK_NAME);

        assertThat(packageTask.getArchiveFileName().get()).isEqualTo("my-custom-artifact-2.1.0.tar");
    }

    @Test
    void testTasksHaveCorrectGroups() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        // Verify all tasks are in the correct group
        var allTasks = project.getTasks().matching(task ->
                task.getName().startsWith("prepare") ||
                task.getName().startsWith("create") ||
                task.getName().startsWith("generate") ||
                task.getName().startsWith("package") ||
                task.getName().equals(BUILD_X_REGISTRY_TASK)
        );

        allTasks.forEach(task -> assertThat(task.getGroup()).isEqualTo("oci"));
    }

    @Test
    void testTasksAreConfiguredLazily() throws IOException {
        var sourceDir = new File(project.getProjectDir(), XREGISTRY_SOURCE_DIR);
        sourceDir.mkdirs();
        write(new File(sourceDir, "test.json").toPath(), "{}".getBytes());

        project.getPluginManager().apply(PLUGIN_ID);

        // tasks should exist but not be fully configured until accessed
        assertThat(project.getTasks().getNames()).contains(
                PrepareAction.TASK_NAME,
                CreateRegistryLayerAction.TASK_NAME,
                GenerateLayerDigestAction.TASK_NAME,
                CreateConfigAction.TASK_NAME,
                CreateManifestAction.TASK_NAME,
                CreateLayoutAction.TASK_NAME,
                PackageArtifactAction.TASK_NAME,
                BUILD_X_REGISTRY_TASK
        );
    }

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
    }


}