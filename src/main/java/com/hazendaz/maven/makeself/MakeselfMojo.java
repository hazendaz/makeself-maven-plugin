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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * The Class MakeselfMojo.
 */
@Mojo(name = "makeself", defaultPhase = LifecyclePhase.VERIFY, requiresProject = false)
public class MakeselfMojo extends AbstractGitMojo {

    /** Permissions for makeself script results. */
    private static final String PERMISSIONS = "rwxr-xr--";

    /** Static ATTACH_ARTIFACT to maven lifecycle. */
    private static final boolean ATTACH_ARTIFACT = true;

    /**
     * archive_dir is the name of the directory that contains the files to be archived.
     */
    @Parameter(defaultValue = "makeself", property = "archiveDir", required = true)
    private String archiveDir;

    /**
     * file_name is the name of the archive to be created.
     */
    @Parameter(defaultValue = "makeself.sh", property = "fileName", required = true)
    private String fileName;

    /**
     * label is an arbitrary text string describing the package. It will be displayed while extracting the files.
     */
    @Parameter(defaultValue = "Makeself self-extractable archive", property = "label", required = true)
    private String label;

    /**
     * startup_script is the command to be executed from within the directory of extracted files. Thus, if you wish to
     * execute a program contained in this directory, you must prefix your command with './'. For example, './program'
     * will be fine.
     */
    @Parameter(defaultValue = "./makeself.sh", property = "startupScript", required = true)
    private String startupScript;

    /**
     * extension is for type of fileName being created. It defaults to 'sh' for backwards compatibility. Makeself
     * defines 'run' as its default, therefore when using 'run', set extension to 'run'. This extension is used when
     * attaching resulting artifact to maven.
     *
     * @since 1.5.0
     */
    @Parameter(defaultValue = "sh", property = "extension")
    private String extension;

    /**
     * classifier is for fileName being created to allow for more than one. If not defined, multiple artifacts will all
     * be installed to same m2 location. The artifact will take on the project artifact where classfier is the physical
     * name attribute you which to create for the fileName.
     *
     * @since 1.5.0
     */
    @Parameter(property = "classifier")
    private String classifier;

    /**
     * inline script allows user to skip strict verification of startup script for cases where script is defined
     * directly such as 'echo hello' where 'echo' is a 'program' to run and 'hello' is one of many 'script arguments'.
     * Behaviour of makeself plugin prior to 1.5.0 allowed for this undocumented feature which is further allowed and
     * shown as an example in makeself. Verification therefore checks that both startupScript and scriptArgs exist only.
     *
     * @since 1.5.1
     */
    @Parameter(property = "inlineScript")
    private boolean inlineScript;

    /**
     * script_args are additional arguments for startup_script passed as an array.
     *
     * <pre>
     * {@code
     * <scriptArgs>
     *   <scriptArg>arg1</scriptArg>
     *   <scriptArg>arg2</scriptArg>
     * </scriptArgs>
     * }
     * </pre>
     */
    @Parameter(property = "scriptArgs")
    private List<String> scriptArgs;

    /**
     * --version | -v : Print out Makeself version number and exit
     *
     * @since 1.6.0
     */
    @Parameter(property = "version")
    private Boolean version;

    /**
     * --help | -h : Print out this help message and exit (exit is custom to makeself maven plugin).
     */
    @Parameter(property = "help")
    private Boolean help;

    /**
     * --tar-quietly : Suppress verbose output from the tar command.
     *
     * @since 1.6.0
     */
    @Parameter(property = "tarQuietly")
    private Boolean tarQuietly;

    /**
     * --quiet | -q : Do not print any messages other than errors.
     *
     * @since 1.6.0
     */
    @Parameter(property = "quiet")
    private Boolean quiet;

    /**
     * --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux).
     */
    @Parameter(property = "gzip")
    private Boolean gzip;

    /**
     * --bzip2 : Use bzip2 instead of gzip for better compression. The bzip2 command must be available in the command
     * path. It is recommended that the archive prefix be set to something like '.bz2.run', so that potential users know
     * that they'll need bzip2 to extract it.
     */
    @Parameter(property = "bzip2")
    private Boolean bzip2;

    /**
     * --bzip3 : Use bzip3 instead of gzip for better compression. The bzip3 command must be available in the command
     * path. It is recommended that the archive prefix be set to something like '.bz3.run', so that potential users know
     * that they'll need bzip3 to extract it.
     *
     * @since 1.6.0
     */
    @Parameter(property = "bzip3")
    private Boolean bzip3;

    /**
     * --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs. The
     * pbzip2 command must be available in the command path. It is recommended that the archive prefix be set to
     * something like '.pbz2.run', so that potential users know that they'll need bzip2 to extract it.
     */
    @Parameter(property = "pbzip2")
    private Boolean pbzip2;

