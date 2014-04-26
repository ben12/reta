// Package : com.ben12.reta.util
// File : ConcatReader.java
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

package com.ben12.reta.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.Tika;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ConcatReader extends Reader
{
	/** Class logger. */
	private static final Logger	logger	= Logger.getLogger(ConcatReader.class.getName());

	/** Tika facade instance. */
	private final static Tika	TIKA	= new Tika();

	/** File paths to read. */
	private final List<Path>	paths	= new ArrayList<>();

	/** Current file index read. */
	private int					index	= -1;

	/** Current opened reader. */
	private Reader				reader	= null;

	/**
	 * @param path
	 *            path of file to read
	 */
	public void add(Path path)
	{
		Objects.requireNonNull(path);
		paths.add(path);
	}

	/**
	 * Opens the next file reader and closes the previous
	 * 
	 * @throws IOException
	 *             I/O exception
	 */
	private void next() throws IOException
	{
		if (reader != null)
		{
			reader.close();
		}
		if (index + 1 < paths.size())
		{
			index++;
			reader = createReader(paths.get(index));
		}
		else
		{
			reader = null;
		}
	}

	/**
	 * @return current path of file read
	 */
	public Path getCurrentPath()
	{
		Path path = null;
		if (reader != null && index < paths.size())
		{
			path = paths.get(index);
		}
		else if (index == -1 && !paths.isEmpty())
		{
			path = paths.get(0);
		}
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#read(char[], int, int)
	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException
	{
		int r = -1;
		if (reader == null)
		{
			next();
		}
		if (reader != null)
		{
			r = reader.read(cbuf, off, len);
			while (r == -1 && reader != null)
			{
				next();
				if (reader != null)
				{
					r = reader.read(cbuf, off, len);
				}
			}
		}
		return r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() throws IOException
	{
		if (reader != null)
		{
			reader.close();
			reader = null;
		}
	}

	/**
	 * @param path
	 *            file path
	 * @return the reader
	 * @throws IOException
	 *             I/O exception
	 */
	public Reader createReader(Path path) throws IOException
	{
		if (logger.isLoggable(Level.FINE))
		{
			logger.fine("Open " + path + " : " + TIKA.detect(path.toFile()));
		}
		return TIKA.parse(path.toFile());
	}
}
