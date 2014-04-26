// Package : com.ben12.reta.model
// File : InputRequirementSource.java
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

package com.ben12.reta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * @author Benoît Moreau (ben.12)
 */
public class InputRequirementSource
{
	/**
	 * Source document name.
	 */
	private final String								name;

	/**
	 * Source document path.
	 */
	private final String								sourcePath;

	/**
	 * Regular expression filtering files if {@link #sourcePath} is a folder.
	 */
	private final String								filter;

	/**
	 * Regular expression for find the start of requirement.
	 */
	private String										reqStart			= "";

	/**
	 * Regular expression for find the end of requirement.
	 */
	private String										reqEnd				= "";

	/**
	 * Regular expression for find the requirement references.
	 */
	private String										reqRef				= "";

	/**
	 * Requirement attribute indexes in regular expression {@link #reqStart} captions.
	 */
	private final Map<String, Integer>					attributesGroup		= new HashMap<>();

	/**
	 * Reference attribute indexes in regular expression {@link #reqRef} captions.
	 */
	private final Map<String, Integer>					refAttributesGroup	= new HashMap<>();

	/**
	 * List of covered document sources by this document source.
	 */
	private final List<InputRequirementSource>			covers				= new ArrayList<>();

	/**
	 * Set of requirement found in the document.
	 */
	private final TreeSet<Requirement>					requirements		= new TreeSet<>();

	/**
	 * Coverage rate of this document by the other documents.
	 */
	private final Map<InputRequirementSource, Double>	coversBy			= new HashMap<>();

	/**
	 * @param name
	 *            document source name
	 * @param sourcePath
	 *            document source path
	 * @param filter
	 *            regular expression filtering files if {@link #sourcePath} is a folder
	 */
	public InputRequirementSource(String name, String sourcePath, String filter)
	{
		this.name = name;
		this.sourcePath = Objects.requireNonNull(sourcePath);
		this.filter = ((filter == null || filter.isEmpty()) ? null : filter);
	}

	/**
	 * @return the document source name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the document source path
	 */
	public String getSourcePath()
	{
		return sourcePath;
	}

	/**
	 * @return regular expression filtering files if {@link #sourcePath} is a folder
	 */
	public String getFilter()
	{
		return filter;
	}

	/**
	 * @return regular expression of requirement start
	 */
	public String getReqStart()
	{
		return reqStart;
	}

	/**
	 * @param reqStart
	 *            regular expression of requirement start
	 */
	public void setReqStart(String reqStart)
	{
		this.reqStart = reqStart;
	}

	/**
	 * @return regular expression of requirement end
	 */
	public String getReqEnd()
	{
		return reqEnd;
	}

	/**
	 * @param reqEnd
	 *            regular expression of requirement end
	 */
	public void setReqEnd(String reqEnd)
	{
		this.reqEnd = reqEnd;
	}

	/**
	 * @return regular expression of requirement references
	 */
	public String getReqRef()
	{
		return reqRef;
	}

	/**
	 * @param reqRef
	 *            regular expression of requirement references
	 */
	public void setReqRef(String reqRef)
	{
		this.reqRef = reqRef;
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public TreeSet<Requirement> getRequirements()
	{
		return requirements;
	}

	/**
	 * @return attribute indexes in regular expression {@link #reqStart} captions
	 */
	public Map<String, Integer> getAttributesGroup()
	{
		return attributesGroup;
	}

	/**
	 * @return attribute indexes in regular expression {@link #reqRef} captions
	 */
	public Map<String, Integer> getRefAttributesGroup()
	{
		return refAttributesGroup;
	}

	/**
	 * @return list of covered document sources by this document source
	 */
	public List<InputRequirementSource> getCovers()
	{
		return covers;
	}

	/**
	 * @return coverage rate of this document by the other documents
	 */
	public Map<InputRequirementSource, Double> getCoversBy()
	{
		return coversBy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Input ");
		builder.append(name);
		builder.append(" (");
		builder.append(sourcePath);
		builder.append("):\n");
		for (Requirement req : requirements)
		{
			builder.append(req.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
