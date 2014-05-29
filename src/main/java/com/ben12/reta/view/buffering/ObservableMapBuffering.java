// Package : com.ben12.reta.view.buffering
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
package com.ben12.reta.view.buffering;

import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.WeakMapChangeListener;

/**
 * @author Benoît Moreau (ben.12)
 */
public class ObservableMapBuffering<K, E> extends SimpleMapProperty<K, E> implements Buffering<ObservableMap<K, E>>,
		PropertyValidation
{
	private final Class<?>					beanType;

	private final String					propertyName;

	private final ObservableMap<K, E>		subject;

	private final BooleanProperty			buffering		= new SimpleBooleanProperty(false);

	private final BooleanProperty			validity		= new SimpleBooleanProperty(true);

	private final StringProperty			infoValidity	= new SimpleStringProperty(null);

	private boolean							equalsBuffering	= true;

	private final MapChangeListener<K, E>	thisListener;

	private final MapChangeListener<K, E>	subjectListener;

	private final MapChangeListener<K, E>	weakSubjectListener;

	public ObservableMapBuffering(ObservableMap<K, E> newSubject)
	{
		this(null, null, newSubject);
	}

	/**
	 * 
	 */
	public ObservableMapBuffering(Class<?> newBeanType, String newPropertyName, ObservableMap<K, E> newSubject)
	{
		super(FXCollections.observableHashMap());

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

		subject = Objects.requireNonNull(newSubject);

		putAll(subject);

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
		subject.addListener(weakSubjectListener = new WeakMapChangeListener<K, E>(subjectListener));

		validate();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.Buffering#getSubject()
	 */
	@Override
	public ObservableMap<K, E> getSubject()
	{
		return subject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.Buffering#isEqualsBuffering()
	 */
	@Override
	public boolean isEqualsBuffering()
	{
		return equalsBuffering;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.Buffering#setEqualsBuffering(boolean)
	 */
	@Override
	public void setEqualsBuffering(boolean newEqualsBuffering)
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
	 * @see com.ben12.reta.view.buffering.Buffering#bufferingProperty()
	 */
	@Override
	public BooleanProperty bufferingProperty()
	{
		return buffering;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.Buffering#commit()
	 */
	@Override
	public void commit()
	{
		subject.removeListener(weakSubjectListener);
		buffering.setValue(false);
		entrySet().stream().forEachOrdered(e -> subject.put(e.getKey(), e.getValue()));
		subject.entrySet().removeAll(
				subject.entrySet().parallelStream().filter(e -> !containsKey(e.getKey())).collect(Collectors.toSet()));
		subject.addListener(weakSubjectListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.view.buffering.Buffering#revert()
	 */
	@Override
	public void revert()
	{
		removeListener(thisListener);
		buffering.setValue(false);
		subject.entrySet().stream().forEachOrdered(e -> put(e.getKey(), e.getValue()));
		entrySet().removeAll(
				entrySet().parallelStream().filter(e -> !subject.containsKey(e.getKey())).collect(Collectors.toSet()));
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
			equals = entrySet().parallelStream().allMatch(e -> subject.entrySet().contains(e));
		}

		return equals;
	}
}
