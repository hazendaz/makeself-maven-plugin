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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AbstractGitMojo}.
 */
@ExtendWith(MockitoExtension.class)
class AbstractGitMojoTest {

    /** Temporary directory for test files. */
    @TempDir
    Path tempDir;

    /** Mock Maven log. */
    @Mock
    private Log log;

    /** Mock repository system session. */
    @Mock
    private RepositorySystemSession repoSession;

    /** Mock repository system. */
    @Mock
    private RepositorySystem repositorySystem;

    /**
     * Sets a private field on the given object via reflection, walking up the class hierarchy as needed.
     *
     * @param obj
     *            the object
     * @param fieldName
     *            the field name
     * @param value
     *            the value to set
     *
     * @throws Exception
     *             if the field cannot be found or set
     */
    private static void setField(final Object obj, final String fieldName, final Object value) throws Exception {
        final Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * Gets a field value from the given object via reflection, walking up the class hierarchy as needed.
     *
     * @param obj
     *            the object
     * @param fieldName
     *            the field name
     *
     * @return the field value
     *
     * @throws Exception
     *             if the field cannot be found or accessed
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(final Object obj, final String fieldName) throws Exception {
        final Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    /**
     * Finds a declared field by name, walking the class hierarchy.
     *
     * @param clazz
     *            the class to start from
     * @param name
     *            the field name
     *
     * @return the field
     *
     * @throws NoSuchFieldException
     *             if no field with the given name is found
     */
    private static Field findField(final Class<?> clazz, final String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (final NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }

    /**
     * Test runInstaller with a process that succeeds (exit code 0). Verifies log output is written.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testRunInstallerSuccess() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final List<String> command = AbstractGitMojo.WINDOWS ? Arrays.asList("cmd", "/c", "echo", "hello")
                : Arrays.asList("echo", "hello");

        mojo.runInstaller(command);

        // runInstaller always logs an empty line after draining the process output
        Mockito.verify(log).info("");
    }

    /**
     * Test runInstaller with a process that exits with a non-zero status. Verifies the error is logged.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testRunInstallerFailedStatus() throws Exception {
        Assumptions.assumeFalse(AbstractGitMojo.WINDOWS, "Non-zero exit test only applicable on non-Windows");

        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        mojo.runInstaller(Arrays.asList("bash", "-c", "exit 1"));

        Mockito.verify(log).error(Mockito.contains("Process failed with error status:"));
    }

    /**
     * Test extractPortableGit when the expected local directory already exists. Verifies gitPath is set to the existing
     * location and no artifact resolution is attempted.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExtractPortableGitExistingLocation() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);
        setField(mojo, "remoteRepositories", Collections.emptyList());

        // Create the directory that extractPortableGit looks for
        final Path existingLocation = tempDir.resolve(portableGit.getName()).resolve(portableGit.getVersion());
        Files.createDirectories(existingLocation);

        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        mojo.extractPortableGit();

        // gitPath should now point into the existing PortableGit location
        final String gitPath = getField(mojo, "gitPath");
        Assertions.assertTrue(gitPath.contains(portableGit.getName()), "gitPath should reference the PortableGit name");
    }

    /**
     * Test extractPortableGit when artifact resolution throws ArtifactResolutionException. Verifies
     * MojoFailureException is thrown.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExtractPortableGitResolutionException() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);
        setField(mojo, "remoteRepositories", Collections.emptyList());

        // Point to tempDir so the "already exists" branch is NOT taken (directory does not exist)
        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        Mockito.when(repositorySystem.resolveArtifact(Mockito.any(), Mockito.any()))
                .thenThrow(new ArtifactResolutionException(Collections.emptyList(), "resolution failed in test"));
        setField(mojo, "repositorySystem", repositorySystem);

        Assertions.assertThrows(MojoFailureException.class, mojo::extractPortableGit);
    }

    /**
     * Test extractPortableGit when the resolved ArtifactResult reports isResolved() == false. Verifies
     * MojoFailureException is thrown.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExtractPortableGitNotResolved() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);
        setField(mojo, "remoteRepositories", Collections.emptyList());

        // Point to tempDir so the "already exists" branch is NOT taken
        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        final ArtifactResult notResolved = Mockito.mock(ArtifactResult.class);
        Mockito.when(notResolved.isResolved()).thenReturn(false);
        Mockito.when(repositorySystem.resolveArtifact(Mockito.any(), Mockito.any())).thenReturn(notResolved);
        setField(mojo, "repositorySystem", repositorySystem);

        Assertions.assertThrows(MojoFailureException.class, mojo::extractPortableGit);
    }

    /**
     * Test checkGitSetup successfully initializes portableGit and resolves to an existing directory. Verifies gitPath
     * is set after the call.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testCheckGitSetupWithExistingLocation() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);
        setField(mojo, "remoteRepositories", Collections.emptyList());

        // Pre-create the directory so extractPortableGit takes the fast path
        final PortableGit portableGit = new PortableGit(log);
        final Path existingLocation = tempDir.resolve(portableGit.getName()).resolve(portableGit.getVersion());
        Files.createDirectories(existingLocation);

        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        mojo.checkGitSetup();

        final String gitPath = getField(mojo, "gitPath");
        Assertions.assertNotNull(gitPath, "gitPath should be set after checkGitSetup");
        Assertions.assertFalse(gitPath.isEmpty(), "gitPath should not be empty after checkGitSetup");
    }

    /**
     * Creates a minimal tar.gz archive in the given directory containing a single script file.
     *
     * @param tarGzPath
     *            path where the tar.gz should be written
     * @param entryName
     *            the name of the entry inside the archive (may include sub-directories)
     * @param entryContent
     *            the bytes to store in the archive entry
     *
     * @throws Exception
     *             if writing fails
     */
    private static void createTarGz(final Path tarGzPath, final String entryName, final byte[] entryContent)
            throws Exception {
        try (java.io.OutputStream fos = Files.newOutputStream(tarGzPath);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            // Add a directory entry first
            final String dirName = entryName.contains("/") ? entryName.substring(0, entryName.lastIndexOf('/') + 1)
                    : "";
            if (!dirName.isEmpty()) {
                final TarArchiveEntry dirEntry = new TarArchiveEntry(dirName);
                taos.putArchiveEntry(dirEntry);
                taos.closeArchiveEntry();
            }

            // Add the file entry
            final TarArchiveEntry fileEntry = new TarArchiveEntry(entryName);
            fileEntry.setSize(entryContent.length);
            taos.putArchiveEntry(fileEntry);
            taos.write(entryContent);
            taos.closeArchiveEntry();
        }
    }

    /**
     * Test installGit extracts files from a synthetic tar.gz to the configured local repository directory. Verifies
     * that the content is extracted even though the subsequent "installer" call will fail on non-Windows (the error is
     * caught and logged rather than re-thrown).
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testInstallGitExtractsFiles() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);

        // Stub repoSession so installGit knows where to extract
        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        // Create a minimal tar.gz that contains one directory entry and one file entry
        final Path tarGzPath = tempDir.resolve("portable-git.tar.gz");
        final byte[] scriptContent = "#!/bin/sh\nexit 0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        createTarGz(tarGzPath, portableGit.getName() + "/install.sh", scriptContent);

        // Create a mock artifact pointing to the synthetic tar.gz
        final Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getFile()).thenReturn(tarGzPath.toFile());

        final String location = tempDir.toFile().getAbsolutePath() + java.io.File.separator + portableGit.getName()
                + java.io.File.separator + portableGit.getVersion();

        // Should not throw; any IOException from running the "installer" is caught and logged
        mojo.installGit(artifact, location);

        // Verify that at least one file was extracted to the PortableGit directory
        final Path extractedDir = tempDir.resolve(portableGit.getName());
        Assertions.assertTrue(Files.exists(extractedDir), "PortableGit extraction directory should have been created");
    }

    /**
     * Test installGit gracefully handles an IOException from a bad tar.gz (empty file).
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testInstallGitHandlesBadTarGz() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);
        setField(mojo, "repoSession", repoSession);

        // Create an empty (invalid) file as the "artifact"
        final Path emptyFile = Files.createTempFile(tempDir, "empty", ".tar.gz");

        final Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getFile()).thenReturn(emptyFile.toFile());

        final String location = tempDir.toFile().getAbsolutePath() + java.io.File.separator + portableGit.getName()
                + java.io.File.separator + portableGit.getVersion();

        // Should not throw – IOException from bad gzip is caught and logged
        Assertions.assertDoesNotThrow(() -> mojo.installGit(artifact, location));
        Mockito.verify(log).error(Mockito.eq(""), Mockito.any(Exception.class));
    }

    /**
     * Test installGit detects a directory-traversal attempt and throws an IOException that is caught internally.
     * Verifies that the error is logged and no exception escapes.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testInstallGitDirectoryTraversal() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);

        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        // Create a tar.gz with a path-traversal entry (../../evil.sh)
        final Path tarGzPath = tempDir.resolve("traversal.tar.gz");
        final byte[] content = "evil content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (java.io.OutputStream fos = Files.newOutputStream(tarGzPath);
                GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            final TarArchiveEntry fileEntry = new TarArchiveEntry("../../evil.sh");
            fileEntry.setSize(content.length);
            taos.putArchiveEntry(fileEntry);
            taos.write(content);
            taos.closeArchiveEntry();
        }

        final Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getFile()).thenReturn(tarGzPath.toFile());

        final String location = tempDir.toFile().getAbsolutePath() + java.io.File.separator + portableGit.getName()
                + java.io.File.separator + portableGit.getVersion();

        // Should not throw – the traversal IOException is caught and logged
        Assertions.assertDoesNotThrow(() -> mojo.installGit(artifact, location));
        Mockito.verify(log, Mockito.atLeastOnce()).error(Mockito.eq(""), Mockito.any(Exception.class));
    }

    /**
     * Test installGit runs the installer and sets gitPath when a valid archive is provided and the installer succeeds.
     * The runInstaller method is overridden to avoid spawning a real process.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testInstallGitRunsInstaller() throws Exception {
        final GitMojo mojo = new GitMojo() {
            @Override
            protected void runInstaller(final java.util.List<String> command)
                    throws java.io.IOException, InterruptedException {
                // Simulate successful installer execution without spawning a process
            }
        };
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);

        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        // Create a minimal tar.gz with a file entry
        final Path tarGzPath = tempDir.resolve("portable-git-installer.tar.gz");
        final byte[] scriptContent = "#!/bin/sh\nexit 0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        createTarGz(tarGzPath, portableGit.getName() + "/install.sh", scriptContent);

        final Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getFile()).thenReturn(tarGzPath.toFile());

        final String location = tempDir.toFile().getAbsolutePath() + java.io.File.separator + portableGit.getName()
                + java.io.File.separator + portableGit.getVersion();

        mojo.installGit(artifact, location);

        // After successful installation gitPath should be set to location + GIT_USER_BIN
        final String gitPath = getField(mojo, "gitPath");
        Assertions.assertTrue(gitPath.contains(portableGit.getName()), "gitPath should reference the PortableGit name");
    }

    /**
     * Test extractPortableGit calls installGit when the artifact is successfully resolved. Verifies that the code path
     * after isResolved()=true is exercised (lines that call installGit).
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExtractPortableGitResolvedAndInstalled() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        final PortableGit portableGit = new PortableGit(log);
        setField(mojo, "portableGit", portableGit);
        setField(mojo, "remoteRepositories", Collections.emptyList());

        // Point to tempDir so the "already exists" fast-path is NOT taken
        final LocalRepository localRepository = new LocalRepository(tempDir.toFile());
        Mockito.when(repoSession.getLocalRepository()).thenReturn(localRepository);
        setField(mojo, "repoSession", repoSession);

        // Create a real tar.gz so installGit can open it
        final Path tarGzPath = tempDir.resolve("resolved-portable-git.tar.gz");
        final byte[] scriptContent = "#!/bin/sh\nexit 0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        createTarGz(tarGzPath, portableGit.getName() + "/install.sh", scriptContent);

        // Mock a fully resolved artifact result
        final Artifact resolvedArtifact = Mockito.mock(Artifact.class);
        Mockito.when(resolvedArtifact.getFile()).thenReturn(tarGzPath.toFile());

        final ArtifactResult resolvedResult = Mockito.mock(ArtifactResult.class);
        Mockito.when(resolvedResult.isResolved()).thenReturn(true);
        Mockito.when(resolvedResult.getArtifact()).thenReturn(resolvedArtifact);

        Mockito.when(repositorySystem.resolveArtifact(Mockito.any(), Mockito.any())).thenReturn(resolvedResult);
        setField(mojo, "repositorySystem", repositorySystem);

        // extractPortableGit should call installGit without throwing
        Assertions.assertDoesNotThrow(mojo::extractPortableGit);
    }

}
