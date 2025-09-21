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

package com.metaformsystems.fleet.xregistry.processor;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.exists;

/**
 * Walks a file-system-based xRegistry that is in compact format.
 * <p>
 * Compact format is a directory structure with files in the format "group.resource-name.version.extension".
 * The following resource types are supported:
 * <ul>
 * <li>POLICY - under /policies</li>
 * <li>SCHEMA - under /schemas</li>
 * <li>RULE - under /rules</li>
 * </ul>
 */
public class CompactFileSystemWalker extends AbstractFileSystemWalker {
    private static final int TOKEN_COUNT = 4;  // group, resource name, version, extension
    private static final int GROUP = 0;
    private static final int RESOURCE_NAME = 1;

    public CompactFileSystemWalker(XRegistryVisitor visitor) {
        super(visitor);
    }

    protected void processPath(ArtifactType type, Path rootPath) {
        var resourcePath = rootPath.resolve(type.resourcesName());
        if (exists(resourcePath)) {
            try (var paths = Files.list(resourcePath)) {
                paths.filter(Files::isRegularFile)
                        .forEach(filePath -> processFile(type, filePath));
            } catch (IOException e) {
                visitor.onError(resourcePath.toString());
            }
        }
    }

    private void processFile(ArtifactType type, Path filePath) {
        var artifact = parseFilename(filePath.getFileName().toString());
        if (artifact == null) {
            return;
        }
        processFile(type, artifact, filePath);
    }

    /**
     * Parse a filename in the format "group.resource-name.version.extension" and return the artifact.
     *
     * @param filename the filename to parse
     * @return Artifact or null if format is invalid
     */
    @Nullable
    public Artifact parseFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }

        var tokens = filename.split("\\.");
        if (tokens.length < TOKEN_COUNT) {
            return null; // Need at least group.resource.version.extension
        }

        var group = tokens[GROUP];
        var resourceName = tokens[RESOURCE_NAME];

        // everything from third token to second-to-last token is the version
        var version = new StringBuilder();
        for (var i = 2; i < tokens.length - 1; i++) {
            if (i > 2) {
                version.append(".");
            }
            version.append(tokens[i]);
        }
        return new Artifact(group, resourceName, version.toString());
    }

}
