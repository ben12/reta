/**
 * 
 */
package com.ben12.reta.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

/**
 * @author Benoît
 * 
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
