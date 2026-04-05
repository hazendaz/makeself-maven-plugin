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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Class MakeselfMojoTest.
 */
@ExtendWith(MockitoExtension.class)
class MakeselfMojoTest {

    /** Temporary directory for test files. */
    @TempDir
    Path tempDir;

    /** Mock Maven log. */
    @Mock
    private Log log;

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
     * Invokes the private {@code loadArgs} method on the given mojo via reflection.
     *
     * @param mojo
     *            the mojo instance
     *
     * @return the list of command-line arguments built by loadArgs
     *
     * @throws Exception
     *             if the method cannot be invoked
     */
    @SuppressWarnings("unchecked")
    private static List<String> callLoadArgs(final MakeselfMojo mojo) throws Exception {
        final Method method = MakeselfMojo.class.getDeclaredMethod("loadArgs");
        method.setAccessible(true);
        return (List<String>) method.invoke(mojo);
    }

    /**
     * Invokes the private {@code isTrue} method on the given mojo via reflection.
     *
     * @param mojo
     *            the mojo instance
     * @param value
     *            the Boolean value to test
     *
     * @return the result of isTrue
     *
     * @throws Exception
     *             if the method cannot be invoked
     */
    private static boolean callIsTrue(final MakeselfMojo mojo, final Boolean value) throws Exception {
        final Method method = MakeselfMojo.class.getDeclaredMethod("isTrue", Boolean.class);
        method.setAccessible(true);
        return (boolean) method.invoke(mojo, value);
    }

