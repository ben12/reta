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
import java.util.Collections;
import java.util.List;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ConcatReader extends Reader
{
	private final List<Reader> readers;

	private int index = 0;

	private Reader reader = null;

	public ConcatReader(List<Reader> readers)
	{
		this.readers = Collections.unmodifiableList(readers);
	}

	private void next()
	{
		if (index < readers.size())
		{
			reader = readers.get(index);
			index++;
		}
		else
		{
			reader = null;
		}
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
			if (r == -1)
			{
				reader.close();
				next();
				r = read(cbuf, off, len);
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
		}
	}

}
