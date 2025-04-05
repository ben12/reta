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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.google.common.base.Strings;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import com.ben12.reta.api.Requirement;
import com.ben12.reta.api.RequirementSourceManager;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.constraints.NotNullElement;
import com.ben12.reta.plugin.SourceProviderPlugin;

/**
 * @author Benoît Moreau (ben.12)
 */
public class InputRequirementSource implements RequirementSourceManager
{
	/** {@link #name} property name. */
	public static final String							NAME					= "name";

	/** {@link #covers} property name. */
	public static final String							COVERS					= "covers";

	/** Source document name. */
	@NotEmpty
	@Pattern(regexp = "[^,]*")
	private final StringProperty						name					= new SimpleStringProperty(this, NAME);

	/** Source provider plugin. */
	private final SourceProviderPlugin					provider;

	/** List of covered document sources by this document source. */
	@NotNullElement(message = "{unknown.cover.source}")
	private final List<InputRequirementSource>			covers					= new ArrayList<>();

	/** Set of requirement found in the document. */
	private final TreeSet<RequirementImpl>				requirements			= new TreeSet<>();

	/** Coverage rate of this document by the other documents. */
	private final Map<InputRequirementSource, Double>	coversBy				= new HashMap<>();

	/** Plug-in source configuration. */
	private final SourceConfiguration					configuration;

	/** Requirement attribute names added. */
	private final Set<String>							requirementAttributes	= new LinkedHashSet<>();

	/** Reference attribute names added. */
	private final Set<String>							referenceAttributes		= new LinkedHashSet<>();

	/**
	 * @param theName
	 *            document source name
	 * @param theProvider
	 *            plug-in source provider
	 * @param theConfiguration
	 *            plug-in source configuration
	 */
	public InputRequirementSource(final String theName, final SourceProviderPlugin theProvider,
			final SourceConfiguration theConfiguration)
	{
		name.set(theName);
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
		requirementAttributes.clear();
	}

	/**
	 * @return requirement source name property
	 */
	public StringProperty nameProperty()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.RequirementSourceManager#getName()
	 */
	@Override
	public String getName()
	{
		return name.get();
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
	public TreeSet<RequirementImpl> getRequirements()
	{
		return requirements;
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public List<RequirementImpl> getAllReferences()
	{
		return requirements.stream().flatMap((r) -> r.getReferences().stream()).distinct().collect(Collectors.toList());
	}

	/**
	 * @return the set of requirement found in the document
	 */
	public List<RequirementImpl> getAllUknownReferences()
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
	 * @return all requirement attributes set
	 */
	public Set<String> getRequirementAttributes()
	{
		return requirementAttributes;
	}

	/**
	 * @param att
	 *            requirement attribute name to add
	 */
	public void addRequirementAttribute(final String att)
	{
		requirementAttributes.add(att);
	}

	/**
	 * @return all reference attributes set
	 */
	public Set<String> getReferenceAttributes()
	{
		return referenceAttributes;
	}

	/**
	 * @param att
	 *            reference attribute name to add
	 */
	public void addReferenceAttribute(final String att)
	{
		referenceAttributes.add(att);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.RequirementSourceManager#addRequirement(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public Requirement addRequirement(final String summary, final String id, final String version, final String content,
			final Map<String, String> attributes)
	{
		RequirementImpl requirement = new RequirementImpl(this);
		requirement.setText(summary);
		requirement.setId(id);
		requirement.setVersion(version);
		requirement.setContent(content);
		for (final Map.Entry<String, String> att : attributes.entrySet())
		{
			requirement.putAttribute(att.getKey(), att.getValue());
			requirementAttributes.add(att.getKey());
		}
		// register Version attribute if exists
		if (!Strings.isNullOrEmpty(version))
		{
			requirementAttributes.add(SourceConfiguration.ATTRIBUTE_VERSION);
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
		builder.append(name.get());
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
