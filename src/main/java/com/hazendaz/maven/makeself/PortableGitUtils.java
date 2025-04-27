/*
 *    Copyright 2011-2025 the original author or authors.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.artifact.Artifact;

public final class PortableGitUtils {

    /**
     * Do not allow instantiation.
     */
    private PortableGitUtils() {
        // Do not allow Instantiation
    }

    /**
     * Untar portable git.
     *
     * @param baseDirectory
     *            the base directory path
     * @param portableGit
     *            the portable git
     * @param artifact
     *            the artifact
     * @param log
     *            the log
     */
    protected static void untarPortableGit(final File baseDirectory, final PortableGit portableGit,
            final Artifact artifact, final Log log) {
        Path currentPath = null;

        // Unzip 'git-for-windows-*-portable.tar.gz' from '.m2/repository/com/github/hazendaz/git/git-for-windows'
        // into '.m2/repository/PortableGit'
        log.debug("Extracting portable git tarball: " + artifact.getFile().toPath());
        try (InputStream inputStream = Files.newInputStream(artifact.getFile().toPath());
                InputStream bufferedStream = new BufferedInputStream(inputStream);
                InputStream gzipStream = new GzipCompressorInputStream(bufferedStream);
                ArchiveInputStream<TarArchiveEntry> tarStream = new TarArchiveInputStream(gzipStream)) {
            ArchiveEntry entry;
            String directory = baseDirectory + File.separator + portableGit.getName();
            while ((entry = tarStream.getNextEntry()) != null) {
                currentPath = Path.of(directory, entry.getName());
                log.debug("Found path: " + currentPath);
                if (!currentPath.normalize().startsWith(directory)) {
                    throw new IOException("Bad zip entry, possible directory traversal");
                }
                if (Files.isDirectory(currentPath)) {
                    Files.createDirectories(currentPath);
                } else {
                    Path parent = currentPath.getParent();
                    if (!Files.exists(parent)) {
                        Files.createDirectory(parent);
                    }
                    Files.copy(tarStream, currentPath);

                    if (currentPath.toString().contains("7z.exe")) {
                        PortableGitUtils.unzipPortableGit(baseDirectory, portableGit, currentPath, log);
                    }
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    /**
     * Extracts the embedded .7z archive from a self-extracting .exe file. Returns the path to the temporary .7z file.
     * Uses a buffer to copy bytes from the signature onward for robustness.
     */
    private static Path extractEmbedded7zFromExe(Path exePath, Log log) throws IOException {
        final byte[] signature = new byte[] { 0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C };
        byte[] buffer = new byte[8192];
        long signaturePos = -1;
        try (InputStream in = Files.newInputStream(exePath)) {
            int match = 0;
            long pos = 0;
            int b;
            // Scan for 7z signature
            while ((b = in.read()) != -1) {
                if (b == (signature[match] & 0xFF)) {
                    match++;
                    if (match == signature.length) {
                        signaturePos = pos - signature.length + 1;
                        break;
                    }
                } else {
                    match = 0;
                }
                pos++;
            }
            if (signaturePos == -1) {
                throw new IOException("7z signature not found in SFX exe: " + exePath);
            }
            log.info("7z signature found at offset: " + signaturePos);
        }
        // Copy from signature position to end of file using buffer
        Path temp7z = Files.createTempFile("extracted", ".7z");
        try (InputStream in = Files.newInputStream(exePath);
                OutputStream out = Files.newOutputStream(temp7z)) {
            long skipped = in.skip(signaturePos);
            if (skipped != signaturePos) {
                throw new IOException("Failed to skip to 7z signature position. Skipped: " + skipped);
            }
            int read;
            long totalBytes = 0;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalBytes += read;
            }
            log.info("Extracted embedded 7z archive size: " + totalBytes + " bytes");
        }
        log.debug("Extracted embedded 7z archive to: " + temp7z);
        return temp7z;
    }

    /**
     * Unzip portable git.
     *
     * Note: Extraction of official Git for Windows 7z archives may fail due to unsupported LZMA2 options (e.g., lc + lp > 4).
     * This is a limitation of Apache Commons Compress and XZ for Java. If this occurs, use an external tool (7-Zip, WinRAR) for extraction.
     *
     * @param baseDirectory
     *            the base directory path
     * @param portableGit
     *            the portable git
     * @param path
     *            the path to zip file
     * @param log
     *            the log
     */
    protected static void unzipPortableGit(final File baseDirectory, final PortableGit portableGit, final Path path,
            final Log log) {
        Path currentPath = path;
        Path sevenZPath = currentPath;
        boolean isTemp = false;
        // Unzip 'PortableGit-*-bit.7z.exe' from '.m2/repository/PortableGit' into '.m2/repository/PortableGit'
        log.debug("Extracting portable git self extracting 7z: " + currentPath);
        try {
            // Check if file is .exe (SFX)
            if (currentPath.toString().toLowerCase().endsWith(".exe")) {
                sevenZPath = extractEmbedded7zFromExe(currentPath, log);
                isTemp = true;
            }
            try (SevenZFile sevenZFile = SevenZFile.builder().setPath(sevenZPath).get()) {
                log.debug("found entry in portable git: " + sevenZFile.getNextEntry());
                SevenZArchiveEntry entry;
                String directory = baseDirectory + File.separator + portableGit.getName();
                while ((entry = sevenZFile.getNextEntry()) != null) {
                    currentPath = Path.of(directory, entry.getName());
                    log.debug("Found path: " + currentPath);
                    if (!currentPath.normalize().startsWith(directory)) {
                        throw new IOException("Bad zip entry, possible directory traversal");
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(currentPath);
                    } else {
                        Path parent = currentPath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }
                        Files.copy(sevenZFile.getInputStream(entry), currentPath);
                    }
                }
                log.debug("at end of portable git");
            }
        } catch (IOException e) {
            log.error("", e);
        } finally {
            // Clean up temp file if created
            if (isTemp && sevenZPath != null && Files.exists(sevenZPath)) {
                try {
                    Files.delete(sevenZPath);
                } catch (IOException e) {
                    log.warn("Failed to delete temp 7z file: " + sevenZPath, e);
                }
            }
        }
    }

}