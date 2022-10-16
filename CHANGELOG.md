1.5.0
-----
- Add 'extension' exposure to caller to set the type of extension used.  This was previously set to 'sh' and will remain its default.  When using 'run' type as defined by makeself standards, set this to 'run'.  This is used for attachments to maven for distribution to sonatype/artifactory/etc.
- Add 'classifier' exposure to caller to set the classifier of the fileName used when using multiple executions.  This previously did not use classifier and will remain not using classifier which takes on form of the project.  If not set when more than one execution used, the attachment will only pick up last defined.

1.3.2
-----
- Updated build libraries
- Use git 2.33.1
- makeself 2.4.5.snapstop.2021-09-24

1.3.1
-----
- Added changelog for direct tracking of changes.
- Changed how PortableGit is downloaded to allow more than one per machine.  Now entirely under 'repository/PortableGit' and then versioned.
