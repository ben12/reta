// Package : com.ben12.reta.api
// File : Requirement.java
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
public interface Requirement
{
	/**
	 * @param summary
	 *            reference summary
	 * @param id
	 *            referred requirement ID
	 * @param version
	 *            referred requirement version
	 * @param attributes
	 *            reference additional attributes
	 * @return true if requirement reference has been added, false if requirement reference (id, version) already exists
	 */
	boolean addReference(String summary, String id, String version, Map<String, String> attributes);
}
