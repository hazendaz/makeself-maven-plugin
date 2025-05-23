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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * The Class MakeselfMojo.
 */
@Mojo(name = "makeself", defaultPhase = LifecyclePhase.VERIFY, requiresProject = false)
public class MakeselfMojo extends AbstractMojo {

    /**
     * isWindows is detected at start of plugin to ensure windows needs.
     */
    private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    /**
     * Permissions for makeself script results.
     */
    private static final String PERMISSIONS = "rwxr-xr--";

    /**
     * Static ATTACH_ARTIFACT to maven lifecycle.
     */
    private static final boolean ATTACH_ARTIFACT = true;

    /**
     * The path to git which is left blank unless portable git is used.
     */
    private String gitPath = "";

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

    /** Maven Artifact Factory. */
    @Inject
    private RepositorySystem repositorySystem;

    /** Maven Project. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Maven Repository System Session. */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    /** Maven Remote Repositories. */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> remoteRepositories;

    /** The makeself. */
    private Path makeself;

    /** Portable Git. */
    private PortableGit portableGit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check if plugin run should be skipped
        if (this.skip) {
            getLog().info("Makeself is skipped");
            return;
        }

        // Validate archive directory exists
        Path path = Path.of(buildTarget.concat(archiveDir));
        if (!Files.exists(path)) {
            throw new MojoExecutionException("ArchiveDir: missing '" + buildTarget.concat(archiveDir) + "'");
        }

        // Validate inline script or startup script file
        if (inlineScript) {
            // Validate inline script has script args
            if (scriptArgs == null) {
                throw new MojoExecutionException("ScriptArgs required when running inlineScript");
            }
        } else {
            // Validate startupScript file starts with './'
            if (!startupScript.startsWith("./")) {
                throw new MojoExecutionException("StartupScript required to start with './'");
            }

            // Validate startupScript file exists
            path = Path.of(buildTarget.concat(archiveDir).concat(startupScript.substring(1)));
            if (!Files.exists(path)) {
                throw new MojoExecutionException("StartupScript: missing '"
                        + buildTarget.concat(archiveDir).concat(startupScript.substring(1)) + "'");
            }
        }

        // Setup make self files
        this.extractMakeself();

        // Check git setup
        if (MakeselfMojo.WINDOWS) {
            this.checkGitSetup();
        }

