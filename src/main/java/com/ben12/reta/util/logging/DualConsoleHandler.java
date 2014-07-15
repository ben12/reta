// Package : com.ben12.reta.util.logging
// File : DualConsoleHandler.java
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
package com.ben12.reta.util.logging;

import java.io.UnsupportedEncodingException;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * @author Benoît Moreau (ben.12)
 */
public final class DualConsoleHandler extends ConsoleHandler
{
	private final StreamHandler	stdOutHandler	= new StreamHandler(System.out, new SimpleFormatter());

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#setLevel(java.util.logging.Level)
	 */
	@Override
	public synchronized void setLevel(Level newLevel) throws SecurityException
	{
		super.setLevel(newLevel);
		if (stdOutHandler != null)
		{
			stdOutHandler.setLevel(newLevel);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#setFilter(java.util.logging.Filter)
	 */
	@Override
	public synchronized void setFilter(Filter newFilter) throws SecurityException
	{
		super.setFilter(newFilter);
		if (stdOutHandler != null)
		{
			stdOutHandler.setFilter(newFilter);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.StreamHandler#setEncoding(java.lang.String)
	 */
	@Override
	public synchronized void setEncoding(String encoding) throws SecurityException, UnsupportedEncodingException
	{
		super.setEncoding(encoding);
		if (stdOutHandler != null)
		{
			stdOutHandler.setEncoding(encoding);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#setFormatter(java.util.logging.Formatter)
	 */
	@Override
	public synchronized void setFormatter(Formatter newFormatter) throws SecurityException
	{
		super.setFormatter(newFormatter);
		if (stdOutHandler != null)
		{
			stdOutHandler.setFormatter(newFormatter);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#setErrorManager(java.util.logging.ErrorManager)
	 */
	@Override
	public synchronized void setErrorManager(ErrorManager em)
	{
		super.setErrorManager(em);
		if (stdOutHandler != null)
		{
			stdOutHandler.setErrorManager(em);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.ConsoleHandler#publish(java.util.logging.LogRecord)
	 */
	@Override
	public void publish(LogRecord record)
	{
		if (record.getLevel().intValue() < Level.WARNING.intValue())
		{
			stdOutHandler.publish(record);
			stdOutHandler.flush();
		}
		else
		{
			super.publish(record);
		}
	}
}
