// Package : com.ben12.reta.api
// File : RETAParseException.java
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
package com.ben12.reta.api;

/**
 * @author Benoît Moreau (ben.12)
 */
public class RETAParseException extends Exception
{
	private static final long	serialVersionUID	= -8420537781672663994L;

	/**
	 * @param message
	 *            the detail message
	 */
	public RETAParseException(final String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 *            cause the cause
	 */
	public RETAParseException(final Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 *            the detail message
	 * @param cause
	 *            cause the cause
	 */
	public RETAParseException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
