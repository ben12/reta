// Package : com.ben12.reta.beans.property.buffering
// File : Buffering.java
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
package com.ben12.reta.beans.property.buffering;

import javafx.beans.property.BooleanProperty;

/**
 * Buffering interface.
 * 
 * @param <T>
 *            buffered value type
 * @author Benoît Moreau (ben.12)
 */
public interface Buffering<T>
{
	/**
	 * @return buffered subject
	 */
	T getSubject();

	/**
	 * @return use equals method for check buffering
	 */
	boolean isEqualsBuffering();

	/**
	 * @param equalsBuffering
	 *            true for use equals method for check buffering, use == otherwise
	 */
	void setEqualsBuffering(boolean equalsBuffering);

	/**
	 * @return buffering property
	 */
	BooleanProperty bufferingProperty();

	/**
	 * @return value is buffering
	 */
	default boolean isBuffering()
	{
		return bufferingProperty().get();
	}

	/**
	 * Commits change.
	 */
	void commit();

	/**
	 * Reverts change.
	 */
	void revert();
}
