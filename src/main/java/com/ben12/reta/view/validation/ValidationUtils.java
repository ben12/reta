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

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.ben12.reta.view.buffering.PropertyValidation;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ValidationUtils
{
	private static final DropShadow	ERROR_EFFECT	= new DropShadow(4, Color.RED);

	private static final Validator	VALIDATOR		= Validation.buildDefaultValidatorFactory().getValidator();

	public static void bindValidationLabel(Node editableComponent, Node validityView, PropertyValidation property)
	{
		bindValidationLabel(validityView, property);
		bindValidationEffect(editableComponent, property);
	}

	public static void bindValidationEffect(Node editableComponent, final PropertyValidation property)
	{
		editableComponent.effectProperty().bind(Bindings.createObjectBinding(new Callable<Effect>()
		{
			@Override
			public Effect call() throws Exception
			{
				return (property.validityProperty().get() ? null : ERROR_EFFECT);
			}
		}, property.validityProperty()));
	}

	public static void bindValidationLabel(Node validityView, PropertyValidation property)
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

	public static <T, C> PropertyValidation bindValidationLabel(Node editableComponent, Node validityView,
			Property<T> property, final Class<C> beanType, final String propertyName)
	{
		PropertyValidation propertyValidation = bindValidationLabel(validityView, property, beanType, propertyName);
		bindValidationEffect(editableComponent, propertyValidation);
		return propertyValidation;
	}

	public static <T, C> PropertyValidation bindValidationLabel(Node validityView, Property<T> property,
			final Class<C> beanType, final String propertyName)
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

		return propertyValidation;
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
