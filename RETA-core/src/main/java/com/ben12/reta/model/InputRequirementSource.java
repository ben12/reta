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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;

import com.ben12.reta.api.RequirementSourceManager;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.constraints.NotNullElement;
import com.ben12.reta.plugin.SourceProviderPlugin;

/**
 * @author Benoît Moreau (ben.12)
 */
public class InputRequirementSource implements RequirementSourceManager
{
	public static final String							NAME			= "name";

	public static final String							COVERS			= "covers";

	/**
	 * Source document name.
	 */
	@NotEmpty
	@Pattern(regexp = "[^,]*")
	private String										name;

	private final SourceProviderPlugin					provider;

	/**
	 * List of covered document sources by this document source.
	 */
	@NotNullElement(message = "{unknown.cover.source}")
	private final List<InputRequirementSource>			covers			= new ArrayList<>();

	/**
	 * Set of requirement found in the document.
	 */
	private final TreeSet<Requirement>					requirements	= new TreeSet<>();

	/**
	 * Coverage rate of this document by the other documents.
	 */
	private final Map<InputRequirementSource, Double>	coversBy		= new HashMap<>();

	/**
	 * Plug-in source configuration.
	 */
	private final SourceConfiguration					configuration;

	private final Set<String>							allAttributes	= new LinkedHashSet<>();

	/**
	 * @param theName
	 *            document source name
	 * @param theConfiguration
	 *            plug-in source configuration
	 */
	public InputRequirementSource(final String theName, final SourceProviderPlugin theProvider,
			final SourceConfiguration theConfiguration)
	{
		name = theName;
		provider = theProvider;
		configuration = theConfiguration;
	}

	/**
	 * Clear previous analysis results.
	 */
	public void clear()
	{
		requirements.clear();
		coversBy.clear();
		allAttributes.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.RequirementSourceManager#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * @param newName
	 *            the name to set
	 */
	public void setName(final String newName)
	{
		name = newName;
	}

	/**
	 * @return the provider
	 */
	public SourceProviderPlugin getProvider()
	{
		return provider;
	}

	/**
	 * @return the configuration
	 */
	public SourceConfiguration getConfiguration()
	{
		return configuration;
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public TreeSet<Requirement> getRequirements()
	{
		return requirements;
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public List<Requirement> getAllReferences()
	{
		return requirements.stream().flatMap((r) -> r.getReferences().stream()).distinct().collect(Collectors.toList());
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public List<Requirement> getAllUknownReferences()
	{
		return getAllReferences().stream().filter((r) -> r.getSource() == null).collect(Collectors.toList());
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

	/**
	 * @return all attributes set
	 */
	public Set<String> getAllAttributes()
	{
		return allAttributes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.RequirementSourceManager#addRequirement(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public Requirement addRequirement(final String summary, final String id, final String version,
			final String content, final Map<String, String> attributes)
	{
		Requirement requirement = new Requirement(this);
		requirement.setText(summary);
		requirement.setId(id);
		requirement.setVersion(version);
		requirement.setContent(content);
		for (final Map.Entry<String, String> att : attributes.entrySet())
		{
			requirement.putAttribute(att.getKey(), att.getValue());
			allAttributes.add(att.getKey());
		}
		if (!requirements.add(requirement))
		{
			// already exists
			requirement = null;
		}
		return requirement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("Input ");
		builder.append(name);
		builder.append(" (");
		builder.append(configuration.toString());
		builder.append("):\n\n");
		for (final Requirement req : requirements)
		{
			builder.append("--------------------------------------------------------------------\n");
			builder.append(req.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
