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
	private final String						name;

	private String								source				= "";

	private String								filter				= ".*";

	private TreeSet<Requirement>				requirements		= new TreeSet<>();

	private String								reqStart			= "";

	private String								reqEnd				= "";

	private String								reqRef				= "";

	private List<InputRequirementSource>		covers				= new ArrayList<>();

	private Map<InputRequirementSource, Double>	coversBy			= new HashMap<>();

	private Map<String, Integer>				attributesGroup		= new HashMap<>();

	private Map<String, Integer>				refAttributesGroup	= null;

	public InputRequirementSource(String name, String source, String filter)
	{
		this.name = name;
		this.source = Objects.requireNonNull(source);
		this.filter = ((filter == null || filter.isEmpty()) ? null : filter);
	}

	public String getName()
	{
		return name;
	}

	public String getSource()
	{
		return source;
	}

	public String getFilter()
	{
		return filter;
	}

	public String getReqStart()
	{
		return reqStart;
	}

	public void setReqStart(String reqStart)
	{
		this.reqStart = reqStart;
	}

	public String getReqEnd()
	{
		return reqEnd;
	}

	public void setReqEnd(String reqEnd)
	{
		this.reqEnd = reqEnd;
	}

	public String getReqRef()
	{
		return reqRef;
	}

	public void setReqRef(String reqRef)
	{
		this.reqRef = reqRef;
	}

	public TreeSet<Requirement> getRequirements()
	{
		return requirements;
	}

	public Map<String, Integer> getAttributesGroup()
	{
		return attributesGroup;
	}

	public Map<String, Integer> getRefAttributesGroup()
	{
		if (refAttributesGroup == null)
		{
			refAttributesGroup = new HashMap<>();
		}
		return refAttributesGroup;
	}

	public List<InputRequirementSource> getCovers()
	{
		return covers;
	}

	public Map<InputRequirementSource, Double> getCoversBy()
	{
		return coversBy;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Input ");
		builder.append(name);
		builder.append(" (");
		builder.append(source);
		builder.append("):\n");
		for (Requirement req : requirements)
		{
			builder.append(req.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
