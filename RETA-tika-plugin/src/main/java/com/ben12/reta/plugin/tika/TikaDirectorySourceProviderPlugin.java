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

import java.util.ResourceBundle;

/**
 * @author Benoît Moreau (ben.12)
 */
public class TikaDirectorySourceProviderPlugin extends TikaSourceProviderPlugin
{

	private static final String		PROVIDER_NAME	= "tika.dir.provider";

	private final ResourceBundle	labels			= ResourceBundle.getBundle("com/ben12/reta/plugin/tika/Labels");

	/**
	 * 
	 */
	public TikaDirectorySourceProviderPlugin()
	{
	}

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
}