    /**
     * Test skip execution logs message and returns without error.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testSkipExecution() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        setField(mojo, "skip", true);

        mojo.execute();

        Mockito.verify(log).info("Makeself is skipped");
    }

    /**
     * Test missing archive dir throws MojoExecutionException.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testMissingArchiveDir() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        setField(mojo, "buildTarget", tempDir.toString() + "/");
        setField(mojo, "archiveDir", "does-not-exist");

        Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
    }

    /**
     * Test inline script without script args throws MojoExecutionException.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testInlineScriptWithoutScriptArgs() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        Files.createDirectory(tempDir.resolve("archive"));
        setField(mojo, "inlineScript", true);
        setField(mojo, "buildTarget", tempDir.toString() + "/");
        setField(mojo, "archiveDir", "archive");
        // scriptArgs remains null

        Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
    }

    /**
     * Test startup script without leading dot-slash throws MojoExecutionException.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testStartupScriptWithoutDotSlash() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        Files.createDirectory(tempDir.resolve("archive"));
        setField(mojo, "buildTarget", tempDir.toString() + "/");
        setField(mojo, "archiveDir", "archive");
        setField(mojo, "startupScript", "script.sh");

        Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
    }

    /**
     * Test startup script file missing throws MojoExecutionException.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testStartupScriptFileMissing() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        Files.createDirectory(tempDir.resolve("archive"));
        setField(mojo, "buildTarget", tempDir.toString() + "/");
        setField(mojo, "archiveDir", "archive");
        setField(mojo, "startupScript", "./nonexistent.sh");

        Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
    }

    /**
     * Test load args returns empty list when all optional parameters are null.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testLoadArgsEmpty() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);

        final List<String> args = callLoadArgs(mojo);

        Assertions.assertTrue(args.isEmpty());
    }

    /**
     * Test load args includes the correct CLI flag for every boolean parameter when set to true.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testLoadArgsBooleanFlags() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        setField(mojo, "tarQuietly", Boolean.TRUE);
        setField(mojo, "quiet", Boolean.TRUE);
        setField(mojo, "gzip", Boolean.TRUE);
        setField(mojo, "bzip2", Boolean.TRUE);
        setField(mojo, "bzip3", Boolean.TRUE);
        setField(mojo, "pbzip2", Boolean.TRUE);
        setField(mojo, "xz", Boolean.TRUE);
        setField(mojo, "lzo", Boolean.TRUE);
        setField(mojo, "lz4", Boolean.TRUE);
        setField(mojo, "zstd", Boolean.TRUE);
        setField(mojo, "pigz", Boolean.TRUE);
        setField(mojo, "base64", Boolean.TRUE);
        setField(mojo, "gpgEncrypt", Boolean.TRUE);
        setField(mojo, "gpgAsymmetricEncryptSign", Boolean.TRUE);
        setField(mojo, "sslEncrypt", Boolean.TRUE);
        setField(mojo, "sslNoMd", Boolean.TRUE);
        setField(mojo, "compress", Boolean.TRUE);
        setField(mojo, "nochown", Boolean.TRUE);
        setField(mojo, "chown", Boolean.TRUE);
        setField(mojo, "nocomp", Boolean.TRUE);
        setField(mojo, "notemp", Boolean.TRUE);
        setField(mojo, "needroot", Boolean.TRUE);
        setField(mojo, "current", Boolean.TRUE);
        setField(mojo, "follow", Boolean.TRUE);
        setField(mojo, "noprogress", Boolean.TRUE);
        setField(mojo, "append", Boolean.TRUE);
        setField(mojo, "copy", Boolean.TRUE);
        setField(mojo, "nox11", Boolean.TRUE);
        setField(mojo, "nowait", Boolean.TRUE);
        setField(mojo, "nomd5", Boolean.TRUE);
        setField(mojo, "nocrc", Boolean.TRUE);
        setField(mojo, "sha256", Boolean.TRUE);
        setField(mojo, "keepUmask", Boolean.TRUE);
        setField(mojo, "exportConf", Boolean.TRUE);
        setField(mojo, "nooverwrite", Boolean.TRUE);

        final List<String> args = callLoadArgs(mojo);

        Assertions.assertTrue(args.containsAll(Arrays.asList("--tar-quietly", "--quiet", "--gzip", "--bzip2", "--bzip3",
                "--pbzip2", "--xz", "--lzo", "--lz4", "--zstd", "--pigz", "--base64", "--gpg-encrypt",
                "--gpg-asymmetric-encrypt-sign", "--ssl-encrypt", "--ssl-no-md", "--compress", "--nochown", "--chown",
                "--nocomp", "--notemp", "--needroot", "--current", "--follow", "--noprogress", "--append", "--copy",
                "--nox11", "--nowait", "--nomd5", "--nocrc", "--sha256", "--keep-umask", "--export-conf",
                "--nooverwrite")));
    }

    /**
     * Test load args includes the correct flag-value pairs for all string and integer parameters.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testLoadArgsStringAndIntOptions() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();
        mojo.setLog(log);
        setField(mojo, "sslPasswd", "mypassword");
        setField(mojo, "sslPassSrc", "env:MY_PASS");
        setField(mojo, "complevel", 6);
        setField(mojo, "compExtra", "--extra-opt");
        setField(mojo, "threads", 4);
        setField(mojo, "headerFile", "my-header.sh");
        setField(mojo, "preextractScript", "preextract.sh");
        setField(mojo, "cleanupScript", "cleanup.sh");
        setField(mojo, "lsmFile", "my-package.lsm");
        setField(mojo, "gpgExtraOpt", "--gpg-opt");
        setField(mojo, "tarFormatOpt", "posix");
        setField(mojo, "tarExtraOpt", "--exclude=.git");
        setField(mojo, "untarExtraOpt", "--strip=1");
        setField(mojo, "signPassphrase", "signingkey");
        setField(mojo, "extractTargetDir", "/opt/myapp");
        setField(mojo, "packagingDate", "2026-01-01");
        setField(mojo, "licenseFile", "LICENSE");
        setField(mojo, "helpHeaderFile", "help-header.txt");

        final List<String> args = callLoadArgs(mojo);

        Assertions.assertAll(() -> Assertions.assertTrue(args.contains("--ssl-passwd") && args.contains("mypassword")),
                () -> Assertions.assertTrue(args.contains("--ssl-pass-src") && args.contains("env:MY_PASS")),
                () -> Assertions.assertTrue(args.contains("--complevel") && args.contains("6")),
                () -> Assertions.assertTrue(args.contains("--comp-extra") && args.contains("--extra-opt")),
                () -> Assertions.assertTrue(args.contains("--threads") && args.contains("4")),
                () -> Assertions.assertTrue(args.contains("--header") && args.contains("my-header.sh")),
                () -> Assertions.assertTrue(args.contains("--preextract") && args.contains("preextract.sh")),
                () -> Assertions.assertTrue(args.contains("--cleanup") && args.contains("cleanup.sh")),
                () -> Assertions.assertTrue(args.contains("--lsm") && args.contains("my-package.lsm")),
                () -> Assertions.assertTrue(args.contains("--gpg-extra") && args.contains("--gpg-opt")),
                () -> Assertions.assertTrue(args.contains("--tar-format") && args.contains("posix")),
                () -> Assertions.assertTrue(args.contains("--tar-extra") && args.contains("--exclude=.git")),
                () -> Assertions.assertTrue(args.contains("--untar-extra") && args.contains("--strip=1")),
                () -> Assertions.assertTrue(args.contains("--sign") && args.contains("signingkey")),
                () -> Assertions.assertTrue(args.contains("--target") && args.contains("/opt/myapp")),
                () -> Assertions.assertTrue(args.contains("--packaging-date") && args.contains("2026-01-01")),
                () -> Assertions.assertTrue(args.contains("--license") && args.contains("LICENSE")),
                () -> Assertions.assertTrue(args.contains("--help-header") && args.contains("help-header.txt")));
    }

    /**
     * Test is true returns false for null, true for Boolean.TRUE, and false for Boolean.FALSE.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testIsTrue() throws Exception {
        final MakeselfMojo mojo = new MakeselfMojo();

        Assertions.assertAll(() -> Assertions.assertFalse(callIsTrue(mojo, null)),
                () -> Assertions.assertTrue(callIsTrue(mojo, Boolean.TRUE)),
                () -> Assertions.assertFalse(callIsTrue(mojo, Boolean.FALSE)));
    }

}
