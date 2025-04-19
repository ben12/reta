// Package : com.ben12.reta.model
// File : GraphData.java
// 
// Copyright (C) 2025 benmo
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
package com.ben12.reta.model;

import java.util.Map;

/**
 * @author benmo
 */
public class GraphData
{
	private final String								html;

	private final Map<String, InputRequirementSource>	sourceEntities;

	private final Map<String, Link>						links;

	/**
	 * @param pSvg
	 *            SVG source
	 * @param pSourceEntities
	 *            requirement source alias
	 * @param pLinks
	 *            links identifiers
	 */
	public GraphData(final String pHtml, final Map<String, InputRequirementSource> pSourceEntities,
			final Map<String, Link> pLinks)
	{
		this.html = pHtml;
		this.sourceEntities = pSourceEntities;
		this.links = pLinks;
	}

	/**
	 * @return the svg source
	 */
	public String getHtml()
	{
		return html;
	}

	/**
	 * @return the requirement source alias
	 */
	public Map<String, InputRequirementSource> getSourceEntities()
	{
		return sourceEntities;
	}

	/**
	 * @return the links identifiers
	 */
	public Map<String, Link> getLinks()
	{
		return links;
	}

	public static class Link
	{
		private final String					line;

		private final InputRequirementSource	source1;

		private final RequirementImpl			req1;

		private final InputRequirementSource	source2;

		private final RequirementImpl			req2;

		public Link(final String pLine, final InputRequirementSource pSource1, final RequirementImpl pReq1,
				final InputRequirementSource pSource2, final RequirementImpl pReq2)
		{
			this.line = pLine;
			this.source1 = pSource1;
			this.req1 = pReq1;
			this.source2 = pSource2;
			this.req2 = pReq2;

		}

		/**
		 * @return the line
		 */
		public String getLine()
		{
			return line;
		}

		/**
		 * @return the source1
		 */
		public InputRequirementSource getSource1()
		{
			return source1;
		}

		/**
		 * @return the req1
		 */
		public RequirementImpl getReq1()
		{
			return req1;
		}

		/**
		 * @return the source2
		 */
		public InputRequirementSource getSource2()
		{
			return source2;
		}

		/**
		 * @return the req2
		 */
		public RequirementImpl getReq2()
		{
			return req2;
		}
	}
}
