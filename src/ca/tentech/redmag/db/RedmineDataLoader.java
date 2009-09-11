/**
 * File name: RedmineDataLoader.java
 * Date: 2009-06-19
 * Time: 15:16:05
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
 *
 * Description:
 * Redmine database querying class
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
package ca.tentech.redmag.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * <P>This class executes different queries on the Redmine database
 * through the MySQL JDBC connector. It was developped using
 * Redmine 0.8.3 tables and does not rely on separate views.</P>
 * 
 * <P>Each function does an independant query through an independant connection.</p>
 * <P>SQL Tutorial consulted: http://www.aavso.org/aavso/meetings/spring09/sql_intro.pdf</P>
 * 
 * TODO: Adapt to specialized views and other connectors
 * TODO: User-based exceptions
 * TODO: Path-based exceptions- Path-based removal from main blanketing to add individually
 * TODO: Additionnal field for whether a user wants commit e-mails or not
 * TODO: Mail hook generator
 * TODO: User watch for last login (warnings)
 * TODO: User bulk add
 * TODO: Parent-child repos names
 * TODO: Post-commit hook extra script
 * TODO: Mass system message by project
 * @author veilleux
 * 
 */
public class RedmineDataLoader {
	/** URL of MySQL database */
	private String dbUrl;
	/** User name to access redmine DB */
	private String username;
	/** Password to access redmine DB */
	private String password;

	/**
	 * Initialize the query class with database information.
	 * 
	 * @param dbUrl - JDBC URL of database
	 * @param username - User name to use for connection
	 * @param password - Password to use for connection
	 * @throws SQLException on initialization error
	 */
	public RedmineDataLoader(String dbUrl, String username, String password) throws SQLException {
		this.dbUrl = dbUrl;
		this.username = username;
		this.password = password;

		// Register the JDBC driver for MySQL.
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			
			// Rethrow exception to ensure no calls are made on badly initialized JDBC driver
			throw new SQLException(e);
		}
	}

	/**
	 * Queries all existing roles, keyed by ID.
	 * 
	 * @return a map of human-readble role names, keyed by ID
	 * @throws SQLException on database access error 
	 */
	public HashMap<Integer, String> getRoleList() throws SQLException {
		HashMap<Integer, String> result = new HashMap<Integer, String>();
		
		try {
			ResultSet resultSet;

			// Get connection to database
			Connection c = DriverManager.getConnection(dbUrl, username,
					password);

			// Query DB for roles
			Statement statement = c.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			resultSet = statement.executeQuery("SELECT id, name FROM roles;");

			while (resultSet.next()) {
				result.put(resultSet.getInt("id"), resultSet.getString("name"));
			}

			c.close();
		} catch (Exception e1) {
			throw new SQLException(e1);
		}
		
		return result;
	}

	/**
	 * Queries the project list and returns project instances. The 
	 * RedmineProject class is a subset of the project table.
	 * 
	 * @return a list of RedmineProject instances
	 * @throws SQLException on database access error 
	 */
	public List<RedmineProject> getProjectList() throws SQLException {
		LinkedList<RedmineProject> result = new LinkedList<RedmineProject>();
		
		try {
			ResultSet resultSet;

			// Get connection to database
			Connection c = DriverManager.getConnection(dbUrl, username,
					password);

			// Query DB for projects
			Statement statement = c.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			resultSet = statement.executeQuery("SELECT identifier, name, description, parent_id, updated_on FROM projects;");

			// Add all project rows to the collection
			while (resultSet.next()) {
				String identifier = resultSet.getString("identifier");
				String name = resultSet.getString("name");
				String description = resultSet.getString("description");
				boolean isSubproject = resultSet.getInt("parent_id") > 0;
				Date lastUpdated = resultSet.getDate("updated_on");
				
				result.add(new RedmineProject(identifier, name, description, isSubproject, lastUpdated));
			}

			c.close();
		} catch (Exception e1) {
			throw new SQLException(e1);
		}
		
		return result;
	}

	/**
	 * Returns a subset of Redmine project members by joining members, users
	 * and projects. The <code>projectIdentifier</code> is the project for
	 * which to gather users. In the case of <code>projectIdentifier == ""</code>,
	 * all members of all projects are retrieved.
	 *  
	 * @param projectIdentifier - Project table "identifier" to select from. If
	 * empty, members of all projects are queried.
	 * 
	 * @return a list of RedmineProjectMember instances
	 * @throws SQLException on database access error
	 */
	public List<RedmineProjectMember> gatherUsersByProject(String projectIdentifier) throws SQLException {
		List<RedmineProjectMember> result = new LinkedList<RedmineProjectMember>();
		
		try {
			ResultSet resultSet;

			// Get connection to database
			Connection c = DriverManager.getConnection(dbUrl, username,
					password);

			// Query DB for members, joining with projects and users
			Statement statement = c.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);

			// Check to see if we are asking for a specific project
			if (!projectIdentifier.equals("")) {
				resultSet = statement.executeQuery("SELECT p.identifier, u.login," +
						" u.firstname, u.lastname, u.mail, u.admin, u.last_login_on,"+
						" m.role_id FROM members m, projects p, users u WHERE"+
						" m.project_id=p.id AND u.id=m.user_id AND p.identifier='"+
						projectIdentifier+"' ORDER BY identifier, role_id;");
			} else {
				resultSet = statement.executeQuery("SELECT p.identifier, u.login," +
						" u.firstname, u.lastname, u.mail, u.admin, u.last_login_on,"+
						" m.role_id FROM members m, projects p, users u WHERE"+
						" m.project_id=p.id AND u.id=m.user_id ORDER BY identifier, role_id;");
			}

			// Store result set to POJO collection
			while (resultSet.next()) {
				String login = resultSet.getString("login");
				String firstName = resultSet.getString("firstname");
				String lastName = resultSet.getString("lastname");
				String mailAddress = resultSet.getString("mail");
				String projectId = resultSet.getString("identifier");
				int roleId = resultSet.getInt("role_id");
				boolean isAdministrator = resultSet.getBoolean("admin");
				Date lastLoginDate = resultSet.getDate("last_login_on");
				
				RedmineProjectMember pm = new RedmineProjectMember(login, firstName, lastName, mailAddress, projectId, roleId, isAdministrator, lastLoginDate);
				result.add(pm);
			}

			c.close();
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new SQLException(e1);
		}
		
		return result;
	}
	
	/**
	 * Test function for ReadmineDataLoader class 
	 */
	public static void testLoader() {
		
		try {
			RedmineDataLoader loader = new RedmineDataLoader("jdbc:mysql://localhost:3306/junkdb","root","");
			
			System.out.println("---- Fetching all projects ----");
			for (RedmineProjectMember pm : loader.gatherUsersByProject("")) {
				System.out.println(pm.toString());
			}
			
			System.out.println("---- Fecthing individual projects ----");
			for (RedmineProject p : loader.getProjectList()) {
				System.out.printf("* Fetching project [%s]\n",p.getIdentifier());
				for (RedmineProjectMember pm : loader.gatherUsersByProject(p.getIdentifier())) {
					System.out.println(pm.toString());
				}
			}
			
			System.out.println("---- Fetching Roles ----");
			HashMap<Integer, String> roleList = loader.getRoleList();
			for (Integer id : roleList.keySet()) {
				System.out.printf("%d: %s\n", id.intValue(), roleList.get(id));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
