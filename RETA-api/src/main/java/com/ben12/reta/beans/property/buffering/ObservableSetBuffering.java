// Package : com.ben12.reta.beans.property.buffering
// File : ObservableSetBuffering.java
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
package com.ben12.reta.beans.property.buffering;

import java.util.Objects;
import java.util.stream.Collectors;

import com.ben12.reta.beans.property.validation.BeanPropertyValidation;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakSetChangeListener;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ObservableSetBuffering<E> extends SimpleSetProperty<E> implements Buffering<ObservableSet<E>>,
		BeanPropertyValidation<ObservableSet<E>>
{
	private final Class<?>				beanType;

	private final String				propertyName;

	private final ObservableSet<E>		subject;

	private final BooleanProperty		buffering		= new SimpleBooleanProperty(false);

	private final BooleanProperty		validity		= new SimpleBooleanProperty(true);

	private final StringProperty		infoValidity	= new SimpleStringProperty(null);

	private boolean						equalsBuffering	= true;

	private final SetChangeListener<E>	thisListener;

	private final SetChangeListener<E>	subjectListener;

	private final SetChangeListener<E>	weakSubjectListener;

	public ObservableSetBuffering(final ObservableSet<E> newSubject)
	{
		this(null, null, newSubject);
	}

	/**
	 * 
	 */
	public ObservableSetBuffering(final Class<?> newBeanType, final String newPropertyName,
			final ObservableSet<E> newSubject)
	{
		super(FXCollections.observableSet());

		if (newBeanType != null || !(newSubject instanceof ReadOnlyProperty<?>))
		{
			beanType = newBeanType;
		}
		else
		{
			final Object bean = ((ReadOnlyProperty<?>) newSubject).getBean();
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

		subject = Objects.requireNonNull(newSubject);

		addAll(subject);

		thisListener = c -> {
			if (equalsBuffering)
			{
				buffering.setValue(!equalsSubject());
			}
			else
			{
				buffering.setValue(true);
			}
			validate();
		};

		subjectListener = c -> {
			if (!buffering.getValue())
			{
				revert();
			}
		};

		addListener(thisListener);
		subject.addListener(weakSubjectListener = new WeakSetChangeListener<E>(subjectListener));

		validate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.PropertyValidation#getBeanType()
	 */
	@Override
	public Class<?> getBeanType()
	{
		return beanType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.PropertyValidation#getPropertyName()
	 */
	@Override
	public String getPropertyName()
	{
		return propertyName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.PropertyValidation#validityProperty()
	 */
	@Override
	public BooleanProperty validityProperty()
	{
		return validity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.PropertyValidation#infoValidityProperty()
	 */
	@Override
	public StringProperty infoValidityProperty()
	{
		return infoValidity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#getSubject()
	 */
	@Override
	public ObservableSet<E> getSubject()
	{
		return subject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#isEqualsBuffering()
	 */
	@Override
	public boolean isEqualsBuffering()
	{
		return equalsBuffering;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#setEqualsBuffering(boolean)
	 */
	@Override
	public void setEqualsBuffering(final boolean newEqualsBuffering)
	{
		equalsBuffering = newEqualsBuffering;
		if (isBuffering() && equalsBuffering)
		{
			buffering.setValue(!equalsSubject());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#bufferingProperty()
	 */
	@Override
	public BooleanProperty bufferingProperty()
	{
		return buffering;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#commit()
	 */
	@Override
	public void commit()
	{
		subject.removeListener(weakSubjectListener);
		buffering.setValue(false);
		stream().forEachOrdered(b -> subject.add(b));
		subject.removeAll(subject.parallelStream().filter(e -> !contains(e)).collect(Collectors.toSet()));
		subject.addListener(weakSubjectListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#revert()
	 */
	@Override
	public void revert()
	{
		removeListener(thisListener);
		buffering.setValue(false);
		subject.stream().forEachOrdered(e -> add(e));
		removeAll(parallelStream().filter(b -> !subject.contains(b)).collect(Collectors.toSet()));
		addListener(thisListener);
		validate();
	}

	/**
	 * @param newSubject
	 * @return
	 */
	private boolean equalsSubject()
	{
		boolean equals = size() == subject.size();

		if (equals)
		{
			equals = parallelStream().allMatch(e -> subject.contains(e));
		}

		return equals;
	}
}
