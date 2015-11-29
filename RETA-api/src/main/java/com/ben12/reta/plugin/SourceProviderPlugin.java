// Package : com.ben12.reta.plugin
// File : SourceProviderPlugin.java
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
package com.ben12.reta.plugin;

import javafx.scene.Node;

import org.ini4j.Profile.Section;

import com.ben12.reta.api.RETAParser;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.property.buffering.BufferingManager;

/**
 * Plug-in interface to implement.
 * 
 * @author Benoît Moreau (ben.12)
 */
public interface SourceProviderPlugin
{
	/**
	 * @return source name exposed to users
	 */
	String getSourceName();

	/**
	 * Save the {@link SourceConfiguration} of this plug-in in the dedicated INI section.
	 * 
	 * @param sourceConfiguration
	 *            source configuration to save
	 * @param iniSection
	 *            INI section where source configuration must be saved
	 */
	void saveSourceConfiguration(SourceConfiguration sourceConfiguration, Section iniSection);

	/**
	 * Load the {@link SourceConfiguration} of this plug-in from the dedicated INI section.
	 * 
	 * @param iniSection
	 *            INI section where source configuration must be read
	 * @return source configuration read
	 */
	SourceConfiguration loadSourceConfiguration(Section iniSection);

	/**
	 * Creation of an empty or default {@link SourceConfiguration} for this plug-in.
	 * 
	 * @return new source configuration
	 */
	SourceConfiguration createNewSourceConfiguration();

	/**
	 * Create a new JavaFX GUI for the edition of the {@link SourceConfiguration} of this plug-in.
	 * 
	 * @param sourceConfiguration
	 *            source configuration to edit
	 * @param bufferingManager
	 *            buffering manager to use
	 * @return JavaFX GUI for source configuration edition
	 */
	Node createSourceConfigurationEditor(SourceConfiguration sourceConfiguration, BufferingManager bufferingManager);

	/**
	 * Create a new parser for the requirement source configuration.
	 * 
	 * @param configuration
	 *            source configuration to use
	 * @return the parser to use for the requirement source configuration
	 */
	RETAParser createParser(SourceConfiguration configuration);
}
