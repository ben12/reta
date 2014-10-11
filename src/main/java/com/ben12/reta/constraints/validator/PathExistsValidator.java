// Package : com.ben12.reta.constraints.validator
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
package com.ben12.reta.constraints.validator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.ben12.reta.constraints.PathExists;
import com.ben12.reta.constraints.PathExists.KindOfPath;
import com.ben12.reta.util.RETAAnalysis;

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
	public void initialize(PathExists parameters)
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
	public boolean isValid(CharSequence value, ConstraintValidatorContext context)
	{
		boolean valid = true;
		if (value != null)
		{
			Path path = null;
			try
			{
				path = Paths.get(value.toString());
			}
			catch (InvalidPathException e)
			{
				return false;
			}

			if (path != null && parent)
			{
				path = path.getParent();
			}

			if (path == null)
			{
				return true;
			}
			else if (!path.isAbsolute())
			{
				File root = RETAAnalysis.getInstance().getConfig();
				if (root != null)
				{
					path = root.getParentFile().getAbsoluteFile().toPath().resolve(path);
				}
				else
				{
					return true;
				}
			}

			if (!Files.exists(path))
			{
				valid = false;
			}
			else if (KindOfPath.DIRECTORY.equals(kindOfPath) && !Files.isDirectory(path))
			{
				valid = false;
			}
			else if (KindOfPath.FILE.equals(kindOfPath) && !Files.isRegularFile(path))
			{
				valid = false;
			}
		}
		return valid;
	}
}