    /**
     * --xz : Use xz instead of gzip for better compression. The xz command must be available in the command path. It is
     * recommended that the archive prefix be set to something like '.xz.run' for the archive, so that potential users
     * know that they'll need xz to extract it.
     */
    @Parameter(property = "xz")
    private Boolean xz;

    /**
     * --lzo : Use lzop instead of gzip for better compression. The lzop command must be available in the command path.
     * It is recommended that the archive prefix be set to something like '.lzo.run' for the archive, so that potential
     * users know that they'll need lzop to extract it.
     */
    @Parameter(property = "lzo")
    private Boolean lzo;

    /**
     * --lz4 : Use lz4 instead of gzip for better compression. The lz4 command must be available in the command path. It
     * is recommended that the archive prefix be set to something like '.lz4.run' for the archive, so that potential
     * users know that they'll need lz4 to extract it.
     */
    @Parameter(property = "lz4")
    private Boolean lz4;

    /**
     * --zstd : Use zstd for compression.
     */
    @Parameter(property = "zstd")
    private Boolean zstd;

    /**
     * --pigz : Use pigz for compression.
     */
    @Parameter(property = "pigz")
    private Boolean pigz;

    /**
     * --base64 : Encode the archive to ASCII in Base64 format (base64 command required).
     */
    @Parameter(property = "base64")
    private Boolean base64;

    /**
     * --gpg-encrypt : Encrypt the archive using gpg -ac -z $COMPRESS_LEVEL. This will prompt for a password to encrypt
     * with. Assumes that potential users have gpg installed.
     */
    @Parameter(property = "gpgEncrypt")
    private Boolean gpgEncrypt;

    /**
     * --gpg-asymmetric-encrypt-sign : Instead of compressing, asymmetrically encrypt and sign the data using GPG."
     */
    @Parameter(property = "gpgAsymmetricEncryptSign")
    private Boolean gpgAsymmetricEncryptSign;

    /**
     * --ssl-encrypt : Encrypt the archive using openssl aes-256-cbc -a -salt. This will prompt for a password to
     * encrypt with. Assumes that the potential users have the OpenSSL tools installed.
     */
    @Parameter(property = "sslEncrypt")
    private Boolean sslEncrypt;

    /**
     * --ssl-passwd pass : Use the given password to encrypt the data using OpenSSL.
     */
    @Parameter(property = "sslPasswd")
    private String sslPasswd;

    /**
     * --ssl-pass-src : Use the given src as the source of password to encrypt the data using OpenSSL. See \"PASS PHRASE
     * ARGUMENTS\" in man openssl. If this option is not supplied, the user wil be asked to enter encryption pasword on
     * the current terminal.
     */
    @Parameter(property = "sslPassSrc")
    private String sslPassSrc;

    /**
     * --ssl-no-md : Do not use \"-md\" option not supported by older OpenSSL.
     */
    @Parameter(property = "sslNoMd")
    private Boolean sslNoMd;

    /**
     * --compress : Use the UNIX compress command to compress the data. This should be the default on all platforms that
     * don't have gzip available.
     */
    @Parameter(property = "compress")
    private Boolean compress;

    /**
     * --complevel : Specify the compression level for gzip, bzip2, bzip3, pbzip2, xz, lzo or lz4. (defaults to 9).
     */
    @Parameter(property = "complevel")
    private Integer complevel;

    /**
     * --comp-extra : Append extra options to the chosen compressor.
     */
    @Parameter(property = "compExtra")
    private String compExtra;

    /**
     * --nochown : Do not give the target folder to the current user (default)
     *
     * @since 1.6.0
     */
    @Parameter(property = "nochown")
    private Boolean nochown;

    /**
     * --chown : Give the target folder to the current user recursively
     *
     * @since 1.6.0
     */
    @Parameter(property = "chown")
    private Boolean chown;

    /**
     * --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
     */
    @Parameter(property = "nocomp")
    private Boolean nocomp;

    /**
     * --threads : Specify the number of threads to be used by compressors that support parallelization. Omit to use
     * compressor's default. Most useful (and required) for opting into xz's threading, usually with --threads=0 for all
     * available cores. pbzip2 and pigz are parallel by default, and setting this value allows limiting the number of
     * threads they use.
     */
    @Parameter(property = "threads")
    private Integer threads;

    /**
     * --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
     * created in the current directory. This is better to distribute software packages that may extract and compile by
     * themselves (i.e. launch the compilation through the embedded script).
     */
    @Parameter(property = "notemp")
    private Boolean notemp;

    /**
     * --needroot : Check that the root user is extracting the archive before proceeding
     *
     * @since 1.6.0
     */
    @Parameter(property = "needroot")
    private Boolean needroot;

