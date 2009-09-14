/**
 * File name: RedmagMain.java
 * Date: 2009-06-23
 * Time: 22:17:49
 * $Id$
 * 
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca)
 * 
 * History: 
 * - June 2009 (veilleux):  Original version with JOpt-Simple
 * - September 13 2009 (veilleux): 
 *   - Switched to com.Ostermiller.util.CmdLn for option parsing (GPL)
 *   - Tested all argument parsing error handlers
 *   - Simplified validation logic
 *   - Simplified error message display
 *   - Added an argument to specify database host
 *   - Bumped minor version to 1.2
 * - September 14 2009 (veilleux):
 *   - Replaced all occurences of SVNKit calls to the self-written MicroSvnReposAdmin,
 *     thus eliminating the SVNKit dependency.
 *   - Bumped minor version to 1.3
 *   
 * Description:
 * Main class for command-line version of Redmag tool
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
package ca.tentech.redmag;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import ca.tentech.redmag.operations.SvnReposAccessProcessor;

import com.Ostermiller.util.CmdLn;
import com.Ostermiller.util.CmdLnException;
import com.Ostermiller.util.CmdLnOption;

/**
 * Main class for command-line version of Redmag tool. Parses the command line
 * and runs the required tasks against the Redmine database
 * 
 * @author veilleux
 *
 */
public class RedmagMain {
	public static final String VERSION = "1.3";
	public static final String VERSION_STRING = "Redmag v"+VERSION+
	"\nThe automatic SVN repository management tool for Redmine integration\n"+
	"By Tennessee Carmel-Veilleux (veilleux@tentech.ca)\n";

	private static final int OK_EXITCODE = 0;
	private static final int BAD_ARGUMENTS_EXITCODE = 1;
	private static final int DB_ERROR_EXITCODE = 3;
	
	private static final int DEFAULT_DATABASE_PORT = 3306;
	private static final String DEFAULT_DATABASE_HOST = "localhost";
	
