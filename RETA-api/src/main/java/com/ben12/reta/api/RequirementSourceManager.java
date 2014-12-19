// Package : com.ben12.reta.api
// File : RequirementSourceManager.java
// 
// Copyright (C) 2014 Benoît Moreau (ben.12)
//
// This file is part of RETA (Requirement Engineering Traceability Analysis).
//
// RETA is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// RETA is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with RETA.  If not, see <http://www.gnu.org/licenses/>.
package com.ben12.reta.api;

import java.util.Map;

/**
 * @author Benoît Moreau (ben.12)
 */
public interface RequirementSourceManager
{
	/**
	 * @return requirement source name
	 */
	String getName();

	/**
	 * @param summary
	 *            requirement summary
	 * @param id
	 *            requirement ID
	 * @param version
	 *            requirement version
	 * @param content
	 *            requirement content description
	 * @param attributes
	 *            requirement additional attributes
	 * @return requirement added, null if requirement (id, version) already exists
	 */
	Requirement addRequirement(String summary, String id, String version, String content, Map<String, String> attributes);
}
