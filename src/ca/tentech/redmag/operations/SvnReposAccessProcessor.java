/**
 * File name: SvnReposAccessProcessor.java
 * Date: 2009-06-23
 * Time: 21:51:34
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
 *
 * History: 
 * - June 2009 (veilleux):  Original version
 * - September 13 2009 (veilleux): 
 *   - Fixed the missing display in verbose mode when no repositories were created
 * - September 14 2009 (veilleux):
 *   - Replaced all occurences of SVNKit usage by MicroSvnReposAdmin class
 *   
 * Description:
 * Processing class for generating SVN repository access. Handles
 * creating missing SVN repositories as well as generating SVN
 * AuthZ path-based restriction configurations.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package ca.tentech.redmag.operations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ca.tentech.redmag.RedmagMain;
import ca.tentech.redmag.db.RedmineDataLoader;
import ca.tentech.redmag.db.RedmineProject;
import ca.tentech.redmag.db.RedmineProjectMember;
import ca.tentech.redmag.svn.MicroSvnReposAdmin;

/**
 * Processing class for generating SVN repository access
 * @author veilleux
 */
public class SvnReposAccessProcessor {
	private String username = "";
	private String password = "";
	private String dbUrl = "";
	private List<Integer> readRoles = null;
	private List<Integer> readWriteRoles = null;
	private boolean verbose = false;
	private String outputFilename = "";
	private String svnRoot = "";
	
	// Sets of paths for the creation of repositories and permissions
	private Set<String> validSvnPath;
	private Set<String> existingSvnPath;
	private HashMap<String, String> pathToId;
	private List<RedmineProject> projects = null;
	private RedmineDataLoader loader = null;
	
	private static enum ReposRights {
		NONE,
		READ,
		READ_WRITE
	};
	
	/**
	 * Default constructor for internal state only. Setters MUST be called for all options !
	 */
	public SvnReposAccessProcessor() {
		super();
		
		validSvnPath = new HashSet<String>();
		existingSvnPath = new HashSet<String>();
		pathToId = new HashMap<String, String>();
	}
		
	/**
	 * Returns a permission determined by a project's role policy.
	 * This project group policy is independent of users and
	 * exceptions.
	 * 
	 * TODO: Fix what happens if a role is both READ and READ_WRITE
	 * 
	 * @param projectIdentifier - project identifier string (system-friendly)
	 * @param roleId - role ID, valid in "roles" table
	 * @return a ReposRights enum member specifying priviledge
	 */
	private ReposRights getRolePriviledge(String projectIdentifier, int roleId) {
		// Check read roles
		for (Integer role : readRoles) {
			if (role.intValue() == roleId) {
				return ReposRights.READ;
			}
		}
		
		// Check read/write roles
		for (Integer role : readWriteRoles) {
			if (role.intValue() == roleId) {
				return ReposRights.READ_WRITE;
			}
		}
		
		// Default: NO rights
		return ReposRights.NONE;
	}
	
	/**
	 * Equivalent of " ".join(list) in Python. Joins all string 
	 * <code>elements</code> with <code>separator</code> string,
	 * forming a full enumeration of elements.
	 * 
	 * @param elements - elements to join together
	 * @param separator - separator string to use between elements
	 * @return a complete joined collection
	 */
	private String stringJoin(List<String> elements, String separator) {
		StringBuilder result = new StringBuilder();
		int cnt = 0;
		
		for (String element : elements) {
			if (cnt != 0) result.append(separator);
			cnt++;
			
			result.append(element);
		}
		
		return result.toString(); 
	}
	
