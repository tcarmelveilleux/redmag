/**
 * File name: RedmineProject.java
 * Date: 2009-06-21
 * Time: 18:54:35
 * $Id$
 *
 * By Tennessee Carmel-Veillleux (veilleux (at) tentech (dot) ca) 
 *
 * Description:
 * Model definition for a Redmine project. Based on Redmine 0.8.3 tables.
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
 * Model definition for a Redmine project. This class is a subset of 
 * the Redmine 0.8.3 projects table.
 * 
 *  @author veilleux
 */
public class RedmineProject {
	/** System-friendly identifier (no spaces) */
	private String identifier;
	/** Project full name */
	private String name;
	/** Project text description */
	private String description;
	/** Whether project is a subproject */
	private boolean isSubproject;
	/** Date of last update */
	private Date lastUpdated;
	
	/**
	 * @param identifier - System-friendly identifier (no spaces)
	 * @param name - Project full name
	 * @param description - Project text description
	 * @param isSubproject - Whether project is a subproject
	 * @param lastUpdated - Date of last update
	 */
	public RedmineProject(String identifier, String name, String description,
			boolean isSubproject, Date lastUpdated) {
		super();
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.isSubproject = isSubproject;
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the isSubproject
	 */
	public boolean isSubproject() {
		return isSubproject;
	}

	/**
	 * @param isSubproject the isSubproject to set
	 */
	public void setSubproject(boolean isSubproject) {
		this.isSubproject = isSubproject;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String lastUpdatedStr;
		
		// Prevent NULL on last login
		if (lastUpdated == null) {
			lastUpdatedStr = "Never";
		} else {
			lastUpdatedStr = lastUpdated.toString();
		}
		
		return String.format("Identifier: %s, name: %s, description: %s isSubProject= %s, Last update= %s",
				identifier, name, description, Boolean.toString(isSubproject), lastUpdatedStr);
	}
}

