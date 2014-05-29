// Package : com.ben12.reta.view.validation
// File : ValidationUtils.java
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
package com.ben12.reta.view.validation;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.ben12.reta.view.buffering.PropertyValidation;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ValidationUtils
{
	private static final byte[]		ERROR_GIF_BYTES	= { 71, 73, 70, 56, 57, 97, 7, 0, 8, 0, -94, 0, 0, -1, 0, 0, -65,
			63, 63, 127, 63, 63, -1, 127, 127, -65, 95, 95, -33, -65, -65, -1, -1, -1, -1, -1, -1, 33, -7, 4, 1, 0, 0,
			7, 0, 44, 0, 0, 0, 0, 7, 0, 8, 0, 0, 3, 25, 88, -79, -85, 54, 96, -104, 37, -55, 19, 1, 88, 2, -80, 54, 28,
			86, 93, -63, 19, 77, -118, -96, 6, 69, 2, 0, 59, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private static final Validator	VALIDATOR		= Validation.buildDefaultValidatorFactory().getValidator();

	public static final Image		ERROR_IMAGE;

	static
	{
		Image image = null;
		try
		{
			ImageIcon imageIcon = new ImageIcon(ERROR_GIF_BYTES);
			BufferedImage bufferedImage = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics g = bufferedImage.createGraphics();
			imageIcon.paintIcon(null, g, 0, 0);
			g.dispose();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "png", out);
			out.flush();
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			image = new Image(in);
		}
		catch (IOException e)
		{
			Logger.getLogger(ValidationUtils.class.getName()).log(Level.SEVERE, "", e);
		}
		ERROR_IMAGE = image;
	}

	public static <T, C> void bindValidationLabel(Node validityView, PropertyValidation property)
	{
		validityView.visibleProperty().bind(Bindings.not(property.validityProperty()));

		// Workaround against font bug https://javafx-jira.kenai.com/browse/RT-34948
		Tooltip infoTooltip = new Tooltip()
		{
			final double	fsize	= getFont().getSize();

			@Override
			protected void show()
			{
				setFont(Font.font("monospace", fsize));
				super.show();
			}

			@Override
			public void show(Node ownerNode, double anchorX, double anchorY)
			{
				setFont(Font.font("monospace", fsize));
				super.show(ownerNode, anchorX, anchorY);
			}

			@Override
			public void show(Window owner)
			{
				setFont(Font.font("monospace", fsize));
				super.show(owner);
			}

			@Override
			public void show(Window ownerWindow, double anchorX, double anchorY)
			{
				setFont(Font.font("monospace", fsize));
				super.show(ownerWindow, anchorX, anchorY);
			}
		};

		infoTooltip.textProperty().bind(property.infoValidityProperty());
		infoTooltip.setFont(Font.font("monospace", infoTooltip.getFont().getSize()));
		Tooltip.install(validityView, infoTooltip);
	}

	public static <T, C> void bindValidationLabel(Node validityView, Property<T> property, final Class<C> beanType,
			final String propertyName)
	{
		final PropertyValidation propertyValidation = new PropertyValidation()
		{
			final BooleanProperty	validityProperty	= new SimpleBooleanProperty(true);

			final StringProperty	infoProperty		= new SimpleStringProperty(null);

			@Override
			public BooleanProperty validityProperty()
			{
				return validityProperty;
			}

			@Override
			public StringProperty infoValidityProperty()
			{
				return infoProperty;
			}

			@Override
			public Class<?> getBeanType()
			{
				return beanType;
			}

			@Override
			public String getPropertyName()
			{
				return propertyName;
			}

			@Override
			public Object get()
			{
				return property.getValue();
			}
		};

		property.addListener((observable, oldValue, value) -> propertyValidation.validate());

		propertyValidation.validate();

		bindValidationLabel(validityView, propertyValidation);
	}

	/**
	 * @param beanType
	 * @param propertyName
	 * @param value
	 * @param propertyValidation
	 */
	public static <T, C> void validate(Class<C> beanType, String propertyName, T value,
			PropertyValidation propertyValidation)
	{
		Set<ConstraintViolation<C>> violations = VALIDATOR.validateValue(beanType, propertyName, value);
		if (violations.isEmpty())
		{
			propertyValidation.validityProperty().set(true);
			propertyValidation.infoValidityProperty().set("");
		}
		else
		{
			String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));

			propertyValidation.validityProperty().set(false);
			propertyValidation.infoValidityProperty().set(message);
		}
	}

	public static <C> void validate(C bean, PropertyValidation propertyValidation)
	{
		Set<ConstraintViolation<C>> violations = VALIDATOR.validate(bean);
		if (violations.isEmpty())
		{
			propertyValidation.validityProperty().set(true);
			propertyValidation.infoValidityProperty().set("");
		}
		else
		{
			String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
			propertyValidation.validityProperty().set(false);
			propertyValidation.infoValidityProperty().set(message);
		}
	}
}
