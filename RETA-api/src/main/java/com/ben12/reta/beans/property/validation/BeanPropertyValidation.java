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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;

import com.google.common.base.Strings;

/**
 * PropertyValidation extension using Bean validation.
 * 
 * @param <T>
 *            value type to validate
 * @see javax.validation.Validation
 * @see javax.validation.Validator
 * @author Benoît Moreau (ben.12)
 */
public interface BeanPropertyValidation<T> extends PropertyValidation
{
	/** Default {@link Validator}. */
	Validator DEFAULT_VALIDATOR = Validation.byDefaultProvider()
			.configure()
			.messageInterpolator(new MessageInterpolator()
			{
				private final Map<String, ResourceBundleMessageInterpolator> messageInterpolators = new HashMap<>();

				private ResourceBundleMessageInterpolator getInterpolator(final Context context)
				{
					final Annotation annotation = context.getConstraintDescriptor().getAnnotation();
					String packageName = annotation.annotationType().getPackage().getName();
					packageName = packageName.replace('.', '/') + '/';

					ResourceBundleMessageInterpolator interpolator = messageInterpolators.get(packageName);
					if (interpolator == null)
					{
						final List<String> locations = new ArrayList<>();
						int pos = -1;
						do
						{
							locations.add(0, packageName.substring(0, pos + 1)
									+ AbstractMessageInterpolator.USER_VALIDATION_MESSAGES);
							pos = packageName.indexOf('/', pos + 1);
						}
						while (pos >= 0);

						interpolator = new ResourceBundleMessageInterpolator(
								new AggregateResourceBundleLocator(locations));
						messageInterpolators.put(packageName, interpolator);
					}
					return interpolator;
				}

				@Override
				public String interpolate(final String messageTemplate, final Context context, final Locale locale)
				{
					return getInterpolator(context).interpolate(messageTemplate, context, locale);
				}

				@Override
				public String interpolate(final String messageTemplate, final Context context)
				{
					return getInterpolator(context).interpolate(messageTemplate, context);
				}
			})
			.buildValidatorFactory()
			.getValidator();

	/**
	 * @return value to validate
	 */
	T get();

	/**
	 * @return bean type containing the value to validate
	 */
	Class<?> getBeanType();

	/**
	 * @return property name in the {@link #getBeanType()} to validate
	 */
	String getPropertyName();

	/**
	 * Validate property value.
	 */
	default void validate()
	{
		@SuppressWarnings("unchecked")
		final Class<Object> beanType = (Class<Object>) getBeanType();
		final String propertyName = getPropertyName();

		Set<ConstraintViolation<Object>> violations;
		if (beanType != null && !Strings.isNullOrEmpty(propertyName))
		{
			violations = DEFAULT_VALIDATOR.validateValue(beanType, propertyName, this);
		}
		else
		{
			violations = DEFAULT_VALIDATOR.validate(this);
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
