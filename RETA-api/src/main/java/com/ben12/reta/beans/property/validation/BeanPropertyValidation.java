// Package : com.ben12.reta.beans.property.validation
// File : BeanPropertyValidation.java
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
package com.ben12.reta.beans.property.validation;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.google.common.base.Strings;

/**
 * PropertyValidation extension using Bean validation.
 * 
 * @see javax.validation.Validation
 * @author Benoît Moreau (ben.12)
 */
public interface BeanPropertyValidation<T> extends PropertyValidation
{
	Validator	DEFAULT_VALIDATOR	= Validation.buildDefaultValidatorFactory().getValidator();

	T get();

	<C> Class<C> getBeanType();

	String getPropertyName();

	/**
	 * Validate property value.
	 */
	default void validate()
	{
		Set<ConstraintViolation<Object>> violations;

		if (getBeanType() != null && !Strings.isNullOrEmpty(getPropertyName()))
		{
			violations = DEFAULT_VALIDATOR.validateValue(getBeanType(), getPropertyName(), get());
		}
		else
		{
			violations = DEFAULT_VALIDATOR.validate(get());
		}

		if (violations.isEmpty())
		{
			validityProperty().set(true);
			infoValidityProperty().set("");
		}
		else
		{
			final String message = violations.stream()
					.map(ConstraintViolation::getMessage)
					.collect(Collectors.joining("\n"));

			validityProperty().set(false);
			infoValidityProperty().set(message);
		}
	}
}