	/**
	 * Main entrypoint for Redmag command-line operation
	 * 
	 * @param args - command-line arguments
	 */
	public static void main(String[] args) {
		// Set default values for parameters
		List<Integer> readRoles = new LinkedList<Integer>();
		List<Integer> readWriteRoles = new LinkedList<Integer>();
		boolean createMissingRepos = false;
		boolean verbose = false;
		String outputFilename = "/svn/access.authZ";
		String svnRoot = "/svn";
		SvnReposAccessProcessor processor = null;
		
		// Command line parser instantiation and configuration
		CmdLn parser = new CmdLn(args).setDescription(VERSION_STRING);
        parser.addOptions(new CmdLnOption[] {
        	new CmdLnOption(new String [] {"help"}, new char [] {'?','h'}).setDescription("show help"),
        	new CmdLnOption("user",'u').setRequiredArgument().setDescription("Redmine database username"),
        	new CmdLnOption("password",'p').setRequiredArgument().setDescription("Redmine database password (default: \"\")"),
        	new CmdLnOption("svn-root",'s').setRequiredArgument().setDescription("SVN repositories root (default: /svn)"),
        	new CmdLnOption("dbname",'d').setRequiredArgument().setDescription("Redmine database name"),
        	new CmdLnOption("dbhost",'i').setRequiredArgument().setDescription("Redmine database host (default: localhost)"),
        	new CmdLnOption("port").setRequiredArgument().setDescription("Redmine database port"),
        	new CmdLnOption("verbose",'v').setDescription("be verbose"),
        	new CmdLnOption("list-roles",'l').setDescription("list available user roles"),
        	new CmdLnOption("output-file").setRequiredArgument().setDescription("filename (default:/svn/access.authZ)"),
        	new CmdLnOption("read-roles").setRequiredArgument().setDescription("Provide list of roleIds that can read SVN: roleId1,roleId2,.."),
        	new CmdLnOption("rw-roles").setRequiredArgument().setDescription("Provide list of roleIds that can read and write SVN: roleId1,roleId2,.."),
            new CmdLnOption("create-missing-repos",'c').setDescription("Create missing project repositories")
        });
        
        // Try to parse options
        try {
	        parser.parse();
	        
	        if ( parser.present('h') ) {
	        	parser.printHelp();
	 
	       		System.exit(OK_EXITCODE);
	        }
        } catch (CmdLnException e1) {
        	System.out.println("ERROR: " + e1.getMessage() + "\n");        	
        	System.out.println("Use the -h option to get help !");
       		System.exit(BAD_ARGUMENTS_EXITCODE);
        }
        
		try {		
			// Validate options
			if (parser.present("verbose")) {
				verbose = true;
			}
			
			String username = "";
			if (!parser.present("user")) {
				System.out.println("ERROR: Redmine database user name required !\n");
				throw new IllegalArgumentException();
			} else {
				username = parser.getResult("user").getArgument();
			}

			String dbName = "";
			if (!parser.present("dbname")) {
				System.out.println("ERROR: Redmine database name required !\n");
				throw new IllegalArgumentException();
			} else {
				dbName = parser.getResult("dbname").getArgument();
			}
			
			String dbHost = DEFAULT_DATABASE_HOST;
			if (parser.present("dbhost")) {
				dbHost = parser.getResult("dbhost").getArgument();
			}
			
			String password = "";
			if (parser.present("password")) {
				password = parser.getResult("password").getArgument();
			}
			
			int dbPort = DEFAULT_DATABASE_PORT;
			if (parser.present("port")) {
				try {
					dbPort = Integer.parseInt(parser.getResult("port").getArgument());
				} catch (NumberFormatException e) {
					System.out.println("ERROR: Bad port number format: \"" + parser.getResult("port").getArgument() + "\"");
					throw new IllegalArgumentException();
				}
			}
			
			// Build database URL from gathered data
			String dbUrl = "";
			dbUrl = String.format("jdbc:mysql://%s:%d/%s", dbHost, dbPort, dbName);
			if (verbose) {
				System.out.println("*** Using database URL: " + dbUrl);
			}
			
			// Initialize processor (Part 1: Database access) 
			processor = new SvnReposAccessProcessor();
			processor.setUsername(username);
			processor.setPassword(password);
			processor.setDbUrl(dbUrl);
			
			// Handle listing roles through the command line
			if (parser.present("list-roles")) {
				String roleList = "";
				
				try {
					roleList = processor.getRoleList();	
				}  catch (SQLException e1) {
			    	System.err.println("ERROR: Database Access Error: " + e1.toString());
			    	System.exit(DB_ERROR_EXITCODE);
			    }
				
				System.out.println("Available roles list:");
				System.out.println(roleList);
				System.exit(OK_EXITCODE);
			}
			
			if (parser.present("svn-root")) {
				svnRoot = parser.getResult("svn-root").getArgument();
			}
			
			// Parse read-only roles
			if (!parser.present("read-roles")) {
				System.out.println("ERROR: Read roles list is mandatory !\n");
				throw new IllegalArgumentException();
			} else {
				String [] readRolesOpt = parser.getResult("read-roles").getArgument().split(",");
				readRoles = new LinkedList<Integer>();
				for (String id : readRolesOpt) {
					try {
						readRoles.add(Integer.parseInt(id));
					} catch (NumberFormatException e) {
						System.out.println("ERROR: Bad read-only role id format: \"" + id + "\"");
						throw new IllegalArgumentException();
					}
				}
				
				if (verbose) { 
					System.out.println("*** Read-only roles: " + readRoles.toString());
				}
			}
			
			// Parse read/write roles
			if (!parser.present("rw-roles")) {
				System.out.println("ERROR: Read/writes roles list is mandatory !\n");
				throw new IllegalArgumentException();
			} else {
				String [] readWriteRolesOpt = parser.getResult("rw-roles").getArgument().split(",");
				readWriteRoles = new LinkedList<Integer>();
				
				for (String id : readWriteRolesOpt) {
					try {
						readWriteRoles.add(Integer.parseInt(id));
					} catch (NumberFormatException e) {
						System.out.println("ERROR: Bad read/writerole id format: \"" + id + "\"");
						throw new IllegalArgumentException();
					}
				}
				
				if (verbose) { 
					System.out.println("*** Read/write roles: " + readWriteRoles.toString());
				}
			}
			        
			if (parser.present("create-missing-repos")) {
				createMissingRepos = true;
			}
			
			if (parser.present("output-file")) {
				outputFilename = parser.getResult("output-file").getArgument();
			}
		} catch (IllegalArgumentException e1) {
			System.out.println("Use the -h option to get help !");
    		System.exit(BAD_ARGUMENTS_EXITCODE);
		}
        // No help required: everything must be valid. Let's run our task
        
        // Initialize processor (Part 2: Other options)
        processor.setVerbose(verbose);
        processor.setOutputFilename(outputFilename);
        processor.setReadRoles(readRoles);
        processor.setReadWriteRoles(readWriteRoles);
        processor.setSvnRoot(svnRoot);
        
        // Run necessary operations
        try {
	        processor.checkExistingRepositories();
	        if (createMissingRepos) {
	        	processor.createMissingRepositories();
	        }
	        processor.generateUserPermissions();
	        System.exit(OK_EXITCODE);
        } catch (SQLException e1) {
        	System.out.println("ERROR: Database Access Error: " + e1.toString());
        	System.exit(DB_ERROR_EXITCODE);
        }	
	}
}
