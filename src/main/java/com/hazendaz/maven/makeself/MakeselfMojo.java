/**
 *    Copyright 2011-2018 the original author or authors.
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
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * The Class MakeselfMojo.
 */
@Mojo(name = "makeself", defaultPhase = LifecyclePhase.VERIFY, requiresProject = false)
public class MakeselfMojo extends AbstractMojo {

    /**
     * archive_dir is the name of the directory that contains the files to be archived.
     */
    @Parameter(defaultValue = "makeself", property = "archiveDir", required = true)
    private String archiveDir;

    /**
     * file_name is the name of the archive to be created.
     */
    @Parameter(defaultValue = "makeself", property = "fileName", required = true)
    private String fileName;

    /**
     * label is an arbitrary text string describing the package. It will be displayed while extracting the files.
     */
    @Parameter(defaultValue = "Make self-extrabable archives", property = "label", required = true)
    private String label;

    /**
     * startup_script is the command to be executed from within the directory of extracted files. Thus, if you wish to
     * execute a program contain in this directory, you must prefix your command with './'. For example, './program'
     * will be fine.
     */
    @Parameter(defaultValue = "makeself.sh", property = "startupScript", required = true)
    private String startupScript;

    /**
     * script_args are additional arguments for startup_script.
     */
    @Parameter(property = "scriptArgs")
    private String scriptArgs;

    /**
     * --version | -v : Prints the version number on stdout, then exits immediately. Internally will display on all
     * runs.
     */
    @Parameter(property = "version", readonly = true)
    private Boolean version;

    /** --help | -h : Print out this help message. */
    @Parameter(property = "help")
    private Boolean help;

    /**
     * --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux)
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
     * --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs. The
     * pbzip2 command must be available in the command path. It is recommended that the archive prefix be set to
     * something like '.bz2.run', so that potential users know that they'll need bzip2 to extract it.
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

    /** --pigz : Use pigz for compression. */
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
     * --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
     */
    @Parameter(property = "nocomp")
    private Boolean nocomp;

    /**
     * --complevel : Specify the compression level for gzip, bzip2, pbzip2, xz, lzo or lz4. (defaults to 9).
     */
    @Parameter(property = "complevel")
    private Integer complevel;

    /**
     * --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
     * created in the current directory. This is better to distribute software packages that may extract and compile by
     * themselves (i.e. launch the compilation through the embedded script).
     */
    @Parameter(property = "notemp")
    private Boolean notemp;

    /**
     * --current : Files will be extracted to the current directory, instead of in a subdirectory. This option implies
     * --notemp above.
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
    @Parameter(property = "headerFile", readonly = true)
    private Boolean headerFile;

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

    /** --nox11 : Disable the automatic spawning of a new terminal in X11. */
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
     * --tar-extra opt : Append more options to the tar command line.
     *
     * For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar, one
     * can use makeself.sh --tar-extra "--exclude=.git" ...
     */
    @Parameter(property = "tarExtraOpt")
    private String tarExtraOpt;

    /**
     * --untar-extra opt : AAppend more options to the during the extraction of the tar archive.
     */
    @Parameter(property = "untarExtraOpt")
    private String untarExtraOpt;

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
    @Parameter(defaultValue = "false", alias = "skip", property = "skip")
    private Boolean skip;

    /** Auto run : When set to true, resulting shell will be run. This is useful for testing purposes. */
    @Parameter(defaultValue = "false", property = "autoRun")
    private Boolean autoRun;

    /** The build target. */
    @Parameter(defaultValue = "${project.build.directory}/", readonly = true)
    private String buildTarget;

    /** The target directory. */
    @Parameter(defaultValue = "${project.build.directory}/makeself-tmp", readonly = true)
    private File targetDirectory;

    /** Maven ProjectHelper. */
    @Component
    private MavenProjectHelper projectHelper;

    /** Maven Project. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** The makeself. */
    private File makeself;

    /** Static ATTACH_ARTIFACT to maven lifecycle. */
    private static final boolean ATTACH_ARTIFACT = true;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check if plugin run should be skipped
        if (this.skip) {
            getLog().info("Makeself is skipped");
            return;
        }

        // Setup make self files
        this.extractMakeself();

