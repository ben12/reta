// Package : com.ben12.reta.plugin.tika.model
// File : TikaSourceConfiguration.java
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
package com.ben12.reta.plugin.tika.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.valuehandling.UnwrapValidatedValue;

import com.ben12.reta.api.RETAParseException;
import com.ben12.reta.api.RequirementSourceManager;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.constraints.IsPath;
import com.ben12.reta.beans.constraints.PathExists;
import com.ben12.reta.plugin.tika.beans.constraints.Regex;
import com.ben12.reta.plugin.tika.parser.RetaTikaParser;

/**
 * @author Benoît Moreau (ben.12)
 */
public class TikaSourceConfiguration implements SourceConfiguration
{
	/** {@link #sourcePath} property name. */
	public static final String			SOURCE_PATH			= "sourcePath";

	/** {@link #filter} property name. */
	public static final String			FILTER				= "filter";

	/** {@link #reqStart} property name. */
	public static final String			REQ_START			= "reqStart";

	/** {@link #reqEnd} property name. */
	public static final String			REQ_END				= "reqEnd";

	/** {@link #reqRef} property name. */
	public static final String			REQ_REF				= "reqRef";

	/**
	 * Source document path.
	 */
	@NotNull
	@IsPath
	@PathExists
	@UnwrapValidatedValue
	private final StringProperty		sourcePath			= new SimpleStringProperty(this, SOURCE_PATH, "");

	/**
	 * Regular expression filtering files if {@link #sourcePath} is a folder.
	 */
	@NotNull
	@Regex
	@UnwrapValidatedValue
	private final StringProperty		filter				= new SimpleStringProperty(this, FILTER, "");

	/**
	 * Regular expression for find the start of requirement.
	 */
	@NotNull
	@Regex
	@UnwrapValidatedValue
	private final StringProperty		reqStart			= new SimpleStringProperty(this, REQ_START, "");

	/**
	 * Regular expression for find the end of requirement.
	 */
	@NotNull
	@Regex
	@UnwrapValidatedValue
	private final StringProperty		reqEnd				= new SimpleStringProperty(this, REQ_END, "");

	/**
	 * Regular expression for find the requirement references.
	 */
	@NotNull
	@Regex
	@UnwrapValidatedValue
	private final StringProperty		reqRef				= new SimpleStringProperty(this, REQ_REF, "");

	/**
	 * Requirement attribute indexes in regular expression {@link #reqStart} captions.
	 */
	private final Map<String, Integer>	attributesGroup		= new HashMap<>();

	/**
	 * Reference attribute indexes in regular expression {@link #reqRef} captions.
	 */
	private final Map<String, Integer>	refAttributesGroup	= new HashMap<>();

	/**
	 * RETA parser using Tika.
	 */
	private RetaTikaParser				parser				= null;

	/**
	 * 
	 */
	public TikaSourceConfiguration()
	{
		attributesGroup.put(ATTRIBUTE_ID, 0);
		attributesGroup.put(ATTRIBUTE_TEXT, 0);
	}

	/**
	 * @return the document source path
	 */
	public StringProperty sourcePathProperty()
	{
		return sourcePath;
	}

	/**
	 * @return regular expression filtering files if {@link #sourcePath} is a folder
	 */
	public StringProperty filterProperty()
	{
		return filter;
	}

	/**
	 * @return regular expression of requirement start
	 */
	public StringProperty reqStartProperty()
	{
		return reqStart;
	}

	/**
	 * @return regular expression of requirement end
	 */
	public StringProperty reqEndProperty()
	{
		return reqEnd;
	}

	/**
	 * @return regular expression of requirement references
	 */
	public StringProperty reqRefProperty()
	{
		return reqRef;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.SourceConfiguration#parseSource(com.ben12.reta.api.RequirementSourceManager)
	 */
	@Override
	public void parseSource(final RequirementSourceManager manager) throws RETAParseException
	{
		if (parser == null)
		{
			parser = new RetaTikaParser(this);
		}

		try
		{
			parser.parse(manager, null, Integer.MAX_VALUE);
		}
		catch (final IOException e)
		{
			throw new RETAParseException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.api.SourceConfiguration#parseSourcePreview(com.ben12.reta.api.RequirementSourceManager, java.lang.StringBuilder, int)
	 */
	@Override
	public void parseSourcePreview(final RequirementSourceManager manager, final StringBuilder output, final int limit)
			throws RETAParseException
	{
		if (parser == null)
		{
			parser = new RetaTikaParser(this);
		}

		try
		{
			parser.parse(manager, output, limit);
		}
		catch (final IOException e)
		{
			throw new RETAParseException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.valueOf(sourcePath.get());
	}
}