    /**
     * --current : Files will be extracted to the current directory, instead of in a subdirectory. This option implies
     * --notemp and ddoes not require aq startup_script.
     */
    @Parameter(property = "current")
    private Boolean current;

    /**
     * --follow : Follow the symbolic links inside of the archive directory, i.e. store the files that are being pointed
     * to instead of the links themselves.
     */
    @Parameter(property = "follow")
    private Boolean follow;

    /**
     * --noprogress : Do not show the progress during the decompression
     *
     * @since 1.6.0
     */
    @Parameter(property = "noprogress")
    private Boolean noprogress;

    /**
     * --append (new in 2.1.x): Append data to an existing archive, instead of creating a new one. In this mode, the
     * settings from the original archive are reused (compression type, label, embedded script), and thus don't need to
     * be specified again on the command line.
     */
    @Parameter(property = "append")
    private Boolean append;

    /**
     * --header: Makeself 2.0 uses a separate file to store the header stub, called makeself-header.sh. By default, it
     * is assumed that it is stored in the same location as makeself.sh. This option can be used to specify its actual
     * location if it is stored someplace else. This is not required for this plugin as the header is provided.
     */
    @Parameter(property = "headerFile")
    private String headerFile;

    /**
     * --preextract: Specify a pre-extraction script. The script is executed with the same environment and initial
     * `script_args` as `startup_script`.
     *
     * @since 1.7.0
     */
    @Parameter(property = "preextractScript")
    private String preextractScript;

    /**
     * --cleanup: Specify a script that is run when execution is interrupted or finishes successfully. The script is
     * executed with the same environment and initial `script_args` as `startup_script`.
     */
    @Parameter(property = "cleanupScript")
    private String cleanupScript;

    /**
     * --copy : Upon extraction, the archive will first extract itself to a temporary directory. The main application of
     * this is to allow self-contained installers stored in a Makeself archive on a CD, when the installer program will
     * later need to unmount the CD and allow a new one to be inserted. This prevents "Filesystem busy" errors for
     * installers that span multiple CDs.
     */
    @Parameter(property = "copy")
    private Boolean copy;

    /** --nox11 : Disable the automatic spawning of a new terminal in X11. */
    @Parameter(property = "nox11")
    private Boolean nox11;

    /** --nowait : Do not wait for user input after executing embedded program from an xterm. */
    @Parameter(property = "nowait")
    private Boolean nowait;

    /**
     * --nomd5 : Disable the creation of a MD5 checksum for the archive. This speeds up the extraction process if
     * integrity checking is not necessary.
     */
    @Parameter(property = "nomd5")
    private Boolean nomd5;

    /**
     * --nocrc : Disable the creation of a CRC checksum for the archive. This speeds up the extraction process if
     * integrity checking is not necessary.
     */
    @Parameter(property = "nocrc")
    private Boolean nocrc;

    /**
     * --sha256 : Compute a SHA256 checksum for the archive.
     */
    @Parameter(property = "sha256")
    private Boolean sha256;

    /**
     * --sign passphrase : Signature private key to sign the package with.
     *
     * @since 1.6.0
     */
    @Parameter(property = "signPassphrase")
    private String signPassphrase;

    /**
     * --lsm file : Provide and LSM file to makeself, that will be embedded in the generated archive. LSM files are
     * describing a software package in a way that is easily parseable. The LSM entry can then be later retrieved using
     * the --lsm argument to the archive. An example of a LSM file is provided with Makeself.
     */
    @Parameter(property = "lsmFile")
    private String lsmFile;

    /**
     * --gpg-extra opt : Append more options to the gpg command line.
     */
    @Parameter(property = "gpgExtraOpt")
    private String gpgExtraOpt;

    /**
     * --tar-format opt :Specify the tar archive format (default is ustar); you may use any value accepted by your tar
     * command (such as posix, v7, etc).
     */
    @Parameter(property = "tarFormatOpt")
    private String tarFormatOpt;

    /**
     * --tar-extra opt : Append more options to the tar command line.
     * <p>
     * For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar, one
     * can use makeself.sh --tar-extra "--exclude=.git" ...
     */
    @Parameter(property = "tarExtraOpt")
    private String tarExtraOpt;

    /**
     * --untar-extra opt : Append more options to the during the extraction of the tar archive.
     */
    @Parameter(property = "untarExtraOpt")
    private String untarExtraOpt;

    /**
     * --target dir : Specify the directory where the archive will be extracted. This option implies --notemp and does
     * not require a startup_script.
     *
     * @since 1.6.0
     */
    private String extractTargetDir;

