// Package : com.ben12.reta.plugin.tika.beans.constraints.validator
// File : RegexValidator.java
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
package com.ben12.reta.plugin.tika.beans.constraints.validator;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.ben12.reta.plugin.tika.beans.constraints.Regex;
import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public class RegexValidator implements ConstraintValidator<Regex, CharSequence>
{
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(final Regex parameters)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
	 */
	@Override
	public boolean isValid(final CharSequence value, final ConstraintValidatorContext context)
	{
		boolean valid = true;

		if (value != null && value.length() > 0)
		{
			try
			{
				Pattern.compile(value.toString());
			}
			catch (final IllegalArgumentException e)
			{
				if (!Strings.isNullOrEmpty(e.getLocalizedMessage()))
				{
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate(e.getLocalizedMessage()).addConstraintViolation();
				}
				valid = false;
			}
		}

		return valid;
	}
}