	/**
	 * Check list of projects for existence of repositories. Sets internal state for
	 * <code>generateMissingRepositories()</code> and <code>generateUserPermissions()</code> methods.
	 * 
	 * @throws SQLException on database error
	 * 
	 * ALWAYS call before generateMissingRepositories() OR generateUserPermissions()
	 * TODO: Fix this manually enforced calling order
	 */
	public void checkExistingRepositories() throws SQLException {
		loader = new RedmineDataLoader(dbUrl,username,password);
		
		// Query Redmine for projects
		projects = loader.getProjectList();
		
		// Step 1: Create missing repositories
		// -----------------------------------
		// Step 1a: Validate path (4 case: missing, exists as SVN 
		// directory, exists as file, exists as directory (not SVN)
		if (verbose) { System.out.println("*** Checking for missing repositories:"); }
		
		// Process all currently existing projects
		for (RedmineProject project : projects) {
			if (verbose) {System.out.printf("   Project \"%s\": ", project.getIdentifier());}
		
			// Extract project path information from each project identifiers
			String reposPath = svnRoot + "/" + project.getIdentifier();
			File reposPathFile = new File(reposPath);
			String path = reposPathFile.getAbsolutePath();
			pathToId.put(path, project.getIdentifier());
			
			// Manage different cases of file/directory existence
			if (reposPathFile.exists()) {
				if (reposPathFile.isDirectory()) {
					if (MicroSvnReposAdmin.isValidRepos(reposPathFile)) {
						// Case 1: exists as a valid SVN repository directory
						validSvnPath.add(path);
						existingSvnPath.add(path);
						if (verbose) {System.out.printf("EXISTS at : %s\n", path);}
					} else {
						// Case 2: exists as a non-SVN directory
						if (verbose) {
							System.out.printf("MISSING at : %s\n", path);
							System.out.printf("   --> ERROR: NON-SVN DIRECTORY WITH THAT NAME EXISTS !\n");
						}
					}
				} else {
					// Case 3: exists as a filename
					if (verbose) {
						System.out.printf("MISSING at : %s\n", reposPathFile.getAbsolutePath());
						System.out.printf("   --> ERROR: FILE WITH THAT NAME EXISTS !\n");
					}
				}
			} else { 
				// Case 4: does not exists (available)
				validSvnPath.add(path);
				if (verbose) { System.out.printf("MISSING at : %s\n", path); }
			}
		}
	}
	
	/**
	 * Creates the missing repositories based on the Redmine project database
	 * and given processor options.
	 */
	public void createMissingRepositories() {		
		if (verbose) { 
			System.out.println("\n*** Creating missing repositories"); 
		}

		int numberCreated = 0;			
		for (String path : validSvnPath) {	
			// Only create repositories that don't already exist
			if (!existingSvnPath.contains(path)) {
				numberCreated++;
				if (verbose) {
					System.out.printf("   Creating a repository at \"%s\" : ", path );
				}
				
				try {
					// Create a repos compatible with SVN 1.4+, generate a uuid, do not overwrite
					MicroSvnReposAdmin.createRepos(new File(path), "--pre-1.5-compatible");
					if (verbose) { System.out.printf("SUCCESS !\n"); }
					existingSvnPath.add(path);
				} catch (IOException e) {
					if (verbose) { System.out.printf("FAILURE !\n   -->%s\n", e.toString()); }
				}
			}
		}
		
		if (verbose && numberCreated == 0) {
			System.out.println("    SUCCESS: None to create !");
		}
	}
	
	/**
	 * Generate an AuthZ permission file at the location specified by the
	 * <code>outputFile</code> property.
	 */
	public void generateUserPermissions() throws SQLException { 
		StringBuilder groups = new StringBuilder("[groups]\n");
		StringBuilder sections = new StringBuilder();
		for (String path : existingSvnPath) {
			// Step 1: Query redmine databse for members of the project
			String identifier = pathToId.get(path);
			List<RedmineProjectMember> members = loader.gatherUsersByProject(identifier);
			List<String> readUsers = new LinkedList<String>();
			List<String> readWriteUsers = new LinkedList<String>();
			
			// Step 2: Iterate through project members, filling access lists
			// TODO: If user is in an exception or a subpath is an exception, do NOT give any blanket rights
			for (RedmineProjectMember member : members) {
				switch (getRolePriviledge(identifier, member.getRoleId())) {
					case NONE:
						// NO rights for none priviledge
						break;
					case READ:
						readUsers.add(member.getLogin());
						break;
					case READ_WRITE:
						readWriteUsers.add(member.getLogin());
						break;
				}
			}
			
			// Step 3: Generate groups and project sections from access lists
			sections.append("# Permissions for repos at " + path + "\n");
			sections.append(String.format("[%s:/]\n* = \n", identifier));
			
			if (!readUsers.isEmpty()) {
				groups.append(String.format("%s-r = %s\n",
					identifier, stringJoin(readUsers,", ")));
				sections.append(String.format("@%s-r = r\n", identifier));
			} else {
				sections.append(String.format("# No read-only users for project \"%s\"\n", identifier));
			}
			
			if (!readWriteUsers.isEmpty()) {
				groups.append(String.format("%s-rw = %s\n",
					identifier, stringJoin(readWriteUsers,", ")));
				sections.append(String.format("@%s-rw = rw\n", identifier));
			} else {
				sections.append(String.format("# No read-write users for project \"%s\"\n", identifier));
			}
			
			groups.append("\n");
			sections.append("\n");
		}
		
		// Step 4: Save AuthZ file
		try {
			FileWriter outputFileWriter = new FileWriter(outputFilename, false);
			outputFileWriter.append("#\n# AUTOMATICALLY GENERATED AUTHZ FILE\n" + "# By RedSvnTool " + RedmagMain.VERSION + "\n# *** DO NOT MODIFY BY HAND ***\n# Contact system administrator !\n");
			outputFileWriter.append("# File generated on: " + (new Date()).toString() + "\n\n");
			outputFileWriter.append(groups.toString());
			
			outputFileWriter.append("# Default policy is no access\n[/]\n* = \n\n");
			outputFileWriter.append(sections.toString());
			if (verbose) {
				System.out.printf("\n*** SAVED Authorization file: %s\n",outputFilename);
			}
			outputFileWriter.close();
		} catch (IOException e1) {
			if (verbose) {
				System.out.printf("\n*** ERROR SAVING AUTHZ FILE \"%s\": %s\n",outputFilename, e1.toString());
			}
		}
	}

