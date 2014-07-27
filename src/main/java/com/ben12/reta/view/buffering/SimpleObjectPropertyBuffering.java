// Package : com.ben12.reta.view.buffering
// File : SimpleObjectPropertyBuffering.java
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

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

/**
 * @author Benoît Moreau (ben.12)
 */
public class SimpleObjectPropertyBuffering<T> extends SimpleObjectProperty<T> implements PropertyBufferingValidation<T>
{
	private final Class<?>			beanType;

	private final String			propertyName;

	private final Property<T>		subject;

	private final ChangeListener<T>	subjectListener;

	private final ChangeListener<T>	thisListener;

	private final ChangeListener<T>	weakSubjectListener;

	private final BooleanProperty	buffering		= new SimpleBooleanProperty(false);

	private final BooleanProperty	validity		= new SimpleBooleanProperty(true);

	private final StringProperty	infoValidity	= new SimpleStringProperty("");

	private boolean					equalsBuffering	= true;

	/**
	 * 
	 */
	public SimpleObjectPropertyBuffering(Property<T> subject)
	{
		this(null, null, subject);
	}

	/**
	 * 
	 */
	public SimpleObjectPropertyBuffering(Class<?> newBeanType, String newPropertyName, Property<T> newSubject)
	{
		if (newBeanType != null || !(newSubject instanceof ReadOnlyProperty<?>))
		{
			beanType = newBeanType;
		}
		else
		{
			Object bean = ((ReadOnlyProperty<?>) newSubject).getBean();
			beanType = (bean != null ? bean.getClass() : null);
		}

		if (newPropertyName != null || !(newSubject instanceof ReadOnlyProperty<?>))
		{
			propertyName = newPropertyName;
		}
		else
		{
			propertyName = ((ReadOnlyProperty<?>) newSubject).getName();
		}

		subject = java.util.Objects.requireNonNull(newSubject);

		setValue(subject.getValue());

		thisListener = (observable, oldValue, value) -> {
			if (equalsBuffering)
			{
				buffering.setValue(!Objects.equals(getValue(), subject.getValue()));
			}
			else
			{
				buffering.setValue(true);
			}
			validate();
		};

		subjectListener = (observable, oldValue, value) -> {
			if (!buffering.getValue())
			{
				revert();
			}
		};

		addListener(thisListener);
		this.subject.addListener(weakSubjectListener = new WeakChangeListener<T>(subjectListener));

		validate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyBuffering#getSubject()
	 */
	@Override
	public Property<T> getSubject()
	{
		return subject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyValidation#getBeanType()
	 */
	@Override
	public Class<?> getBeanType()
	{
		return beanType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyValidation#getPropertyName()
	 */
	@Override
	public String getPropertyName()
	{
		return propertyName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyBuffering#bufferingProperty()
	 */
	@Override
	public BooleanProperty bufferingProperty()
	{
		return buffering;
	}

	/**
	 * @return the equalsBuffering
	 */
	@Override
	public boolean isEqualsBuffering()
	{
		return equalsBuffering;
	}

	/**
	 * @param newEqualsBuffering
	 *            the equalsBuffering to set
	 */
	@Override
	public void setEqualsBuffering(boolean newEqualsBuffering)
	{
		equalsBuffering = newEqualsBuffering;
		if (isBuffering() && equalsBuffering)
		{
			buffering.setValue(!Objects.equals(getValue(), subject.getValue()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyBuffering#commit()
	 */
	@Override
	public void commit()
	{
		subject.removeListener(weakSubjectListener);
		buffering.setValue(false);
		subject.setValue(getValue());
		subject.addListener(weakSubjectListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyBuffering#revert()
	 */
	@Override
	public void revert()
	{
		removeListener(thisListener);
		buffering.setValue(false);
		setValue(subject.getValue());
		addListener(thisListener);
		validate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyValidation#validityProperty()
	 */
	@Override
	public BooleanProperty validityProperty()
	{
		return validity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.PropertyValidation#infoValidityProperty()
	 */
	@Override
	public StringProperty infoValidityProperty()
	{
		return infoValidity;
	}
}
