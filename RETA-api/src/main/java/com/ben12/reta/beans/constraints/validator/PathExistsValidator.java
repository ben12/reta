// Package : com.ben12.reta.beans.constraints.validator
// File : PathValidator.java
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
package com.ben12.reta.beans.constraints.validator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.ben12.reta.beans.constraints.PathExists;
import com.ben12.reta.beans.constraints.PathExists.KindOfPath;

/**
 * @author Benoît Moreau (ben.12)
 */
public class PathExistsValidator implements ConstraintValidator<PathExists, CharSequence>
{
	private KindOfPath	kindOfPath	= KindOfPath.FILE_OR_DIRECTORY;

	private boolean		parent		= false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(final PathExists parameters)
	{
		kindOfPath = parameters.kind();
		parent = parameters.parent();
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
		if (value != null)
		{
			Path path = null;
			try
			{
				path = Paths.get(value.toString());

				if (path != null && parent)
				{
					path = path.getParent();
				}

				if (path == null)
				{
					valid = true;
				}
				else
				{
					if (!path.isAbsolute())
					{
						final File root = new File(System.getProperty("user.dir"));
						path = root.toPath().resolve(path);
					}

					switch (kindOfPath)
					{
					case DIRECTORY:
						valid = Files.isDirectory(path);
						break;

					case FILE:
						valid = Files.isRegularFile(path);
						break;

					default:
						valid = Files.exists(path);
						break;
					}
				}
			}
			catch (final InvalidPathException e)
			{
				valid = false;
			}
		}
		return valid;
	}
}
