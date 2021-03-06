// Package : com.ben12.reta.api
// File : RETAParser.java
// 
// Copyright (C) 2015 Beno�t Moreau (ben.12)
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

/**
 * @author Beno�t Moreau (ben.12)
 */
public interface RETAParser
{
	/**
	 * Parse the source and use {@code manager} for add requirements and references found.
	 * 
	 * @param manager
	 *            requirement source manager
	 * @throws RETAParseException
	 *             Parsing exception
	 */
	void parseSource(RequirementSourceManager manager) throws RETAParseException;

	/**
	 * Parse the source and use {@code manager} for add requirements and references found.<br>
	 * Print the requirement source parsed in {@code output} and stop parsing when limit is reached.
	 * 
	 * @param manager
	 *            requirement source manager
	 * @param output
	 *            source content parsed
	 * @param limit
	 *            source limit size to parse
	 * @throws RETAParseException
	 *             Parsing exception
	 */
	void parseSourcePreview(RequirementSourceManager manager, StringBuilder output, int limit)
			throws RETAParseException;
}