    /**
     * --keep-umask : Keep the umask set to shell default, rather than overriding when executing self-extracting
     * archive.
     */
    @Parameter(property = "keepUmask")
    private Boolean keepUmask;

    /**
     * --export-conf : Export configuration variables to startup_script.
     */
    @Parameter(property = "exportConf")
    private Boolean exportConf;

    /**
     * --packaging-date date : Use provided string as the packaging date instead of the current date.
     */
    @Parameter(property = "packagingDate")
    private String packagingDate;

    /**
     * --license : Append a license file.
     */
    @Parameter(property = "licenseFile")
    private String licenseFile;

    /**
     * --nooverwrite : Do not extract the archive if the specified target directory already exists.
     */
    @Parameter(property = "nooverwrite")
    private Boolean nooverwrite;

    /**
     * --help-header file : Add a header to the archive's --help output.
     */
    @Parameter(property = "helpHeaderFile")
    private String helpHeaderFile;

    /** Skip run of plugin. */
    @Parameter(defaultValue = "false", property = "makeself.skip")
    private boolean skip;

    /** Auto run : When set to true, resulting shell will be run. This is useful for testing purposes. */
    @Parameter(defaultValue = "false", property = "autoRun")
    private boolean autoRun;

    /** The build target. */
    @Parameter(defaultValue = "${project.build.directory}/", readonly = true)
    private String buildTarget;

    /** The makeself temp directory. */
    @Parameter(defaultValue = "${project.build.directory}/makeself-tmp/", readonly = true)
    private File makeselfTempDirectory;

    /** Maven ProjectHelper. */
    @Inject
    private MavenProjectHelper projectHelper;

    /** Maven Project. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** The makeself. */
    private Path makeself;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Ensure gitPath is never null
        if (this.gitPath == null) {
            this.gitPath = "";
        }

        // Check if plugin run should be skipped
        if (this.skip) {
            this.getLog().info("Makeself is skipped");
            return;
        }

        // Validate archive directory exists
        Path path = Path.of(this.buildTarget.concat(this.archiveDir));
        if (!Files.exists(path)) {
            throw new MojoExecutionException("ArchiveDir: missing '" + this.buildTarget.concat(this.archiveDir) + "'");
        }

        // Validate inline script or startup script file
        if (this.inlineScript) {
            // Validate inline script has script args
            if (this.scriptArgs == null) {
                throw new MojoExecutionException("ScriptArgs required when running inlineScript");
            }
        } else {
            // Validate startupScript file starts with './'
            if (!this.startupScript.startsWith("./")) {
                throw new MojoExecutionException("StartupScript required to start with './'");
            }

            // Validate startupScript file exists
            path = Path.of(this.buildTarget.concat(this.archiveDir).concat(this.startupScript.substring(1)));
            if (!Files.exists(path)) {
                throw new MojoExecutionException("StartupScript: missing '"
                        + this.buildTarget.concat(this.archiveDir).concat(this.startupScript.substring(1)) + "'");
            }
        }

        // Setup make self files
        this.extractMakeself();

        // Check git setup
        if (this.isWindows()) {
            if (!this.gitPath.isEmpty() && Files.exists(Path.of(this.gitPath))) {
                this.getLog().debug("Using existing 'Git' found at " + this.gitPath);
                this.gitPath = this.gitPath + AbstractGitMojo.GIT_USER_BIN;
            } else {
                this.checkGitSetup();
            }
        } else {
            // Do not use git path when not windows
            this.gitPath = "";
        }

