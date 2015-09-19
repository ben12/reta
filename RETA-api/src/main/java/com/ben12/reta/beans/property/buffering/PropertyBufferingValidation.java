// Package : com.ben12.reta.beans.property.buffering
// File : PropertyBufferingValidation.java
// 
// Copyright (C) 2014 Benoît
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

import com.ben12.reta.beans.property.validation.BeanPropertyValidation;

/**
 * Adds validation interface to {@link PropertyBuffering}.
 * 
 * @param <T>
 *            {@link PropertyBuffering} value type
 * @author Benoît
 */
public interface PropertyBufferingValidation<T> extends PropertyBuffering<T>, BeanPropertyValidation<T>
{
}
