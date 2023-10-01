1.6.0
-----
- Require maven 3.5.0
- Use git 2.42.0
- makeself 2.5.0.snapshot.2023-08-29

- Deprecated 'sign' in favor of 'signPassphrase' for better clarity
- Add 'signPassphrase'
- Add 'bzip3' support
- Add 'extractTargetDir' support
- Add 'tarQuietly' support
- Add 'quiet' support
- Add 'version' support with direct exit (note: we internally have always ran 'version' but this allows user to quickly call version as a parameter and exit
- Fix 'sslPasswd' and 'sslPasswdScr' support
- Adjust 'skip' to 'makeself.skip' so its possible to pass that on command line
- Fix potential path traversal issue but code is internal so its very unlikely to occur unless using jar not provided by us.
- Rework help logic so its quicker to run and get out

- Add automatic module name as 'com.github.hazendaz.maven.makeself'
- Builds now reproducible
- Set plexus utils to provided as no direct usage and internally use version 4 with xml 3
- Internally renamed 'targetDirectory' used for makeself temp space to 'makeselfTempDirectory'

1.5.1
-----
- Add 'inlineScript' to skip 1.5.0 verifications for startupScript and instead check scriptArgs exist for case where startupScript is not a file as expected but rather inline script followed by arguments.

1.5.0
-----
- Add 'extension' exposure to caller to set the type of extension used.  This was previously set to 'sh' and will remain its default.  When using 'run' type as defined by makeself standards, set this to 'run'.  This is used for attachments to maven for distribution to sonatype/artifactory/etc.
- Add 'classifier' exposure to caller to set the classifier of the fileName used when using multiple executions.  This previously did not use classifier and will remain not using classifier which takes on form of the project.  If not set when more than one execution used, the attachment will only pick up last defined.
- Added verifications on input data for 4 required parameters

1.3.2
-----
- Updated build libraries
- Use git 2.33.1
- makeself 2.4.5.snapshop.2021-09-24

1.3.1
-----
- Added changelog for direct tracking of changes.
- Changed how PortableGit is downloaded to allow more than one per machine.  Now entirely under 'repository/PortableGit' and then versioned.
