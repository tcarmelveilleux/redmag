/**
 * File name: RedmagMain.java
 * Date: 2009-06-23
 * Time: 22:17:49
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import ca.tentech.redmag.operations.SvnReposAccessProcessor;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main class for command-line version of Redmag tool. Parses the command line
 * and runs the required tasks against the Redmine database
 * 
 * @author veilleux
 *
 */
public class RedmagMain {
	public static final String VERSION = "1.1";
	public static final String VERSION_STRING = "Redmag v"+VERSION+
	"\nThe automatic SVN repository management tool for Redmine integration\n"+
	"By Tennessee Carmel-Veilleux (veilleux@tentech.ca)\n";

	private static final int OK_EXITCODE = 0;
	private static final int BAD_ARGUMENTS_EXITCODE = 1;
	private static final int DB_ERROR_EXITCODE = 3;
	
	private static final int DEFAULT_DATABASE_PORT = 3306;
	
	/**
	 * Main entrypoint for Redmag command-line operation
	 * 
	 * @param args - N/A
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// Set default values for parameters
		String username = "";
		String password = "";
		String svnRoot = "/svn";
		String dbUrl = "";
		List<Integer> readRoles = new LinkedList<Integer>();
		List<Integer> readWriteRoles = new LinkedList<Integer>();
		boolean createMissingRepos = false;
		boolean verbose = false;
		String outputFilename = "/svn/access.authZ";
		
        OptionParser parser = new OptionParser() {
            {
            	acceptsAll( asList("u", "user") ).withRequiredArg().ofType( String.class )
                    .describedAs( "Redmine database username" );
            
            	acceptsAll( asList("p", "password") ).withRequiredArg().ofType( String.class )
                    .describedAs( "Redmine database password" );
            	
            	acceptsAll( asList("s", "svn-root") ).withRequiredArg().ofType( String.class )
                .describedAs( "SVN repositories root (default: /svn)" );
                
            	acceptsAll( asList("d", "dbname") ).withRequiredArg().ofType( String.class )
                .describedAs( "Redmine database name" );
            	
            	accepts( "port" ).withRequiredArg().ofType( Integer.class )
                	.describedAs( "Redmine database port" );
            
                acceptsAll( asList( "v", "verbose" ), "be verbose" );
                acceptsAll( asList( "l", "list-roles" ), "list available user roles" );
                
                accepts( "output-file" ).withOptionalArg().ofType( File.class )
                     .describedAs( "filename (default:/svn/access.authZ)" );
                acceptsAll( asList( "h", "?" ), "show help" );
                
                accepts("read-roles", "Provide list of roleIds that can read SVN" ).withRequiredArg()
                    .describedAs( "roleId1,roleId2,..." )
                    .ofType( Integer.class )
                    .withValuesSeparatedBy( ',' );
                
                accepts("readwrite-roles", "Provide list of roleIds that can read and write SVN" ).withRequiredArg()
                .describedAs( "roleId1,roleId2,..." )
                .ofType( Integer.class )
                .withValuesSeparatedBy( ',' );
                
                acceptsAll( asList("c", "create-missing-repos"), "Create missing project repositories");
            }
        };

        boolean needHelp = false;
        OptionSet options = null;
        
        // Try to parse options
        try {
	        options = parser.parse( args );
	        if ( options.has( "?" ) ) {
	            needHelp = true;
	        }
        } catch (OptionException e1) {
        	System.out.println("ERROR: " + e1.getMessage() + "\n");
        	
        	System.out.println(VERSION_STRING);
    		try {
    			parser.printHelpOn( System.out );
    		} catch (Exception e) {
    			// If this ever happens we're in trouble :) 
    			e.printStackTrace();
    		}
    		
       		System.exit(BAD_ARGUMENTS_EXITCODE);
        }
	        
	    // Validate options
        if (!options.hasArgument("user") && !needHelp) {
        	System.out.println("ERROR: Redmine database user name required !\n");
        	needHelp = true;
        } else {
        	username = (String)options.valueOf("user");
        }

        String dbName = "";
        if (!options.hasArgument("dbname") && !needHelp) {
        	System.out.println("ERROR: Redmine database name !\n");
        	needHelp = true;
        } else {
        	dbName = (String)options.valueOf("dbname");
        }
        
        if (options.hasArgument("password")) {
        	password = (String)options.valueOf("password");
        }
        
        int dbport = DEFAULT_DATABASE_PORT;
        if (options.hasArgument("port")) {
        	dbport = ((Integer)(options.valueOf("port"))).intValue();
        }
        
        // Build database URL from gathered data
        dbUrl = String.format("jdbc:mysql://localhost:%d/%s", dbport, dbName);
        
        // Initialize processor (Part 1: Database access)
        SvnReposAccessProcessor processor = new SvnReposAccessProcessor();
        processor.setUsername(username);
        processor.setPassword(password);
        processor.setDbUrl(dbUrl);
        
        // Handle listing roles through the command line
        if (options.has("list-roles") && !needHelp) {
        	String roleList = "";
        	
        	try {
        		roleList = processor.getRoleList();	
			}  catch (SQLException e1) {
	        	System.err.println("Database Access Error: " + e1.toString());
	        	System.exit(DB_ERROR_EXITCODE);
	        }
			
			System.out.println("Available roles list:");
			System.out.println(roleList);
        	System.exit(OK_EXITCODE);
        }
        
        // Continue with other arguments if roles did not need to be listed
        if (options.hasArgument("svn-root")) {
        	svnRoot = (String)options.valueOf("svn-root");
        }
        
        if (!options.hasArgument("read-roles") && !needHelp) {
        	System.out.println("ERROR: Read roles list is mandatory !\n");
        	needHelp = true;
        } else {
        	readRoles = (List<Integer>)options.valuesOf("read-roles");
        }
        
        if (!options.hasArgument("readwrite-roles") && !needHelp) {
        	System.out.println("ERROR: Read/write roles list is mandatory !\n");
        	needHelp = true;
        } else {
        	readWriteRoles = (List<Integer>)options.valuesOf("readwrite-roles");
        }
        
        if (options.has("create-missing-repos")) {
        	createMissingRepos = true;
        }
        
        if (options.has("verbose")) {
        	verbose = true;
        }
        
        if (options.hasArgument("output-file")) {
        	outputFilename = (String)options.valueOf("output-file");
        }
        
        // Check for help
        try {
        	if (needHelp) {
        		System.out.println(VERSION_STRING);
        		parser.printHelpOn( System.out );
        		
        		if (!options.has("?")) {
            		System.exit(BAD_ARGUMENTS_EXITCODE);
            	}
        	}
        } catch (IOException e1) {
        	// IGNORE: CANNOT DO ANYTHING, EXIT !
        	// We are in deep trouble if we can't write to SystemOut !
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
        	System.out.println("Database Access Error: " + e1.toString());
        	System.exit(DB_ERROR_EXITCODE);
        }
   	}
}
