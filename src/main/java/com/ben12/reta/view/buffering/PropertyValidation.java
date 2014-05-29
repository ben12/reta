// Package : com.ben12.reta.view.buffering
// File : PropertyValidation.java
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
package com.ben12.reta.view.buffering;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

import com.ben12.reta.view.validation.ValidationUtils;
import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public interface PropertyValidation
{
	BooleanProperty validityProperty();

	StringProperty infoValidityProperty();

	Object get();

	Class<?> getBeanType();

	String getPropertyName();

	/**
	 * Validate property value.
	 */
	default void validate()
	{
		if (getBeanType() != null && !Strings.isNullOrEmpty(getPropertyName()))
		{
			ValidationUtils.validate(getBeanType(), getPropertyName(), get(), this);
		}
		else
		{
			ValidationUtils.validate(get(), this);
		}
	}
}