        try {
            // Output version of bash
            getLog().debug("Execute Bash Version");
            execute(Arrays.asList(gitPath + "bash", "--version"), !ATTACH_ARTIFACT);

            // Output version of makeself.sh
            getLog().debug("Execute Makeself Version");
            execute(Arrays.asList(gitPath + "bash", makeself.toAbsolutePath().toString(), "--version"),
                    !ATTACH_ARTIFACT);

            // If version arguments supplied, exit as we just printed version.
            if (isTrue(version)) {
                return;
            }

            // If help arguments supplied, write output and get out of code.
            if (isTrue(help)) {
                getLog().debug("Execute Makeself Help");
                execute(Arrays.asList(gitPath + "bash", makeself.toAbsolutePath().toString(), "--help"),
                        !ATTACH_ARTIFACT);
                return;
            }

            // Basic Configuration
            getLog().debug("Loading Makeself Basic Configuration");
            List<String> target = new ArrayList<>(
                    Arrays.asList(gitPath + "bash", makeself.toAbsolutePath().toString()));
            target.addAll(loadArgs());
            target.add(buildTarget.concat(archiveDir));
            target.add(buildTarget.concat(fileName));
            target.add(label);
            target.add(startupScript);
            if (scriptArgs != null) {
                target.addAll(scriptArgs);
            }

            // Indicate makeself running
            getLog().info("Running makeself build");

            // Execute main run of makeself.sh
            getLog().debug("Execute Makeself Build");
            execute(target, ATTACH_ARTIFACT);

            // Output info on file makeself created
            getLog().debug("Execute Makeself Info on Resulting Shell Script");
            execute(Arrays.asList(gitPath + "bash", buildTarget.concat(fileName), "--info"), !ATTACH_ARTIFACT);

            // Output list on file makeself created (non windows need)
            if (!MakeselfMojo.WINDOWS) {
                getLog().debug("Execute Makeself List on Resulting Shell Script");
                execute(Arrays.asList(gitPath + "bash", buildTarget.concat(fileName), "--list"), !ATTACH_ARTIFACT);
            }

            // auto run script
            if (this.autoRun) {
                getLog().info("Auto-run created shell (this may take a few minutes)");
                execute(Arrays.asList(gitPath + "bash", buildTarget.concat(fileName)), !ATTACH_ARTIFACT);
            }
        } catch (IOException e) {
            getLog().error("", e);
        } catch (InterruptedException e) {
            getLog().error("", e);
            // restore interruption status of the corresponding thread
            Thread.currentThread().interrupt();
        }
    }

    private void execute(List<String> target, boolean attach) throws IOException, InterruptedException {

        // Log execution target
        getLog().debug("Execution commands: " + target);

        // Create Process Builder
        ProcessBuilder processBuilder = new ProcessBuilder(target);
        processBuilder.redirectErrorStream(true);

        // Add portable git to windows environment
        if (MakeselfMojo.WINDOWS) {
            Map<String, String> envs = processBuilder.environment();
            getLog().debug("Environment Variables: " + envs);
            final String location = repoSession.getLocalRepository().getBasedir() + File.separator
                    + this.portableGit.getName() + File.separator + this.portableGit.getVersion();
            // Windows cmd/powershell shows "Path" in this case
            if (envs.get("Path") != null) {
                envs.put("Path", location + "/usr/bin;" + envs.get("Path"));
                getLog().debug("Environment Path Variable: " + envs.get("Path"));
                // Windows bash shows "PATH" in this case and has issues with spacing as in 'Program Files'
            } else if (envs.get("PATH") != null) {
                envs.put("PATH",
                        location + "/usr/bin;" + envs.get("PATH").replace("Program Files", "\"Program Files\""));
                getLog().debug("Environment Path Variable: " + envs.get("PATH"));
            }
        }

        // Create Process
        Process process = processBuilder.start();

        // Write process output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
            getLog().info("");
        }

        // Wait for process completion
        int status = process.waitFor();
        if (status > 0) {
            getLog().error(String.join(" ", "makeself failed with error status:", String.valueOf(status)));
        }

        // Attach artifact to maven build for install/deploy/release on success
        if (status == 0 && attach) {
            projectHelper.attachArtifact(project, this.extension, this.classifier,
                    Path.of(buildTarget, FilenameUtils.getName(fileName)).toFile());
        }
    }

    /**
     * Extract makeself.
     */
    private void extractMakeself() {
        getLog().debug("Extracting Makeself");

        // Create makeself directory
        Path makeselfTemp = Path.of(makeselfTempDirectory.getAbsolutePath());
        if (!Files.exists(makeselfTemp) && !makeselfTemp.toFile().mkdirs()) {
            getLog().error(String.join(" ", "Unable to make directory", makeselfTempDirectory.getAbsolutePath()));
            return;
        }
        getLog().debug(String.join(" ", "Created directory for", makeselfTempDirectory.getAbsolutePath()));

        ClassLoader classloader = this.getClass().getClassLoader();

        // Write makeself script
        makeself = makeselfTempDirectory.toPath().resolve("makeself.sh");
        if (!Files.exists(makeself)) {
            getLog().debug("Writing makeself.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself.sh")) {
                Path path = makeself.toAbsolutePath();
                Files.copy(link, path);
                setFilePermissions(makeself.toFile());
                setPosixFilePermissions(path);
            } catch (IOException e) {
                getLog().error("", e);
            }
        }

        // Write makeself-header script
        Path makeselfHeader = makeselfTempDirectory.toPath().resolve("makeself-header.sh");
        if (!Files.exists(makeselfHeader)) {
            getLog().debug("Writing makeself-header.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself-header.sh")) {
                Path path = makeselfHeader.toAbsolutePath();
                Files.copy(link, path);
                setFilePermissions(makeselfHeader.toFile());
                setPosixFilePermissions(path);
            } catch (IOException e) {
                getLog().error("", e);
            }
        }
    }

    /**
     * Check Git Setup.
     *
     * @throws MojoFailureException
     *             the mojo failure exception
     */
    private void checkGitSetup() throws MojoFailureException {
        // Get Portable Git Maven Information
        this.portableGit = new PortableGit(getLog());

        // Extract Portable Git
        this.extractPortableGit();
    }

    /**
     * Extract Portable Git.
     *
     * @throws MojoFailureException
     *             failure retrieving portable git
     */
    private void extractPortableGit() throws MojoFailureException {
        final String location = repoSession.getLocalRepository().getBasedir() + File.separator
                + this.portableGit.getName() + File.separator + this.portableGit.getVersion();
        if (Files.exists(Path.of(location))) {
            getLog().debug("Existing 'PortableGit' folder found at " + location);
            gitPath = location + "/usr/bin/";
            return;
        }

        getLog().info("Loading portable git");
        final Artifact artifact = new DefaultArtifact(this.portableGit.getGroupId(), this.portableGit.getArtifactId(),
                this.portableGit.getClassifier(), this.portableGit.getExtension(), this.portableGit.getVersion());
        final ArtifactRequest artifactRequest = new ArtifactRequest().setRepositories(this.remoteRepositories)
                .setArtifact(artifact);
        ArtifactResult resolutionResult = null;
        try {
            resolutionResult = repositorySystem.resolveArtifact(repoSession, artifactRequest);
            if (!resolutionResult.isResolved()) {
                throw new MojoFailureException("Unable to resolve artifact: " + artifact.getGroupId() + ":"
                        + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getClassifier() + ":"
                        + artifact.getExtension());
            }
        } catch (ArtifactResolutionException e) {
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
    private void installGit(final Artifact artifact, final String location) {
        Path currentFile = null;

        // Unzip 'git-for-windows-*-portable.tar.gz' from '.m2/repository/com/github/hazendaz/git/git-for-windows'
        // into '.m2/repository/PortableGit'
        try (InputStream inputStream = Files.newInputStream(artifact.getFile().toPath());
                InputStream bufferedStream = new BufferedInputStream(inputStream);
                InputStream gzipStream = new GzipCompressorInputStream(bufferedStream);
                ArchiveInputStream<TarArchiveEntry> tarStream = new TarArchiveInputStream(gzipStream)) {
            ArchiveEntry entry;
            String directory = repoSession.getLocalRepository().getBasedir() + File.separator
                    + this.portableGit.getName();
            while ((entry = tarStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                currentFile = Path.of(directory, entry.getName());
                if (!currentFile.normalize().startsWith(directory)) {
                    throw new IOException("Bad zip entry, possible directory traversal");
                }
                Path parent = currentFile.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
                getLog().debug("Current file: " + currentFile.getFileName());
                Files.copy(tarStream, currentFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            getLog().error("", e);
        }

        try {
            if (currentFile != null) {
                // Extract Portable Git
                getLog().debug("Extract Portable Git");
                execute(Arrays.asList(currentFile.toString(), "-y", "-o", location), !ATTACH_ARTIFACT);
                gitPath = location + "/usr/bin/";
            }
        } catch (IOException e) {
            getLog().error("", e);
        } catch (InterruptedException e) {
            getLog().error("", e);
            // restore interruption status of the corresponding thread
            Thread.currentThread().interrupt();
        }
    }

    private void setFilePermissions(File file) {
        if (!file.setExecutable(true, true)) {
            getLog().error(String.join(" ", "Unable to set executable:", file.getName()));
        } else {
            getLog().debug(String.join(" ", "Set executable for", file.getName()));
        }
    }

    private void setPosixFilePermissions(Path path) {
        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(PERMISSIONS);

        try {
            Files.setPosixFilePermissions(path, permissions);
            getLog().debug(String.join(" ", "Set Posix File Permissions for", path.toString(), "as", PERMISSIONS));
        } catch (IOException e) {
            getLog().error("Failed attempted Posix permissions", e);
        } catch (UnsupportedOperationException e) {
            // Attempting but don't care about status if it fails
            getLog().debug("Failed attempted Posix permissions", e);
        }
    }

    /**
     * Load args.
     *
     * @return the string
     */
    private List<String> loadArgs() {
        getLog().debug("Loading arguments");

        List<String> args = new ArrayList<>(50);

        // " --tar-quietly : Suppress verbose output from the tar command"
        if (isTrue(tarQuietly)) {
            args.add("--tar-quietly");
        }

        // " --quiet | -q : Do not print any messages other than errors."
        if (isTrue(quiet)) {
            args.add("--quiet");
        }

        // --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux)
        if (isTrue(gzip)) {
            args.add("--gzip");
        }

        // --bzip2 : Use bzip2 instead of gzip for better compression. The bzip2 command must be available in the
        // command path. It is recommended that the archive prefix be set to something like '.bz2.run', so that
        // potential users know that they'll need bzip2 to extract it.
        if (isTrue(bzip2)) {
            args.add("--bzip2");
        }

        // --bzip3 : Use bzip3 instead of gzip for better compression. The bzip3 command must be available in the
        // command path. It is recommended that the archive prefix be set to something like '.bz3.run', so that
        // potential users know that they'll need bzip3 to extract it.
        if (isTrue(bzip3)) {
            args.add("--bzip3");
        }

        // --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs.
        // The pbzip2 command must be available in the command path. It is recommended that the archive prefix be
        // set to something like '.pbz2.run', so that potential users know that they'll need bzip2 to extract it.
        if (isTrue(pbzip2)) {
            args.add("--pbzip2");
        }

        // --xz : Use xz instead of gzip for better compression. The xz command must be available in the command path.
        // It is recommended that the archive prefix be set to something like '.xz.run' for the archive, so that
        // potential users know that they'll need xz to extract it.
        if (isTrue(xz)) {
            args.add("--xz");
        }

        // --lzo : Use lzop instead of gzip for better compression. The lzop command must be available in the command
        // path. It is recommended that the archive prefix be set to something like '.lzo.run' for the archive, so
        // that potential users know that they'll need lzop to extract it.
        if (isTrue(lzo)) {
            args.add("--lzo");
        }

        // --lz4 : Use lz4 instead of gzip for better compression. The lz4 command must be available in the command
        // path. It is recommended that the archive prefix be set to something like '.lz4.run' for the archive, so
        // that potential users know that they'll need lz4 to extract it.
        if (isTrue(lz4)) {
            args.add("--lz4");
        }

        // --zstd : Use zstd for compression.
        if (isTrue(zstd)) {
            args.add("--zstd");
        }

        // --pigz : Use pigz for compression.
        if (isTrue(pigz)) {
            args.add("--pigz");
        }

        // --base64 : Encode the archive to ASCII in Base64 format (base64 command required).
        if (isTrue(base64)) {
            args.add("--base64");
        }

        // --gpg-encrypt : Encrypt the archive using gpg -ac -z $COMPRESS_LEVEL. This will prompt for a password to
        // encrypt with. Assumes that potential users have gpg installed.
        if (isTrue(gpgEncrypt)) {
            args.add("--gpg-encrypt");
        }

        // --gpg-asymmetric-encrypt-sign : Instead of compressing, asymmetrically encrypt and sign the data using GPG
        if (isTrue(gpgAsymmetricEncryptSign)) {
            args.add("--gpg-asymmetric-encrypt-sign");
        }

        // --ssl-encrypt : Encrypt the archive using openssl aes-256-cbc -a -salt. This will prompt for a password to
        // encrypt with. Assumes that the potential users have the OpenSSL tools installed.
        if (isTrue(sslEncrypt)) {
            args.add("--ssl-encrypt");
        }

        // --ssl-passwd pass : Use the given password to encrypt the data using OpenSSL.
        if (sslPasswd != null) {
            args.add("--ssl-passwd");
            args.add(sslPasswd);
        }

        // --ssl-pass-src src : Use the given src as the source of password to encrypt the data using OpenSSL. See
        // \"PASS PHRASE ARGUMENTS\" in man openssl. If this option is not supplied, the user wil be asked to enter
        // encryption pasword on the current terminal.
        if (sslPassSrc != null) {
            args.add("--ssl-pass-src");
            args.add(sslPassSrc);
        }

        // --ssl-no-md : Do not use \"-md\" option not supported by older OpenSSL.
        if (isTrue(sslNoMd)) {
            args.add("--ssl-no-md");
        }

        // --compress : Use the UNIX compress command to compress the data. This should be the default on all platforms
        // that don't have gzip available.
        if (isTrue(compress)) {
            args.add("--compress");
        }

        // --complevel : Specify the compression level for gzip, bzip2, bzip3, pbzip2, xz, lzo or lz4. (defaults to 9)
        if (complevel != null) {
            args.add("--complevel");
            args.add(complevel.toString());
        }

        // --nochown : Do not give the target folder to the current user (default)
        if (isTrue(nochown)) {
            args.add("--nochown");
        }

        // --chown : Give the target folder to the current user recursively.
        if (isTrue(chown)) {
            args.add("--chown");
        }

        // --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
        if (isTrue(nocomp)) {
            args.add("--nocomp");
        }

        // --threads thds : Number of threads to be used by compressors that support parallelization.
        // Omit to use compressor's default. Most useful (and required) for opting into xz's threading,
        // usually with '--threads=0' for all available cores.pbzip2 and pigz are parallel by default,
        // and setting this value allows limiting the number of threads they use.
        if (threads != null) {
            args.add("--threads");
            args.add(threads.toString());
        }

        // --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
        // created in the current directory. This is better to distribute software packages that may extract and compile
        // by themselves (i.e. launch the compilation through the embedded script).
        if (isTrue(notemp)) {
            args.add("--notemp");
        }

        // --needroot : Check that the root user is extracting the archive before proceeding
        if (isTrue(needroot)) {
            args.add("--needroot");
        }

        // --current : Files will be extracted to the current directory, instead of in a sub-directory. This option
        // implies --notemp and does not require a startup_script.
        if (isTrue(current)) {
            args.add("--current");
        }

        // --follow : Follow the symbolic links inside of the archive directory, i.e. store the files that are being
        // pointed to instead of the links themselves.
        if (isTrue(follow)) {
            args.add("--follow");
        }

        // --noprogress : Do not show the progress during the decompression
        if (isTrue(noprogress)) {
            args.add("--noprogress");
        }

        // --append (new in 2.1.x): Append data to an existing archive, instead of creating a new one. In this mode, the
        // settings from the original archive are reused (compression type, label, embedded script), and thus don't need
        // to be specified again on the command line.
        if (isTrue(append)) {
            args.add("--append");
        }

        // --header : Makeself 2.0 uses a separate file to store the header stub, called makeself-header.sh. By default,
        // it is assumed that it is stored in the same location as makeself.sh. This option can be used to specify its
        // actual location if it is stored someplace else.
        if (headerFile != null) {
            args.add("--header");
            args.add(headerFile);
        }

        // --preextract : Specify a pre-extraction script. The script is executed with the same environment and initial
        // `script_args` as `startup_script`.
        if (preextractScript != null) {
            args.add("--reextract");
            args.add(preextractScript);
        }

        // --cleanup : Specify a script that is run when execution is interrupted or finishes successfully. The script
        // is executed with the same environment and initial `script_args` as `startup_script`.
        if (cleanupScript != null) {
            args.add("--cleanup");
            args.add(cleanupScript);
        }

        // --copy : Upon extraction, the archive will first extract itself to a temporary directory. The main
        // application of this is to allow self-contained installers stored in a Makeself archive on a CD, when the
        // installer program will later need to unmount the CD and allow a new one to be inserted. This prevents
        // "File system busy" errors for installers that span multiple CDs.
        if (isTrue(copy)) {
            args.add("--copy");
        }

        // --nox11 : Disable the automatic spawning of a new terminal in X11.
        if (isTrue(nox11)) {
            args.add("--nox11");
        }

        // --nowait : When executed from a new X11 terminal, disable the user prompt at the end of the script execution.
        if (isTrue(nowait)) {
            args.add("--nowait");
        }

        // --nomd5 : Disable the creation of a MD5 checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (isTrue(nomd5)) {
            args.add("--nomd5");
        }

        // --nocrc : Disable the creation of a CRC checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (isTrue(nocrc)) {
            args.add("--nocrc");
        }

        // --sha256 : Compute a SHA256 checksum for the archive.
        if (isTrue(sha256)) {
            args.add("--sha256");
        }

        // --lsm file : Provide and LSM file to makeself, that will be embedded in the generated archive. LSM files are
        // describing a software package in a way that is easily parseable. The LSM entry can then be later retrieved
        // using the --lsm argument to the archive. An example of a LSM file is provided
        // with Makeself.
        if (lsmFile != null) {
            args.add("--lsm");
            args.add(lsmFile);
        }

        // --gpg-extra opt : Append more options to the gpg command line.
        if (gpgExtraOpt != null) {
            args.add("--gpg-extra");
            args.add(gpgExtraOpt);
        }

        // --tar-format opt : Specify the tar archive format (default is ustar); you may use any value accepted by your
        // tar command (such as posix, v7, etc).
        if (tarFormatOpt != null) {
            args.add("--tar-format");
            args.add(tarFormatOpt);
        }

        // --tar-extra opt : Append more options to the tar command line.
        // For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar,
        // one can use makeself.sh --tar-extra "--exclude=.git" ...
        if (tarExtraOpt != null) {
            args.add("--tar-extra");
            args.add(tarExtraOpt);
        }

        // --untar-extra opt : Append more options to the during the extraction of the tar archive.
        if (untarExtraOpt != null) {
            args.add("--untar-extra");
            args.add(untarExtraOpt);
        }

        // --sign passphrase : Signature private key to sign the package with
        if (signPassphrase != null) {
            args.add("--sign");
            args.add(signPassphrase);
        }

        // --target dir : Specify the directory where the archive will be extracted. This option implies
        // --notemp and does not require a startup_script.
        if (extractTargetDir != null) {
            args.add("--target");
            args.add(extractTargetDir);
        }

        // --keep-umask : Keep the umask set to shell default, rather than overriding when executing self-extracting
        // archive.
        if (isTrue(keepUmask)) {
            args.add("--keep-umask");
        }

        // --export-conf : Export configuration variables to startup_script"
        if (isTrue(exportConf)) {
            args.add("--export-conf");
        }

        // --packaging-date date : Use provided string as the packaging date instead of the current date.
        if (packagingDate != null) {
            args.add("--packaging-date");
            args.add(packagingDate);
        }

        // --license : Append a license file.
        if (licenseFile != null) {
            args.add("--license");
            args.add(licenseFile);
        }

        // --nooverwrite : Do not extract the archive if the specified target directory already exists.
        if (isTrue(nooverwrite)) {
            args.add("--nooverwrite");
        }

        // --help-header file : Add a header to the archive's --help output.
        if (helpHeaderFile != null) {
            args.add("--help-header");
            args.add(helpHeaderFile);
        }

        return args;
    }

    private boolean isTrue(Boolean value) {
        if (value != null) {
            return value.booleanValue();
        }
        return false;
    }

}
