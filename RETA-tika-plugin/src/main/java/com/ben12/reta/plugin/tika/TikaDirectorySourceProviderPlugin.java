// Package : com.ben12.reta.plugin.tika
// File : TikaDirectorySourceProviderPlugin.java
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
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.plugin.tika.model.TikaSourceConfiguration;
import com.ben12.reta.plugin.tika.view.SourceConfigurationController;

/**
 * Tika directory requirement source provider plug-in.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class TikaDirectorySourceProviderPlugin extends TikaSourceProviderPlugin
{
	/** {@link TikaDirectorySourceProviderPlugin} {@link Logger}. */
	private static final Logger		LOGGER			= Logger
			.getLogger(TikaDirectorySourceProviderPlugin.class.getName());

	/** Directory requirement source provider plug-in name translation key. */
	private static final String		PROVIDER_NAME	= "tika.dir.provider";

	/** Translations {@link ResourceBundle}. */
	private final ResourceBundle	labels			= ResourceBundle.getBundle("com/ben12/reta/plugin/tika/Labels");

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.tika.TikaSourceProviderPlugin#getSourceName()
	 */
	@Override
	public String getSourceName()
	{
		return labels.getString(PROVIDER_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.plugin.tika.TikaSourceProviderPlugin#createSourceConfigurationEditor(com.ben12.reta.api.SourceConfiguration)
	 */
	@Override
	public Node createSourceConfigurationEditor(final SourceConfiguration sourceConfiguration,
			final BufferingManager bufferingManager)
	{
		Node node = null;

		if (sourceConfiguration instanceof TikaSourceConfiguration)
		{
			final FXMLLoader loader = new FXMLLoader();
			loader.setLocation(SourceConfigurationController.class.getResource("FolderSourceConfigurationUI.fxml"));
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
}
