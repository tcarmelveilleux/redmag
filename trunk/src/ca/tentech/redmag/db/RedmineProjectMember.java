/**
 * File name: RedmineProjectMember.java
 * Date: 2009-06-21
 * Time: 14:40:45
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
 *
 * Description:
 * Model definition for a Redmine project member. Based on 
 * Redmine 0.8.3 tables.
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

import java.util.Date;

/**
 * Model definition for a Redmine project member with enough information
 * to generate SVN access policies. This class is a subset of the 
 * Redmine 0.8.3 users table.
 * 
 *  @author veilleux
 */
public class RedmineProjectMember {
	/** User login name */
	private String login;
	/** User first name */
	private String firstName;
	/** User last name */
	private String lastName;
	/** User e-mail address */
	private String mailAddress;
	/** Project identifier */
	private String projectId;
	/** User Role ID in project (ordinal, based on Roles table IDs) */
	private int roleId;
	/** Whether Redmine user is an administrator for the system*/
	private boolean isAdministrator;
	/** Date of last login in Redmine*/
	private Date lastLogin;
	
	/**
	 * @param login - User login name
	 * @param firstName - User first name
	 * @param lastName - User last name
	 * @param mailAddress - User e-mail address
	 * @param projectId - Project identifier
	 * @param roleId - User Role ID in project (ordinal, based on Roles table IDs)
	 * @param isAdministrator - Whether Redmine user is an administrator for the system
	 * @param lastLogin - Date of last login in Redmine
	 */
	public RedmineProjectMember(String login, String firstName,
			String lastName, String mailAddress, String projectId,
			int roleId, boolean isAdministrator, Date lastLogin) {
		super();
		this.login = login;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mailAddress = mailAddress;
		this.projectId = projectId;
		this.roleId = roleId;
		this.isAdministrator = isAdministrator;
		this.lastLogin = lastLogin;
	}
	
	/**
	 * @return the login
	 */
	public String getLogin() {
		return login;
	}
	/**
	 * @param login the login to set
	 */
	public void setLogin(String login) {
		this.login = login;
	}
	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the mailAddress
	 */
	public String getMailAddress() {
		return mailAddress;
	}
	/**
	 * @param mailAddress the mailAddress to set
	 */
	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}
	/**
	 * @return the projectId
	 */
	public String getProjectId() {
		return projectId;
	}
	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	/**
	 * @return the roleId
	 */
	public int getRoleId() {
		return roleId;
	}
	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	/**
	 * @return the isAdministrator
	 */
	public boolean isAdministrator() {
		return isAdministrator;
	}

	/**
	 * @param isAdministrator the isAdministrator to set
	 */
	public void setAdministrator(boolean isAdministrator) {
		this.isAdministrator = isAdministrator;
	}

	/**
	 * @return the lastLogin
	 */
	public Date getLastLogin() {
		return lastLogin;
	}

	/**
	 * @param lastLogin the lastLogin to set
	 */
	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String lastLoginStr;
		
		// Prevent NULL on last login
		if (lastLogin == null) {
			lastLoginStr = "Never";
		} else {
			lastLoginStr = lastLogin.toString();
		}
		
		return String.format("ProjectID: %s, Login: %s, First: %s, Last: %s, Admin= %s, E-mail= %s, Last-Login= %s, Role= %d",
				projectId, login, firstName, lastName, Boolean.toString(isAdministrator), mailAddress, lastLoginStr, roleId);
	}	
}
