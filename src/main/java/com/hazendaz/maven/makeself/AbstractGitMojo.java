/*
 *    Copyright 2011-2026 the original author or authors.
 *
 *    This program is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU General Public License
 *    as published by the Free Software Foundation; either version 2
 *    of the License, or (at your option) any later version.
 *
 *    You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 */
package com.hazendaz.maven.makeself;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Abstract base class providing portable Git download and installation support for Windows.
 */
public abstract class AbstractGitMojo extends AbstractMojo {

    /** isWindows is detected at start of plugin to ensure windows needs. */
    static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    /** The Constant GIT_USER_BIN. */
    static final String GIT_USER_BIN = "/usr/bin/";

    /**
     * The path to existing git install for windows usage. If left blank per default, portable git will be used.
     * Location should be something like 'C:/Program Files/Git'. When set and not windows, it will be treated as blank.
     */
    @Parameter(defaultValue = "", property = "gitPath")
    protected String gitPath;

    /** Maven Artifact Factory. */
    @Inject
    protected RepositorySystem repositorySystem;

    /** Maven Repository System Session. */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    protected RepositorySystemSession repoSession;

    /** Maven Remote Repositories. */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> remoteRepositories;

    /** Portable Git. */
    protected PortableGit portableGit;

    /**
     * Check Git Setup.
     *
     * @throws MojoFailureException
     *             the mojo failure exception
     */
    protected void checkGitSetup() throws MojoFailureException {
        // Get Portable Git Maven Information
        this.portableGit = new PortableGit(this.getLog());

        // Extract Portable Git
        this.extractPortableGit();
    }

    /**
     * Extract Portable Git.
     *
     * @throws MojoFailureException
     *             failure retrieving portable git
     */
    protected void extractPortableGit() throws MojoFailureException {
        final String location = this.repoSession.getLocalRepository().getBasedir() + File.separator
                + this.portableGit.getName() + File.separator + this.portableGit.getVersion();
        if (Files.exists(Path.of(location))) {
            this.getLog().debug("Existing 'PortableGit' folder found at " + location);
            this.gitPath = location + AbstractGitMojo.GIT_USER_BIN;
            return;
        }

        this.getLog().info("Loading portable git");
        final Artifact artifact = new DefaultArtifact(this.portableGit.getGroupId(), this.portableGit.getArtifactId(),
                this.portableGit.getClassifier(), this.portableGit.getExtension(), this.portableGit.getVersion());
        final ArtifactRequest artifactRequest = new ArtifactRequest().setRepositories(this.remoteRepositories)
                .setArtifact(artifact);
        ArtifactResult resolutionResult = null;
        try {
            resolutionResult = this.repositorySystem.resolveArtifact(this.repoSession, artifactRequest);
            if (!resolutionResult.isResolved()) {
                throw new MojoFailureException("Unable to resolve artifact: " + artifact.getGroupId() + ":"
                        + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getClassifier() + ":"
                        + artifact.getExtension());
            }
        } catch (final ArtifactResolutionException e) {
            throw new MojoFailureException(
                    "Unable to resolve artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                            + artifact.getVersion() + ":" + artifact.getClassifier() + ":" + artifact.getExtension());
        }
        this.installGit(resolutionResult.getArtifact(), location);
    }

    /**
     * Install Git extracts git to .m2/repository under PortableGit.
     *
     * @param artifact
     *            the maven artifact representation for git
     * @param location
     *            the location in maven repository to store portable git
     */
    protected void installGit(final Artifact artifact, final String location) {
        Path currentFile = null;

        // Unzip 'git-for-windows-*-portable.tar.gz' from '.m2/repository/com/github/hazendaz/git/git-for-windows'
        // into '.m2/repository/PortableGit'
        try (InputStream inputStream = Files.newInputStream(artifact.getFile().toPath());
                InputStream bufferedStream = new BufferedInputStream(inputStream);
                InputStream gzipStream = new GzipCompressorInputStream(bufferedStream);
                ArchiveInputStream<TarArchiveEntry> tarStream = new TarArchiveInputStream(gzipStream)) {
            ArchiveEntry entry;
            final String directory = this.repoSession.getLocalRepository().getBasedir() + File.separator
                    + this.portableGit.getName();
            while ((entry = tarStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                currentFile = Path.of(directory, entry.getName());
                if (!currentFile.normalize().startsWith(directory)) {
                    throw new IOException("Bad zip entry, possible directory traversal");
                }
                final Path parent = currentFile.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                this.getLog().debug("Current file: " + currentFile.getFileName());
                Files.copy(tarStream, currentFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException e) {
            this.getLog().error("", e);
        }

        try {
            if (currentFile != null) {
                // Extract Portable Git
                this.getLog().debug("Extract Portable Git");
                this.runInstaller(Arrays.asList(currentFile.toString(), "-y", "-o", location));
                this.gitPath = location + AbstractGitMojo.GIT_USER_BIN;
            }
        } catch (final IOException e) {
            this.getLog().error("", e);
        } catch (final InterruptedException e) {
            this.getLog().error("", e);
            // restore interruption status of the corresponding thread
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run installer executes a process and logs its output.
     *
     * @param command
     *            the command to run
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             the interrupted exception
     */
    protected void runInstaller(final List<String> command) throws IOException, InterruptedException {
        this.getLog().debug("Execution commands: " + command);

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                this.getLog().info(line);
            }
            this.getLog().info("");
        }

        final int status = process.waitFor();
        if (status > 0) {
            this.getLog().error(String.join(" ", "Process failed with error status:", String.valueOf(status)));
        }
    }

}
