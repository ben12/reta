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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import org.ini4j.Profile.Section;

import com.google.common.base.Strings;

import com.ben12.reta.api.RETAParser;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.plugin.tika.model.TikaSourceConfiguration;
import com.ben12.reta.plugin.tika.parser.RetaTikaParser;
import com.ben12.reta.plugin.tika.view.SourceConfigurationController;

/**
 * Tika requirement source provider plug-in.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class TikaSourceProviderPlugin implements SourceProviderPlugin
{
	/** {@link TikaSourceProviderPlugin} {@link Logger}. */
	private static final Logger		LOGGER			= Logger.getLogger(TikaSourceProviderPlugin.class.getName());

	/** File requirement source provider plug-in name translation key. */
	private static final String		PROVIDER_NAME	= "tika.file.provider";

	/** Translations {@link ResourceBundle}. */
	private final ResourceBundle	labels			= ResourceBundle.getBundle("com/ben12/reta/plugin/tika/Labels");

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

		section.put("path", tikaConfiguration.sourcePathProperty().get());
		section.put("filter", Strings.nullToEmpty(tikaConfiguration.filterProperty().get()));

		final StringProperty reqStart = tikaConfiguration.reqStartProperty();
		if (reqStart.isNotEmpty().get())
		{
			final Map<String, Integer> reqAttributesGroup = tikaConfiguration.getAttributesGroup();

			section.put("requirement.start.regex", reqStart.get());

			for (final Entry<String, Integer> reqAttributeGroup : reqAttributesGroup.entrySet())
			{
				if (reqAttributeGroup.getValue() != null)
				{
					section.put("requirement.start." + reqAttributeGroup.getKey() + ".index",
							reqAttributeGroup.getValue());
				}
			}

			final StringProperty reqEnd = tikaConfiguration.reqEndProperty();
			if (reqEnd.isNotEmpty().get())
			{
				section.put("requirement.end.regex", reqEnd.get());
			}
		}

		final StringProperty reqRef = tikaConfiguration.reqRefProperty();
		if (reqRef.isNotEmpty().get())
		{
			final Map<String, Integer> refAttributesGroup = tikaConfiguration.getRefAttributesGroup();

			section.put("requirement.ref.regex", reqRef.get());

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
			configuration = new TikaSourceConfiguration();
			configuration.sourcePathProperty().set(source);
			configuration.filterProperty().set(filter);

			try
			{
				final String startRegex = section.get("requirement.start.regex", "");
				final String endRegex = section.get("requirement.end.regex", "");
				final Integer textIndex = section
						.get("requirement.start." + SourceConfiguration.ATTRIBUTE_TEXT + ".index", Integer.class, 0);
				Integer idIndex = section.get("requirement.start." + SourceConfiguration.ATTRIBUTE_ID + ".index",
						Integer.class, null);
				final Integer versionIndex = section.get(
						"requirement.start." + SourceConfiguration.ATTRIBUTE_VERSION + ".index", Integer.class, null);

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

				configuration.reqStartProperty().set(startRegex);
				configuration.reqEndProperty().set(endRegex);
				configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_TEXT, textIndex);
				configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_ID, idIndex);
				if (versionIndex != null)
				{
					configuration.getAttributesGroup().put(SourceConfiguration.ATTRIBUTE_VERSION, versionIndex);
				}

				final String reqStartIndexPrefix = "requirement.start.";
				final String reqStartIndexSuffix = ".index";
				for (final String key : section.keySet())
				{
					if (key.startsWith(reqStartIndexPrefix) && key.endsWith(reqStartIndexSuffix))
					{
						final String att = key.substring(reqStartIndexPrefix.length(),
								key.length() - reqStartIndexSuffix.length());
						final Integer attIndex = section.get(key, Integer.class, null);
						if (attIndex != null)
						{
							configuration.getAttributesGroup().put(att, attIndex);
						}
					}
				}

				final String refRegex = section.get("requirement.ref.regex", "");
				if (!refRegex.isEmpty())
				{
					final Integer refIdIndex = section
							.get("requirement.ref." + SourceConfiguration.ATTRIBUTE_ID + ".index", Integer.class, null);
					final Integer refVersionIndex = section.get(
							"requirement.ref." + SourceConfiguration.ATTRIBUTE_VERSION + ".index", Integer.class, null);

					if (refIdIndex == null)
					{
						LOGGER.severe("requirement.ref.Id.index is mandatory for section " + section.getName());
						throw new IllegalArgumentException();
					}

					configuration.reqRefProperty().set(refRegex);
					configuration.getRefAttributesGroup().put(SourceConfiguration.ATTRIBUTE_ID, refIdIndex);
					if (refVersionIndex != null)
					{
						configuration.getRefAttributesGroup().put(SourceConfiguration.ATTRIBUTE_VERSION,
								refVersionIndex);
					}

					final String refStartIndexPrefix = "requirement.ref.";
					final String refStartIndexSuffix = ".index";
					for (final String key : section.keySet())
					{
						if (key.startsWith(refStartIndexPrefix) && key.endsWith(refStartIndexSuffix))
						{
							final String att = key.substring(refStartIndexPrefix.length(),
									key.length() - refStartIndexSuffix.length());
							final Integer attIndex = section.get(key, Integer.class, null);
							if (attIndex != null)
							{
								configuration.getRefAttributesGroup().put(att, attIndex);
							}
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
		return new TikaSourceConfiguration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#createSourceConfigurationEditor(com.ben12.reta.api.SourceConfiguration, com.ben12.reta.beans.property.buffering.BufferingManager)
	 */
	@Override
	public Node createSourceConfigurationEditor(final SourceConfiguration sourceConfiguration,
			final BufferingManager bufferingManager)
	{
		Node node = null;

		if (sourceConfiguration instanceof TikaSourceConfiguration)
		{
			final FXMLLoader loader = new FXMLLoader();
			loader.setLocation(SourceConfigurationController.class.getResource("FileSourceConfigurationUI.fxml"));
			loader.setResources(labels);

			try
			{
				node = loader.load();
				final SourceConfigurationController controller = loader.getController();

				controller.bind(bufferingManager, (TikaSourceConfiguration) sourceConfiguration);
			}
			catch (final IOException e)
			{
				LOGGER.log(Level.SEVERE, "Loading Tika FXML", e);
			}
		}

		return node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.SourceProviderPlugin#createParser(com.ben12.reta.api.SourceConfiguration)
	 */
	@Override
	public RETAParser createParser(final SourceConfiguration sourceConfiguration)
	{
		RETAParser parser = null;
		if (sourceConfiguration instanceof TikaSourceConfiguration)
		{
			parser = new RetaTikaParser((TikaSourceConfiguration) sourceConfiguration);
		}
		return parser;
	}
}
