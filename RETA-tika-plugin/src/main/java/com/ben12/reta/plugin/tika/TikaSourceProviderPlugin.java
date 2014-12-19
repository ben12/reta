// Package : com.ben12.reta.plugin.tika
// File : TikaSourceProviderPlugin.java
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
package com.ben12.reta.plugin.tika;

import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.Node;

import org.ini4j.Profile.Section;

import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.plugin.tika.model.TikaSourceConfiguration;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public class TikaSourceProviderPlugin implements SourceProviderPlugin
{

	private static final Logger		LOGGER			= Logger.getLogger(TikaSourceProviderPlugin.class.getName());

	private static final String		PROVIDER_NAME	= "tika.file.provider";

	private final ResourceBundle	labels			= ResourceBundle.getBundle("com/ben12/reta/plugin/tika/Labels");

	/**
	 * 
	 */
	public TikaSourceProviderPlugin()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#getSourceName()
	 */
	@Override
	public String getSourceName()
	{
		return labels.getString(PROVIDER_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#saveSourceConfiguration(com.ben12.reta.api.SourceConfiguration, org.ini4j.Profile.Section, java.lang.Iterable)
	 */
	@Override
	public void saveSourceConfiguration(final SourceConfiguration sourceConfiguration, final Section section)
	{
		final TikaSourceConfiguration tikaConfiguration = (TikaSourceConfiguration) sourceConfiguration;

		section.put("path", tikaConfiguration.getSourcePath());
		section.put("filter", tikaConfiguration.getFilter());

		final String reqStart = tikaConfiguration.getReqStart();
		if (!Strings.isNullOrEmpty(reqStart))
		{
			final Map<String, Integer> reqAttributesGroup = tikaConfiguration.getAttributesGroup();

			section.put("requirement.start.attributes",
					reqAttributesGroup.keySet().stream().collect(Collectors.joining(",")));
			section.put("requirement.start.regex", reqStart);

			for (final Entry<String, Integer> reqAttributeGroup : reqAttributesGroup.entrySet())
			{
				if (reqAttributeGroup.getValue() != null)
				{
					section.put("requirement.start." + reqAttributeGroup.getKey() + ".index",
							reqAttributeGroup.getValue());
				}
			}

			final String reqEnd = tikaConfiguration.getReqEnd();
			if (!Strings.isNullOrEmpty(reqEnd))
			{
				section.put("requirement.end.regex", reqEnd);
			}
		}

		final String reqRef = tikaConfiguration.getReqRef();
		if (!Strings.isNullOrEmpty(reqRef))
		{
			final Map<String, Integer> refAttributesGroup = tikaConfiguration.getRefAttributesGroup();

			section.put("requirement.ref.attributes",
					refAttributesGroup.keySet().stream().collect(Collectors.joining(",")));
			section.put("requirement.ref.regex", reqRef);

			for (final Entry<String, Integer> refAttributeGroup : refAttributesGroup.entrySet())
			{
				if (refAttributeGroup.getValue() != null)
				{
					section.put("requirement.ref." + refAttributeGroup.getKey() + ".index",
							refAttributeGroup.getValue());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#loadSourceConfiguration(org.ini4j.Profile.Section, java.lang.Iterable)
	 */
	@Override
	public TikaSourceConfiguration loadSourceConfiguration(final Section section)
	{
		TikaSourceConfiguration configuration = null;
		final String source = section.get("path", "");
		final String filter = section.get("filter", "");
		if (!source.isEmpty())
		{
			configuration = new TikaSourceConfiguration(source, filter);

			try
			{
				final Iterable<String> attributes = Splitter.on(',')
						.trimResults()
						.omitEmptyStrings()
						.split(section.get("requirement.start.attributes", ""));
				final String startRegex = section.get("requirement.start.regex", "");
				final String endRegex = section.get("requirement.end.regex", "");
				final Integer textIndex = section.get("requirement.start." + SourceConfiguration.ATTRIBUTE_TEXT
						+ ".index", Integer.class, 0);
				Integer idIndex = section.get("requirement.start." + SourceConfiguration.ATTRIBUTE_ID + ".index",
						Integer.class, null);
				final Integer versionIndex = section.get("requirement.start." + SourceConfiguration.ATTRIBUTE_VERSION
						+ ".index", Integer.class, null);

				if (startRegex.isEmpty())
				{
					if (!endRegex.isEmpty())
					{
						LOGGER.severe("requirement.start.regex is mandatory for section " + section.getName());
					}
				}
				else if (idIndex == null)
				{
					LOGGER.severe("requirement.start.Id.index is mandatory for section " + section.getName());
					idIndex = 0;
				}

				configuration.setReqStart(startRegex);
				configuration.setReqEnd(endRegex);
				configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_TEXT, textIndex);
				configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_ID, idIndex);
				if (versionIndex != null)
				{
					configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_VERSION, versionIndex);
				}

				for (final String att : attributes)
				{
					final Integer attIndex = section.get("requirement.start." + att + ".index", Integer.class, null);
					if (attIndex != null)
					{
						configuration.getAttributesGroup().put(att, attIndex);
					}
				}

				final String refRegex = section.get("requirement.ref.regex", "");
				if (!refRegex.isEmpty())
				{
					final Iterable<String> refAttributes = Splitter.on(',')
							.trimResults()
							.omitEmptyStrings()
							.split(section.get("requirement.ref.attributes", ""));
					final Integer refIdIndex = section.get("requirement.ref." + SourceConfiguration.ATTRIBUTE_ID
							+ ".index", Integer.class, null);
					final Integer refVersionIndex = section.get("requirement.ref."
							+ SourceConfiguration.ATTRIBUTE_VERSION + ".index", Integer.class, null);

					if (refIdIndex == null)
					{
						LOGGER.severe("requirement.ref.Id.index is mandatory for section " + section.getName());
						throw new IllegalArgumentException();
					}

					configuration.setReqRef(refRegex);
					configuration.getRefAttributesGroup().put(SourceConfiguration.ATTRIBUTE_ID, refIdIndex);
					if (refVersionIndex != null)
					{
						configuration.getRefAttributesGroup().put(SourceConfiguration.ATTRIBUTE_VERSION,
								refVersionIndex);
					}

					for (final String att : refAttributes)
					{
						final Integer attIndex = section.get("requirement.ref." + att + ".index", Integer.class, null);
						if (attIndex != null)
						{
							configuration.getRefAttributesGroup().put(att, attIndex);
						}
					}
				}
			}
			catch (final IllegalArgumentException e)
			{
				LOGGER.log(Level.SEVERE, "Invalid value for section " + section.getName(), e);
				configuration = null;
			}
		}
		return configuration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#createNewSourceConfiguration()
	 */
	@Override
	public TikaSourceConfiguration createNewSourceConfiguration()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#createSourceConfigurationEditor(com.ben12.reta.api.SourceConfiguration)
	 */
	@Override
	public Node createSourceConfigurationEditor(final SourceConfiguration sourceConfiguration)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
