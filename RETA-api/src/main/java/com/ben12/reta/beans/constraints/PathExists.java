// Package : com.ben12.reta.beans.constraints
// File : PathExists.java
// 
// Copyright (C) 2014 Beno�t Moreau (ben.12)
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
package com.ben12.reta.beans.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.ben12.reta.beans.constraints.validator.PathExistsValidator;

/**
 * @author Beno�t Moreau (ben.12)
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = PathExistsValidator.class)
public @interface PathExists
{
	/** Error message to display. */
	String message() default "{com.ben12.reta.beans.constraints.PathExists.message}";

	/** Validating groups. */
	Class<?>[] groups() default {};

	/** Validate the path parent directory. */
	boolean parent() default false;

	/** Check if path is of this kind. */
	KindOfPath kind() default KindOfPath.FILE_OR_DIRECTORY;

	/** Custom payload. */
	Class<? extends Payload>[] payload() default {};

	/**
	 * Kinds of path.
	 */
	enum KindOfPath
	{
		/** File path. */
		FILE,
		/** Directory path. */
		DIRECTORY,
		/** File or directory path. */
		FILE_OR_DIRECTORY;
	}
}