        try {
            // Output version of bash
            getLog().debug("Execute Bash Version");
            execute(Arrays.asList("bash", "--version"), !ATTACH_ARTIFACT);

            // Output version of makeself.sh
            getLog().debug("Execute Makeself Version");
            execute(Arrays.asList("bash", makeself.getAbsolutePath(), "--version"), !ATTACH_ARTIFACT);

            // If help arguments supplied, write output and get out of code.
            String helpArgs = helpArgs();
            if (!helpArgs.isEmpty()) {
                getLog().debug("Execute Makeself Help");
                execute(Arrays.asList("bash", makeself.getAbsolutePath(), helpArgs), !ATTACH_ARTIFACT);
                return;
            }

            // Basic Configuration
            getLog().debug("Loading Makeself Basic Configuration");
            List<String> target = new ArrayList<>();
            target.addAll(Arrays.asList("bash", makeself.getAbsolutePath()));
            target.addAll(loadArgs());
            target.add(buildTarget.concat(archiveDir));
            target.add(buildTarget.concat(fileName));
            target.add(label);
            target.add(startupScript);
            if (scriptArgs != null) {
                target.addAll(Arrays.asList(scriptArgs.split("\\s+")));
            }

            // Indicate makeself running
            getLog().info("Running makeself build");

            // Execute main run of makeself.sh
            getLog().debug("Execute Makeself Build");
            execute(target, ATTACH_ARTIFACT);

            // Output info on file makeself created
            getLog().debug("Execute Makeself Info on Resulting Shell Script");
            execute(Arrays.asList("bash", buildTarget.concat(fileName), "--info"), !ATTACH_ARTIFACT);

            // Output list on file makeself created
            getLog().debug("Execute Makeself List on Resulting Shell Script");
            execute(Arrays.asList("bash", buildTarget.concat(fileName), "--list"), !ATTACH_ARTIFACT);

            // auto run script
            if (this.autoRun) {
                getLog().info("Auto-run created shell (this may take a few minutes)");
                execute(Arrays.asList("bash", buildTarget.concat(fileName)), !ATTACH_ARTIFACT);
            }
        } catch (IOException e) {
            getLog().error("", e);
            if (e.getMessage().contains("Cannot run program \"bash\"")) {
                throw new MojoFailureException(
                        "Configure Bash or Add git for windows '/usr/bin' to environment 'Path' variable to execute this plugin");
            }
        } catch (InterruptedException e) {
            getLog().error("", e);
            // restore interruption status of the corresponding thread
            Thread.currentThread().interrupt();
        }
    }

    private void execute(List<String> target, boolean attach) throws IOException, InterruptedException {
        // Create Process Builder
        ProcessBuilder processBuilder = new ProcessBuilder(target);
        processBuilder.redirectErrorStream(true);

        // Create Process
        Process process = processBuilder.start();

        // Wait for process completion
        int status = process.waitFor();
        if (status > 0) {
            getLog().error("makeself failed with error status: " + status);
        }

        // Write process output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }
            getLog().info("");
        }

        // Attach artifact to maven build for install/deploy/release on success
        if (status == 0 && attach) {
            projectHelper.attachArtifact(project, "sh", new File(buildTarget.concat(fileName)));
        }
    }

    /**
     * Extract makeself.
     */
    private void extractMakeself() {
        getLog().debug("Extracting Makeself");

        // Create makeself directory
        File makeselfTemp = new File(targetDirectory.getAbsolutePath());
        if (!makeselfTemp.exists() && !makeselfTemp.mkdir()) {
            getLog().error("Unable to make directory" + targetDirectory.getAbsolutePath());
            return;
        } else {
            getLog().debug("Created directory for " + targetDirectory.getAbsolutePath());
        }

        ClassLoader classloader = this.getClass().getClassLoader();

        // Write makeself script
        makeself = new File(targetDirectory + "/makeself.sh");
        if (!makeself.exists()) {
            getLog().debug("Writing makeself.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself.sh")) {
                Files.copy(link, makeself.getAbsoluteFile().toPath());
                setFilePermissions(makeself);
                setPosixFilePermissions(makeself.getAbsoluteFile().toPath());
            } catch (IOException e) {
                getLog().error("", e);
            }
        }

        // Write makeself-header script
        File makeselfHeader = new File(targetDirectory + "/makeself-header.sh");
        if (!makeselfHeader.exists()) {
            getLog().debug("Writing makeself-header.sh");
            try (InputStream link = classloader.getResourceAsStream("META-INF/makeself/makeself-header.sh")) {
                Files.copy(link, makeselfHeader.getAbsoluteFile().toPath());
                setFilePermissions(makeselfHeader);
                setPosixFilePermissions(makeselfHeader.getAbsoluteFile().toPath());
            } catch (IOException e) {
                getLog().error("", e);
            }
        }
    }

    private void setFilePermissions(File file) {
        if (!file.setExecutable(true, true)) {
            getLog().error("Unable to set executable: " + file.getName());
        } else {
            getLog().debug("Set executable for " + file.getName());
        }
    }

    private void setPosixFilePermissions(Path path) {
        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr--");

        try {
            Files.setPosixFilePermissions(path, permissions);
            getLog().debug("Set Posix File Permissions for " + path + " as " + permissions);
        } catch (IOException e) {
            getLog().error("Failed attempted Posix permissions", e);
        } catch (UnsupportedOperationException e) {
            // Attempting but don't care about status if it fails
            getLog().debug("Failed attempted Posix permissions", e);
        }
    }

    /**
     * Help args.
     *
     * @return the string
     */
    private String helpArgs() {
        getLog().debug("Loading help arguments");

        StringBuilder args = new StringBuilder();

        // --help | -h : Print out this help message
        if (isTrue(help)) {
            args.append("--help ");
        }
        return args.toString();
    }

    /**
     * Load args.
     *
     * @return the string
     */
    private List<String> loadArgs() {
        getLog().debug("Loading arguments");

        List<String> args = new ArrayList<>();

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

        // --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs.
        // The pbzip2 command must be available in the command path. It is recommended that the archive prefix be
        // set to something like '.bz2.run', so that potential users know that they'll need bzip2 to extract it.
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
        if (sslPasswd != null) {
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

        // --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
        if (isTrue(nocomp)) {
            args.add("--nocomp");
        }

        // --complevel : Specify the compression level for gzip, bzip2, pbzip2, xz, lzo or lz4. (defaults to 9)
        if (complevel != null) {
            args.add("--complevel");
            args.add(complevel.toString());
        }

        // --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
        // created in the current directory. This is better to distribute software packages that may extract and compile
        // by themselves (i.e. launch the compilation through the embedded script).
        if (isTrue(notemp)) {
            args.add("--notemp");
        }

        // --current : Files will be extracted to the current directory, instead of in a sub-directory. This option
        // implies --notemp above.
        if (isTrue(current)) {
            args.add("--current");
        }

        // --follow : Follow the symbolic links inside of the archive directory, i.e. store the files that are being
        // pointed to instead of the links themselves.
        if (isTrue(follow)) {
            args.add("--follow");
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
            args.add(headerFile.toString());
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
            args.add("--helpHeaderFile");
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
