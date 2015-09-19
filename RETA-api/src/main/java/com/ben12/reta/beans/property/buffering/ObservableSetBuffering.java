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

import com.ben12.reta.beans.property.validation.BeanPropertyValidation;

/**
 * Observable set buffering.
 * 
 * @param <E>
 *            set elements type
 * @author Benoît Moreau (ben.12)
 */
public class ObservableSetBuffering<E> extends SimpleSetProperty<E> implements Buffering<ObservableSet<E>>,
		BeanPropertyValidation<ObservableSet<E>>
{
	/** Bean type (used for validation). */
	private final Class<?>				beanType;

	/** Bean property name (used for validation). */
	private final String				propertyName;

	/** Buffered set. */
	private final ObservableSet<E>		subject;

	/** Set is buffering. */
	private final BooleanProperty		buffering		= new SimpleBooleanProperty(false);

	/** Set validity. */
	private final BooleanProperty		validity		= new SimpleBooleanProperty(true);

	/** Set validity info. */
	private final StringProperty		infoValidity	= new SimpleStringProperty(null);

	/** Use equals method for check buffering. */
	private boolean						equalsBuffering	= true;

	/** Listener for buffered set. */
	private final SetChangeListener<E>	thisListener;

	/** Listener for subject set. */
	private final SetChangeListener<E>	subjectListener;

	/** Weak listener wrapper of subject listener. */
	private final SetChangeListener<E>	weakSubjectListener;

	/**
	 * @param newSubject
	 *            subject set to buffer
	 */
	public ObservableSetBuffering(final ObservableSet<E> newSubject)
	{
		this(null, null, newSubject);
	}

	/**
	 * @param newBeanType
	 *            bean type
	 * @param newPropertyName
	 *            bean property name
	 * @param newSubject
	 *            property value
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

		weakSubjectListener = new WeakSetChangeListener<E>(subjectListener);
		subject.addListener(weakSubjectListener);

		validate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.BeanPropertyValidation#getBeanType()
	 */
	@Override
	public Class<?> getBeanType()
	{
		return beanType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.BeanPropertyValidation#getPropertyName()
	 */
	@Override
	public String getPropertyName()
	{
		return propertyName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.PropertyValidation#validityProperty()
	 */
	@Override
	public BooleanProperty validityProperty()
	{
		return validity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.PropertyValidation#infoValidityProperty()
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
	 * @return true if buffered value equals subject value
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