        try {
            // Output version of bash
            this.getLog().debug("Execute Bash Version");
            this.execute(Arrays.asList(this.gitPath + "bash", "--version"), !MakeselfMojo.ATTACH_ARTIFACT);

            // Output version of makeself.sh
            this.getLog().debug("Execute Makeself Version");
            this.execute(Arrays.asList(this.gitPath + "bash", this.makeself.toAbsolutePath().toString(), "--version"),
                    !MakeselfMojo.ATTACH_ARTIFACT);

            // If version arguments supplied, exit as we just printed version.
            if (this.isTrue(this.version)) {
                return;
            }

            // If help arguments supplied, write output and get out of code.
            if (this.isTrue(this.help)) {
                this.getLog().debug("Execute Makeself Help");
                this.execute(Arrays.asList(this.gitPath + "bash", this.makeself.toAbsolutePath().toString(), "--help"),
                        !MakeselfMojo.ATTACH_ARTIFACT);
                return;
            }

            // Basic Configuration
            this.getLog().debug("Loading Makeself Basic Configuration");
            final List<String> target = new ArrayList<>(
                    Arrays.asList(this.gitPath + "bash", this.makeself.toAbsolutePath().toString()));
            target.addAll(this.loadArgs());
            target.add(this.buildTarget.concat(this.archiveDir));
            target.add(this.buildTarget.concat(this.fileName));
            target.add(this.label);
            target.add(this.startupScript);
            if (this.scriptArgs != null) {
                target.addAll(this.scriptArgs);
            }

            // Indicate makeself running
            this.getLog().info("Running makeself build");

            // Execute main run of makeself.sh
            this.getLog().debug("Execute Makeself Build");
            this.execute(target, MakeselfMojo.ATTACH_ARTIFACT);

            // Output info on file makeself created
            this.getLog().debug("Execute Makeself Info on Resulting Shell Script");
            this.execute(Arrays.asList(this.gitPath + "bash", this.buildTarget.concat(this.fileName), "--info"),
                    !MakeselfMojo.ATTACH_ARTIFACT);

            // Output list on file makeself created (non windows need)
            if (!this.isWindows()) {
                this.getLog().debug("Execute Makeself List on Resulting Shell Script");
                this.execute(Arrays.asList(this.gitPath + "bash", this.buildTarget.concat(this.fileName), "--list"),
                        !MakeselfMojo.ATTACH_ARTIFACT);
            }

            // auto run script
            if (this.autoRun) {
                this.getLog().info("Auto-run created shell (this may take a few minutes)");
                this.execute(Arrays.asList(this.gitPath + "bash", this.buildTarget.concat(this.fileName)),
                        !MakeselfMojo.ATTACH_ARTIFACT);
            }
        } catch (final IOException e) {
            this.getLog().error("", e);
        } catch (final InterruptedException e) {
            this.getLog().error("", e);
            // restore interruption status of the corresponding thread
            Thread.currentThread().interrupt();
        }
    }

    private void execute(final List<String> target, final boolean attach) throws IOException, InterruptedException {

        // Log execution target
        this.getLog().debug("Execution commands: " + target);

        // Create Process Builder
        final ProcessBuilder processBuilder = new ProcessBuilder(target);
        processBuilder.redirectErrorStream(true);

        // Add portable git to windows environment
        if (this.isWindows()) {
            final Map<String, String> envs = processBuilder.environment();
            this.getLog().debug("Environment Variables: " + envs);

            if (this.portableGit == null) {
                // Helper as git located in provided location (not real user or system path, just the process)
                final String location = this.gitPath;
                // Windows cmd/powershell shows "Path" in this case
                if (envs.get("Path") != null) {
                    envs.put("Path", location + ";" + envs.get("Path"));
                    this.getLog().debug("Environment Path Variable: " + envs.get("Path"));
                } else if (envs.get("PATH") != null) {
                    // Windows bash shows "PATH" in this case and has issues with spacing as in 'Program Files'
                    envs.put("PATH", location + ";" + envs.get("PATH").replace("Program Files", "\"Program Files\""));
                    this.getLog().debug("Environment Path Variable: " + envs.get("PATH"));
                }
            } else {
                // Helper as portable git located in .m2 (not real user or system path, just the process)
                final String location = this.repoSession.getLocalRepository().getBasedir() + File.separator
                        + this.portableGit.getName() + File.separator + this.portableGit.getVersion();
                // Windows cmd/powershell shows "Path" in this case
                if (envs.get("Path") != null) {
                    envs.put("Path", location + "/usr/bin;" + envs.get("Path"));
                    this.getLog().debug("Environment Path Variable: " + envs.get("Path"));
                } else if (envs.get("PATH") != null) {
                    // Windows bash shows "PATH" in this case and has issues with spacing as in 'Program Files'
                    envs.put("PATH",
                            location + "/usr/bin;" + envs.get("PATH").replace("Program Files", "\"Program Files\""));
                    this.getLog().debug("Environment Path Variable: " + envs.get("PATH"));
                }
            }
        }

        // Create Process
        final Process process = processBuilder.start();

        // Write process output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                this.getLog().info(line);
            }
            this.getLog().info("");
        }

        // Wait for process completion
        final int status = process.waitFor();
        if (status > 0) {
            this.getLog().error(String.join(" ", "makeself failed with error status:", String.valueOf(status)));
        }

        // Attach artifact to maven build for install/deploy/release on success
        if (status == 0 && attach) {
            this.projectHelper.attachArtifact(this.project, this.extension, this.classifier,
                    Path.of(this.buildTarget, FilenameUtils.getName(this.fileName)).toFile());
        }
    }

    /**
     * Extract makeself.
     */
    private void extractMakeself() {
        this.getLog().debug("Extracting Makeself");

        // Create makeself directory
        final Path makeselfTemp = Path.of(this.makeselfTempDirectory.getAbsolutePath());
        if (!Files.exists(makeselfTemp) && !makeselfTemp.toFile().mkdirs()) {
            this.getLog()
                    .error(String.join(" ", "Unable to make directory", this.makeselfTempDirectory.getAbsolutePath()));
            return;
        }
        this.getLog().debug(String.join(" ", "Created directory for", this.makeselfTempDirectory.getAbsolutePath()));

        final ClassLoader classloader = this.getClass().getClassLoader();

        // Write makeself script
        this.makeself = this.makeselfTempDirectory.toPath().resolve("makeself.sh");
        if (!Files.exists(this.makeself)) {
            this.getLog().debug("Writing makeself.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself.sh")) {
                final Path path = this.makeself.toAbsolutePath();
                Files.copy(link, path);
                this.setFilePermissions(this.makeself.toFile());
                this.setPosixFilePermissions(path);
            } catch (final IOException e) {
                this.getLog().error("", e);
            }
        }

        // Write makeself-header script
        final Path makeselfHeader = this.makeselfTempDirectory.toPath().resolve("makeself-header.sh");
        if (!Files.exists(makeselfHeader)) {
            this.getLog().debug("Writing makeself-header.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself-header.sh")) {
                final Path path = makeselfHeader.toAbsolutePath();
                Files.copy(link, path);
                this.setFilePermissions(makeselfHeader.toFile());
                this.setPosixFilePermissions(path);
            } catch (final IOException e) {
                this.getLog().error("", e);
            }
        }
    }

    private void setFilePermissions(final File file) {
        if (!file.setExecutable(true, true)) {
            this.getLog().error(String.join(" ", "Unable to set executable:", file.getName()));
        } else {
            this.getLog().debug(String.join(" ", "Set executable for", file.getName()));
        }
    }

    private void setPosixFilePermissions(final Path path) {
        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(MakeselfMojo.PERMISSIONS);

        try {
            Files.setPosixFilePermissions(path, permissions);
            this.getLog().debug(String.join(" ", "Set Posix File Permissions for", path.toString(), "as",
                    MakeselfMojo.PERMISSIONS));
        } catch (final IOException e) {
            this.getLog().error("Failed attempted Posix permissions", e);
        } catch (final UnsupportedOperationException e) {
            // Attempting but don't care about status if it fails
            this.getLog().debug("Failed attempted Posix permissions", e);
        }
    }

    /**
     * Load args.
     *
     * @return the string
     */
    private List<String> loadArgs() {
        this.getLog().debug("Loading arguments");

        final List<String> args = new ArrayList<>(50);

        // " --tar-quietly : Suppress verbose output from the tar command"
        if (this.isTrue(this.tarQuietly)) {
            args.add("--tar-quietly");
        }

        // " --quiet | -q : Do not print any messages other than errors."
        if (this.isTrue(this.quiet)) {
            args.add("--quiet");
        }

        // --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux)
        if (this.isTrue(this.gzip)) {
            args.add("--gzip");
        }

        // --bzip2 : Use bzip2 instead of gzip for better compression. The bzip2 command must be available in the
        // command path. It is recommended that the archive prefix be set to something like '.bz2.run', so that
        // potential users know that they'll need bzip2 to extract it.
        if (this.isTrue(this.bzip2)) {
            args.add("--bzip2");
        }

        // --bzip3 : Use bzip3 instead of gzip for better compression. The bzip3 command must be available in the
        // command path. It is recommended that the archive prefix be set to something like '.bz3.run', so that
        // potential users know that they'll need bzip3 to extract it.
        if (this.isTrue(this.bzip3)) {
            args.add("--bzip3");
        }

        // --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs.
        // The pbzip2 command must be available in the command path. It is recommended that the archive prefix be
        // set to something like '.pbz2.run', so that potential users know that they'll need bzip2 to extract it.
        if (this.isTrue(this.pbzip2)) {
            args.add("--pbzip2");
        }

        // --xz : Use xz instead of gzip for better compression. The xz command must be available in the command path.
        // It is recommended that the archive prefix be set to something like '.xz.run' for the archive, so that
        // potential users know that they'll need xz to extract it.
        if (this.isTrue(this.xz)) {
            args.add("--xz");
        }

        // --lzo : Use lzop instead of gzip for better compression. The lzop command must be available in the command
        // path. It is recommended that the archive prefix be set to something like '.lzo.run' for the archive, so
        // that potential users know that they'll need lzop to extract it.
        if (this.isTrue(this.lzo)) {
            args.add("--lzo");
        }

        // --lz4 : Use lz4 instead of gzip for better compression. The lz4 command must be available in the command
        // path. It is recommended that the archive prefix be set to something like '.lz4.run' for the archive, so
        // that potential users know that they'll need lz4 to extract it.
        if (this.isTrue(this.lz4)) {
            args.add("--lz4");
        }

        // --zstd : Use zstd for compression.
        if (this.isTrue(this.zstd)) {
            args.add("--zstd");
        }

        // --pigz : Use pigz for compression.
        if (this.isTrue(this.pigz)) {
            args.add("--pigz");
        }

        // --base64 : Encode the archive to ASCII in Base64 format (base64 command required).
        if (this.isTrue(this.base64)) {
            args.add("--base64");
        }

        // --gpg-encrypt : Encrypt the archive using gpg -ac -z $COMPRESS_LEVEL. This will prompt for a password to
        // encrypt with. Assumes that potential users have gpg installed.
        if (this.isTrue(this.gpgEncrypt)) {
            args.add("--gpg-encrypt");
        }

        // --gpg-asymmetric-encrypt-sign : Instead of compressing, asymmetrically encrypt and sign the data using GPG
        if (this.isTrue(this.gpgAsymmetricEncryptSign)) {
            args.add("--gpg-asymmetric-encrypt-sign");
        }

        // --ssl-encrypt : Encrypt the archive using openssl aes-256-cbc -a -salt. This will prompt for a password to
        // encrypt with. Assumes that the potential users have the OpenSSL tools installed.
        if (this.isTrue(this.sslEncrypt)) {
            args.add("--ssl-encrypt");
        }

        // --ssl-passwd pass : Use the given password to encrypt the data using OpenSSL.
        if (this.sslPasswd != null) {
            args.add("--ssl-passwd");
            args.add(this.sslPasswd);
        }

        // --ssl-pass-src src : Use the given src as the source of password to encrypt the data using OpenSSL. See
        // \"PASS PHRASE ARGUMENTS\" in man openssl. If this option is not supplied, the user wil be asked to enter
        // encryption pasword on the current terminal.
        if (this.sslPassSrc != null) {
            args.add("--ssl-pass-src");
            args.add(this.sslPassSrc);
        }

        // --ssl-no-md : Do not use \"-md\" option not supported by older OpenSSL.
        if (this.isTrue(this.sslNoMd)) {
            args.add("--ssl-no-md");
        }

        // --compress : Use the UNIX compress command to compress the data. This should be the default on all platforms
        // that don't have gzip available.
        if (this.isTrue(this.compress)) {
            args.add("--compress");
        }

        // --complevel : Specify the compression level for gzip, bzip2, bzip3, pbzip2, xz, lzo or lz4. (defaults to 9)
        if (this.complevel != null) {
            args.add("--complevel");
            args.add(this.complevel.toString());
        }

        // --comp-extra : Append extra options to the chosen compressor"
        if (this.compExtra != null) {
            args.add("--comp-extra");
            args.add(this.compExtra);
        }

        // --nochown : Do not give the target folder to the current user (default)
        if (this.isTrue(this.nochown)) {
            args.add("--nochown");
        }

        // --chown : Give the target folder to the current user recursively.
        if (this.isTrue(this.chown)) {
            args.add("--chown");
        }

        // --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
        if (this.isTrue(this.nocomp)) {
            args.add("--nocomp");
        }

        // --threads thds : Number of threads to be used by compressors that support parallelization.
        // Omit to use compressor's default. Most useful (and required) for opting into xz's threading,
        // usually with '--threads=0' for all available cores.pbzip2 and pigz are parallel by default,
        // and setting this value allows limiting the number of threads they use.
        if (this.threads != null) {
            args.add("--threads");
            args.add(this.threads.toString());
        }

        // --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
        // created in the current directory. This is better to distribute software packages that may extract and compile
        // by themselves (i.e. launch the compilation through the embedded script).
        if (this.isTrue(this.notemp)) {
            args.add("--notemp");
        }

        // --needroot : Check that the root user is extracting the archive before proceeding
        if (this.isTrue(this.needroot)) {
            args.add("--needroot");
        }

        // --current : Files will be extracted to the current directory, instead of in a sub-directory. This option
        // implies --notemp and does not require a startup_script.
        if (this.isTrue(this.current)) {
            args.add("--current");
        }

        // --follow : Follow the symbolic links inside of the archive directory, i.e. store the files that are being
        // pointed to instead of the links themselves.
        if (this.isTrue(this.follow)) {
            args.add("--follow");
        }

        // --noprogress : Do not show the progress during the decompression
        if (this.isTrue(this.noprogress)) {
            args.add("--noprogress");
        }

        // --append (new in 2.1.x): Append data to an existing archive, instead of creating a new one. In this mode, the
        // settings from the original archive are reused (compression type, label, embedded script), and thus don't need
        // to be specified again on the command line.
        if (this.isTrue(this.append)) {
            args.add("--append");
        }

        // --header : Makeself 2.0 uses a separate file to store the header stub, called makeself-header.sh. By default,
        // it is assumed that it is stored in the same location as makeself.sh. This option can be used to specify its
        // actual location if it is stored someplace else.
        if (this.headerFile != null) {
            args.add("--header");
            args.add(this.headerFile);
        }

        // --preextract : Specify a pre-extraction script. The script is executed with the same environment and initial
        // `script_args` as `startup_script`.
        if (this.preextractScript != null) {
            args.add("--preextract");
            args.add(this.preextractScript);
        }

        // --cleanup : Specify a script that is run when execution is interrupted or finishes successfully. The script
        // is executed with the same environment and initial `script_args` as `startup_script`.
        if (this.cleanupScript != null) {
            args.add("--cleanup");
            args.add(this.cleanupScript);
        }

        // --copy : Upon extraction, the archive will first extract itself to a temporary directory. The main
        // application of this is to allow self-contained installers stored in a Makeself archive on a CD, when the
        // installer program will later need to unmount the CD and allow a new one to be inserted. This prevents
        // "File system busy" errors for installers that span multiple CDs.
        if (this.isTrue(this.copy)) {
            args.add("--copy");
        }

        // --nox11 : Disable the automatic spawning of a new terminal in X11.
        if (this.isTrue(this.nox11)) {
            args.add("--nox11");
        }

        // --nowait : When executed from a new X11 terminal, disable the user prompt at the end of the script execution.
        if (this.isTrue(this.nowait)) {
            args.add("--nowait");
        }

        // --nomd5 : Disable the creation of a MD5 checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (this.isTrue(this.nomd5)) {
            args.add("--nomd5");
        }

        // --nocrc : Disable the creation of a CRC checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (this.isTrue(this.nocrc)) {
            args.add("--nocrc");
        }

        // --sha256 : Compute a SHA256 checksum for the archive.
        if (this.isTrue(this.sha256)) {
            args.add("--sha256");
        }

        // --lsm file : Provide and LSM file to makeself, that will be embedded in the generated archive. LSM files are
        // describing a software package in a way that is easily parseable. The LSM entry can then be later retrieved
        // using the --lsm argument to the archive. An example of a LSM file is provided
        // with Makeself.
        if (this.lsmFile != null) {
            args.add("--lsm");
            args.add(this.lsmFile);
        }

        // --gpg-extra opt : Append more options to the gpg command line.
        if (this.gpgExtraOpt != null) {
            args.add("--gpg-extra");
            args.add(this.gpgExtraOpt);
        }

        // --tar-format opt : Specify the tar archive format (default is ustar); you may use any value accepted by your
        // tar command (such as posix, v7, etc).
        if (this.tarFormatOpt != null) {
            args.add("--tar-format");
            args.add(this.tarFormatOpt);
        }

        // --tar-extra opt : Append more options to the tar command line.
        // For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar,
        // one can use makeself.sh --tar-extra "--exclude=.git" ...
        if (this.tarExtraOpt != null) {
            args.add("--tar-extra");
            args.add(this.tarExtraOpt);
        }

        // --untar-extra opt : Append more options to the during the extraction of the tar archive.
        if (this.untarExtraOpt != null) {
            args.add("--untar-extra");
            args.add(this.untarExtraOpt);
        }

        // --sign passphrase : Signature private key to sign the package with
        if (this.signPassphrase != null) {
            args.add("--sign");
            args.add(this.signPassphrase);
        }

        // --target dir : Specify the directory where the archive will be extracted. This option implies
        // --notemp and does not require a startup_script.
        if (this.extractTargetDir != null) {
            args.add("--target");
            args.add(this.extractTargetDir);
        }

        // --keep-umask : Keep the umask set to shell default, rather than overriding when executing self-extracting
        // archive.
        if (this.isTrue(this.keepUmask)) {
            args.add("--keep-umask");
        }

        // --export-conf : Export configuration variables to startup_script"
        if (this.isTrue(this.exportConf)) {
            args.add("--export-conf");
        }

        // --packaging-date date : Use provided string as the packaging date instead of the current date.
        if (this.packagingDate != null) {
            args.add("--packaging-date");
            args.add(this.packagingDate);
        }

        // --license : Append a license file.
        if (this.licenseFile != null) {
            args.add("--license");
            args.add(this.licenseFile);
        }

        // --nooverwrite : Do not extract the archive if the specified target directory already exists.
        if (this.isTrue(this.nooverwrite)) {
            args.add("--nooverwrite");
        }

        // --help-header file : Add a header to the archive's --help output.
        if (this.helpHeaderFile != null) {
            args.add("--help-header");
            args.add(this.helpHeaderFile);
        }

        return args;
    }

    private boolean isTrue(final Boolean value) {
        if (value != null) {
            return value.booleanValue();
        }
        return false;
    }

}
