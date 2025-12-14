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
println '***********************************'
println 'Checking Existing Git Shell Created'
println '***********************************'

File makeself = new File( localRepositoryPath, "com/github/hazendaz/makeself-existing-git/testing/makeself-existing-git-testing.sh" );
assert makeself.exists()

println '***************************************'
println 'Checking Existing Git Build Log Created'
println '***************************************'

File buildlog = new File(basedir, 'build.log')
assert buildlog.exists()

println '*********************************'
println 'Checking Build Log was successful'
println '*********************************'

assert buildlog.text.contains( "[INFO] hello" )