	/**
	 * Draw a separator line for a table with the specified column <code>lenghts</code>,
	 * using the <code>style</code> line-drawing character.
	 * <p>Example:</p>
	 * <pre>
	 * s = drawSeparatorLine({3,5}, '-');
	 * 
	 * s: "+---+-----+\n"
	 * </pre>
	 * 
	 * @param lengths - array of the lengths of each column
	 * @param style - character to use to draw line
	 * @return the string containing the separator line
	 */
	 private String drawSeparatorLine(int [] lengths, char style) {
		StringBuilder result = new StringBuilder();
		
		result.append('+');
		for (int length : lengths) {
			for (int i = 0; i < length; i++) {
				result.append(style);
			}
			result.append('+');
		}
		
		result.append("\n");
		return result.toString();
	}
	
	/**
	 * Draw a table row with the specified column <code>lenghts</code>,
	 * using the <code>labels</code> contents for each column of the row.
	 * <p>Example:</p>
	 * <pre>
	 * s = drawTableRow({5,9}, {"id", "role"});
	 * 
	 * s: "|  id |    role |\n"
	 * </pre>
	 * 
	 * @param lengths - array of the lengths of each column
	 * @param labels - string contents for each column
	 * @return the string containing the drawn table row
	 */
	private String drawTableRow(int [] lengths, String [] labels) {
		StringBuilder result = new StringBuilder();
		
		result.append('|');
		for (int i = 0; i < labels.length; i++) {
			String label = String.format(String.format("%%%ds", lengths[i]-1), labels[i]);
			result.append(label);
			result.append(" |");
		}
		
		result.append("\n");
		return result.toString();
	}
	
	/**
	 * Gets a full list of user roles in a pretty-printed table for display
	 * 
	 * @return a string containing a description of user roles
	 * @throws SQLException on database access error
	 */
	public String getRoleList() throws SQLException { 
		StringBuilder rolesTable = new StringBuilder();
		
		loader = new RedmineDataLoader(dbUrl,username,password);
		HashMap<Integer, String> roles = loader.getRoleList();
		
		// Obtain the longest role name length
		int maxLength = 0;
		for (String role : roles.values()) {
			if (role.length() > maxLength)
				maxLength = role.length();
		}

		int lengths[] = { 6, maxLength + 2 };
		
		// Draw table headers
		rolesTable.append(drawSeparatorLine(lengths, '-'));
		rolesTable.append(drawTableRow(lengths, new String [] {"id", "role"}));
		rolesTable.append(drawSeparatorLine(lengths, '='));
		
		// Sort role Id keys
		Integer [] keys = roles.keySet().toArray(new Integer [] {});
		Arrays.sort(keys);
		
		// Draw all table rows
		String labels [] = new String[2];
		for (Integer key: keys) {
			labels[0] = String.format("%d", key.intValue());
			labels[1] = roles.get(key);
			rolesTable.append(drawTableRow(lengths, labels));
		}
		
		rolesTable.append(drawSeparatorLine(lengths, '-'));
		
		return rolesTable.toString();
	}
	
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @param dbUrl the dbUrl to set
	 */
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
	/**
	 * @param readRoles the readRoles to set
	 */
	public void setReadRoles(List<Integer> readRoles) {
		this.readRoles = readRoles;
	}
	/**
	 * @param readWriteRoles the readWriteRoles to set
	 */
	public void setReadWriteRoles(List<Integer> readWriteRoles) {
		this.readWriteRoles = readWriteRoles;
	}
	/**
	 * @param verbose the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	/**
	 * @param outputFilename the outputFilename to set
	 */
	public void setOutputFilename(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	/**
	 * @param svnRoot the svnRoot to set
	 */
	public void setSvnRoot(String svnRoot) {
		this.svnRoot = svnRoot;
	}
}
