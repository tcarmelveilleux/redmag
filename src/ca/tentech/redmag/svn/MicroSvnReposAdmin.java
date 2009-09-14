/**
 * File name: MicroSvnReposAdmin.java
 * Date: 2009-09-14
 * Time: 08:13:22
 *
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
 *
 * History: 
 * - September 14 2009 (veilleux): 
 *   - Created the class
 *   
 * Description:
 * Utility class to replace the SVNKit with minimal SVN validation
 * and creation methods based on calling the SVN client directly
 * 
 * ------
 * This file is part of the Redmag program (http://code.google.com/p/redmag)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 */
package ca.tentech.redmag.svn;

import java.io.File;
import java.io.IOException;

import com.Ostermiller.util.ExecHelper;

/**
 * Utility class to replace the SVNKit with minimal SVN validation
 * and creation methods based on calling the SVN client directly
 * 
 * @author veilleux
 */
public final class MicroSvnReposAdmin {
	/**
	 * Validates whether a path contains a valid SVN repository. Uses the local command-line <code>svnadmin</code>
	 * tool. If the repository is incompatible with the current version, false is returned.
	 * 
	 * @param svnPath - path of directory to validate
	 * @return true if the path is a valid SVN repository, false otherwise
	 */
	public static boolean isValidRepos(File svnPath) {
		try {
			// Quietly executes svnadmin
			if (ExecHelper.execUsingShell("svnadmin verify -q " + svnPath.getCanonicalPath()).getStatus() == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e1) {
			// This should never happen, so we print-out the message to standard error...
			System.err.println("ERROR: SVN execution error: " + e1.getMessage());
			return false;
		}
	}
	
	/**
	 * Helper method to create an SVN repository by calling the <code>svnadmin</code> command-line tool.
	 * 
	 * @param svnPath - Directory path to use as root for new repository
	 * @param extraFlags - Extra flags to add to the command (ex: "--pre-1.5-compatible")  
	 * @throws IOException if the svnadmin returns a non-zero exit code. Exception message contains stdout and stderr log
	 */
	public static void createRepos(File svnPath, String extraFlags) throws IOException {
		String command = "svnadmin create " + extraFlags + " " + svnPath.getCanonicalPath();;

		// Execute svnadmin command to create repos
		ExecHelper svnResult = ExecHelper.execUsingShell(command);
		
		if (svnResult.getStatus() != 0) {
			// Failure in creation, never return and send stderr + stdout result as exception message
			throw new IOException(svnResult.getError() + " " + svnResult.getOutput());
		}
	}
}
