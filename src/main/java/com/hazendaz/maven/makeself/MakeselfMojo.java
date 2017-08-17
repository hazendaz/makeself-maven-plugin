/**
 *    Copyright 2011-2017 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The Class MakeselfMojo.
 */
@Mojo(name = "makeself", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class MakeselfMojo extends AbstractMojo {

    /**
     * archive_dir is the name of the directory that contains the files to be archived.
     */
    @Parameter(defaultValue = "makeself", property = "archiveDir", required = true)
    private String  archiveDir;

    /**
     * file_name is the name of the archive to be created.
     */
    @Parameter(defaultValue = "makeself", property = "fileName", required = true)
    private String  fileName;

    /**
     * label is an arbitrary text string describing the package. It will be displayed while extracting the files.
     */
    @Parameter(defaultValue = "Make self-extrabable archives", property = "label", required = true)
    private String  label;

    /**
     * startup_script is the command to be executed from within the directory of extracted files. Thus, if you wish to
     * execute a program contain in this directory, you must prefix your command with './'. For example, './program'
     * will be fine. The script_args are additional arguments for this command.
     */
    @Parameter(defaultValue = "makeself.sh", property = "startupScript", required = true)
    private String  startupScript;

    /** --version | -v : Prints the version number on stdout, then exits immediately. Internally will display on all runs. */
    @Parameter(property = "version", readonly = true)
    private Boolean version;

    /** --help | -h : Print out this help message. */
    @Parameter(property = "help")
    private Boolean help;

    /** --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux) */
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

    /** --base64 : Encode the archive to ASCII in Base64 format (base64 command required). */
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
     * --compress : Use the UNIX compress command to compress the data. This should be the default on all platforms that
     * don't have gzip available.
     */
    @Parameter(property = "compress")
    private Boolean compress;

    /** --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR. */
    @Parameter(property = "nocomp")
    private Boolean nocomp;

    /** --complevel : Specify the compression level for gzip, bzip2, pbzip2, xz, lzo or lz4. (defaults to 9). */
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
     * --lsm file : Provide and LSM file to makeself, that will be embedded in the generated archive. LSM files are
     * describing a software package in a way that is easily parseable. The LSM entry can then be later retrieved using
     * the --lsm argument to the archive. An example of a LSM file is provided with Makeself.
     */
    @Parameter(property = "lsmFile")
    private String  lsmFile;

    /**
     * --gpg-extra opt : Append more options to the gpg command line.
     */
    @Parameter(property = "gpgExtraOpt")
    private String  gpgExtraOpt;

    /**
     * --tar-extra opt : Append more options to the tar command line.
     *
     * For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar, one
     * can use makeself.sh --tar-extra "--exclude=.git" ...
     */
    @Parameter(property = "tarExtraOpt")
    private String  tarExtraOpt;

    /**
     * --untar-extra opt : AAppend more options to the during the extraction of the tar archive.
     */
    @Parameter(property = "untarExtraOpt")
    private String  untarExtraOpt;

    /**
     * --keep-umask : Keep the umask set to shell default, rather than overriding when executing self-extracting
     * archive.
     */
    @Parameter(property = "keepUmask")
    private Boolean keepUmask;

    /**
     * --packaging-date date : Use provided string as the packaging date instead of the current date.
     */
    @Parameter(property = "packagingDate")
    private String  packagingDate;

    /**
     * --license : Append a license file.
     */
    @Parameter(property = "licenseFile")
    private String  licenseFile;

    /**
     * --nooverwrite : Do not extract the archive if the specified target directory already exists.
     */
    @Parameter(property = "nooverwrite")
    private Boolean nooverwrite;

    /**
     * --help-header file : Add a header to the archive's --help output.
     */
    @Parameter(property = "helpHeaderFile")
    private String  helpHeaderFile;

    @Parameter(defaultValue = "false", alias = "skip", property = "skip")
    private Boolean skip;

    /** The target directory. */
    @Parameter(defaultValue = "${project.build.directory}/makeself-temp", readonly = true)
    private File    targetDirectory;

    //** The build target. */
    @Parameter(defaultValue = "${project.build.directory}/", readonly = true)
    private String  buildTarget;

    /** The makeself. */
    private File    makeself;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            getLog().info("Formatting is skipped");
            return;
        }

        // Setup make self files
        this.extractMakeself();

        try {
            // Location of makeself.sh
            String makeselfTarget = "bash " + makeself.getAbsolutePath() + " ";

            // Output version of bash
            execute("bash " + "--version");

            // Output version of makeself.sh
            execute(makeselfTarget + "--version");

            // If help arguments supplied, write output and get out of code.
            String helpArgs = helpArgs();
            if (!helpArgs.isEmpty()) {
                execute(makeselfTarget + helpArgs);
                return;
            }

            // Basic Configuration
            String target = makeselfTarget + loadArgs() + buildTarget + archiveDir + " " + buildTarget + fileName
                    + " \"" + label + "\" " + startupScript;

            // Output Executed Command
            getLog().debug("### " + target);

            // Execute main run of makeself.sh
            execute(target);
        } catch (IOException | InterruptedException e) {
            if (e.getMessage().contains("Cannot run program \"bash\"")) {
                getLog().error(
                        "Configure Bash or Add git for windows '/usr/bin' to environment 'Path' variable to execute this plugin",
                        e);
            } else {
                getLog().error("", e);
            }
        }
    }

    private void execute(String target) throws IOException, InterruptedException {
        String commands[] = target.split(" ");
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int status = process.waitFor();
        if (status > 0) {
            getLog().info("makeself failed with error status: " + status);
        }
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        getLog().info("### " + output);
    }

    /**
     * Extract makeself.
     */
    private void extractMakeself() {
        ClassLoader classloader = this.getClass().getClassLoader();

        final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr--");

        try {
            File makeselfTemp = new File(targetDirectory.getAbsolutePath());
            if (!makeselfTemp.exists()) {
                makeselfTemp.mkdir();
            }

            makeself = new File(targetDirectory + "/makeself.sh");
            if (!makeself.exists()) {
                makeself.setExecutable(true, true);
                try (InputStream link = classloader.getResourceAsStream("makeself.sh")) {
                    Files.copy(link, makeself.getAbsoluteFile().toPath());
                }
                tryPosixFilePermissions(makeself.getAbsoluteFile().toPath(), perms);
            }

            File makeselfHeader = new File(targetDirectory + "/makeself-header.sh");
            if (!makeselfHeader.exists()) {
                makeselfHeader.setExecutable(true, true);
                try (InputStream link = classloader.getResourceAsStream("makeself-header.sh")) {
                    Files.copy(link, makeselfHeader.getAbsoluteFile().toPath());
                }
            tryPosixFilePermissions(makeselfHeader.getAbsoluteFile().toPath(), perms);
            }
        } catch (IOException e) {
            getLog().error("", e);
        }
    }

    private void tryPosixFilePermissions(Path path, Set<PosixFilePermission> perms) {
        try {
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException e) {
            getLog().error("", e);
        } catch (UnsupportedOperationException e) {
            // Attempting but don't care about status if it fails
            getLog().debug("", e);
        }
    }

    /**
     * Help args.
     *
     * @return the string
     */
    private String helpArgs() {
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
    private String loadArgs() {
        StringBuilder args = new StringBuilder();

        // --gzip : Use gzip for compression (the default on platforms on which gzip is commonly available, like Linux)
        if (isTrue(gzip)) {
            args.append("--gzip ");
        }

        // --bzip2 : Use bzip2 instead of gzip for better compression. The bzip2 command must be available in the
        // command path. It
        // is recommended that the archive prefix be set to something like '.bz2.run', so that potential users know that
        // they'll need bzip2 to extract it.
        if (isTrue(bzip2)) {
            args.append("--bzip2 ");
        }

        // --pbzip2 : Use pbzip2 instead of gzip for better and faster compression on machines having multiple CPUs. The
        // pbzip2
        // command must be available in the command path. It is recommended that the archive prefix be set to something
        // like '.bz2.run', so that potential users know that they'll need bzip2 to extract it.
        if (isTrue(pbzip2)) {
            args.append("--pbzip2 ");
        }

        // --xz : Use xz instead of gzip for better compression. The xz command must be available in the command path.
        // It is
        // recommended that the archive prefix be set to something like '.xz.run' for the archive, so that potential
        // users know that they'll need xz to extract it.
        if (isTrue(xz)) {
            args.append("--xz ");
        }

        // --lzo : Use lzop instead of gzip for better compression. The lzop command must be available in the command
        // path. It
        // is recommended that the archive prefix be set to something like '.lzo.run' for the archive, so that potential
        // users know that they'll need lzop to extract it.
        if (isTrue(lzo)) {
            args.append("--lzo ");
        }

        // --lz4 : Use lz4 instead of gzip for better compression. The lz4 command must be available in the command
        // path. It is
        // recommended that the archive prefix be set to something like '.lz4.run' for the archive, so that potential
        // users know that they'll need lz4 to extract it.
        if (isTrue(lz4)) {
            args.append("--lz4 ");
        }

        // --pigz : Use pigz for compression.
        if (isTrue(pigz)) {
            args.append("--pigz ");
        }

        // --base64 : Encode the archive to ASCII in Base64 format (base64 command required).
        if (isTrue(base64)) {
            args.append("--base64 ");
        }

        // --gpg-encrypt : Encrypt the archive using gpg -ac -z $COMPRESS_LEVEL. This will prompt for a password to
        // encrypt with. Assumes that potential users have gpg installed.
        if (isTrue(gpgEncrypt)) {
            args.append("--gpg-encrypt ");
        }

        // --gpg-asymmetric-encrypt-sign : Instead of compressing, asymmetrically encrypt and sign the data using GPG
        if (isTrue(gpgAsymmetricEncryptSign)) {
            args.append("--gpg-asymmetric-encrypt-sign ");
        }

        // --ssl-encrypt : Encrypt the archive using openssl aes-256-cbc -a -salt. This will prompt for a password to
        // encrypt with. Assumes that the potential users have the OpenSSL tools installed.
        if (isTrue(sslEncrypt)) {
            args.append("--ssl-encrypt ");
        }

        // --compress : Use the UNIX compress command to compress the data. This should be the default on all platforms
        // that don't have gzip available.
        if (isTrue(compress)) {
            args.append("--compress ");
        }

        // --nocomp : Do not use any compression for the archive, which will then be an uncompressed TAR.
        if (isTrue(nocomp)) {
            args.append("--nocomp ");
        }

        // --complevel : Specify the compression level for gzip, bzip2, pbzip2, xz, lzo or lz4. (defaults to 9)
        if (complevel != null) {
            args.append("--complevel ").append(complevel).append(" ");
        }

        // --notemp : The generated archive will not extract the files to a temporary directory, but in a new directory
        // created in the current directory. This is better to distribute software packages that may extract and compile
        // by themselves (i.e. launch the compilation through the embedded script).
        if (isTrue(notemp)) {
            args.append("--notemp ");
        }

        // --current : Files will be extracted to the current directory, instead of in a subdirectory. This option
        // implies --notemp above.
        if (isTrue(current)) {
            args.append("--current ");
        }

        // --follow : Follow the symbolic links inside of the archive directory, i.e. store the files that are being
        // pointed to instead of the links themselves.
        if (isTrue(follow)) {
            args.append("--follow ");
        }

        // --append (new in 2.1.x): Append data to an existing archive, instead of creating a new one. In this mode, the
        // settings from the original archive are reused (compression type, label, embedded script), and thus don't need
        // to be specified again on the command line.
        if (isTrue(append)) {
            args.append("--append ");
        }

        // --header : Makeself 2.0 uses a separate file to store the header stub, called makeself-header.sh. By default,
        // it is assumed that it is stored in the same location as makeself.sh. This option can be used to specify its
        // actual location if it is stored someplace else.
        if (headerFile != null) {
            args.append("--header ").append(headerFile).append(" ");
        }

        // --copy : Upon extraction, the archive will first extract itself to a temporary directory. The main
        // application of this is to allow self-contained installers stored in a Makeself archive on a CD, when the
        // installer program will later need to unmount the CD and allow a new one to be inserted. This prevents
        // "Filesystem busy" errors for installers that span multiple CDs.
        if (isTrue(copy)) {
            args.append("--copy ");
        }

        // --nox11 : Disable the automatic spawning of a new terminal in X11.
        if (isTrue(nox11)) {
            args.append("--nox11 ");
        }

        // --nowait : When executed from a new X11 terminal, disable the user prompt at the end of the script execution.
        if (isTrue(nowait)) {
            args.append("--nowait ");
        }

        // --nomd5 : Disable the creation of a MD5 checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (isTrue(nomd5)) {
            args.append("--nomd5 ");
        }

        // --nocrc : Disable the creation of a CRC checksum for the archive. This speeds up the extraction process if
        // integrity checking is not necessary.
        if (isTrue(nocrc)) {
            args.append("--nocrc ");
        }

        // --lsm file : Provide and LSM file to makeself, that will be embedded in the generated archive. LSM files are
        // describing a software package in a way that is easily parseable. The LSM entry can then be later retrieved
        // using the --lsm argument to the archive. An example of a LSM file is provided with Makeself.
        if (lsmFile != null) {
            args.append("--lsm ").append(lsmFile).append(" ");
        }

        // --gpg-extra opt : Append more options to the gpg command line.
        if (gpgExtraOpt != null) {
            args.append("--gpg-extra ").append(gpgExtraOpt).append(" ");
        }

        // --tar-extra opt : Append more options to the tar command line.
        //
        // For instance, in order to exclude the .git directory from the packaged archive directory using the GNU tar,
        // one can use makeself.sh --tar-extra "--exclude=.git" ...
        if (tarExtraOpt != null) {
            args.append("--tar-extra ").append(tarExtraOpt).append(" ");
        }

        // --untar-extra opt : Append more options to the during the extraction of the tar archive.
        if (untarExtraOpt != null) {
            args.append("--untar-extra ").append(untarExtraOpt).append(" ");
        }

        // --keep-umask : Keep the umask set to shell default, rather than overriding when executing self-extracting
        // archive.
        if (isTrue(keepUmask)) {
            args.append("--keep-umask ");
        }

        // --packaging-date date : Use provided string as the packaging date instead of the current date.
        if (packagingDate != null) {
            args.append("--packaging-date ").append(packagingDate).append(" ");
        }

        // --license : Append a license file.
        if (licenseFile != null) {
            args.append("--license ").append(licenseFile).append(" ");
        }

        // --nooverwrite : Do not extract the archive if the specified target directory already exists.
        if (isTrue(nooverwrite)) {
            args.append("--nooverwrite ");
        }

        // --help-header file : Add a header to the archive's --help output.
        if (helpHeaderFile != null) {
            args.append("--helpHeaderFile ").append(helpHeaderFile).append(" ");
        }

        return args.toString();
    }

    private boolean isTrue(Boolean value) {
        if (value != null) {
            return value.booleanValue();
        }
        return false;
    }

}
