// Package : com.ben12.reta.beans.property.buffering
// File : ObservableSetBuffering.java
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
package com.ben12.reta.beans.property.buffering;

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

import com.ben12.reta.beans.property.validation.BeanPropertyValidation;

/**
 * Observable map buffering.
 * 
 * @param <K>
 *            map key type
 * @param <E>
 *            map value type
 * @author Beno�t Moreau (ben.12)
 */
public class ObservableMapBuffering<K, E> extends SimpleMapProperty<K, E>
		implements Buffering<ObservableMap<K, E>>, BeanPropertyValidation<ObservableMap<K, E>>
{
	/** Bean type (used for validation). */
	private final Class<?>					beanType;

	/** Bean property name (used for validation). */
	private final String					propertyName;

	/** Buffered map. */
	private final ObservableMap<K, E>		subject;

	/** Map is buffering. */
	private final BooleanProperty			buffering		= new SimpleBooleanProperty(false);

	/** Map validity. */
	private final BooleanProperty			validity		= new SimpleBooleanProperty(true);

	/** Map validity info. */
	private final StringProperty			infoValidity	= new SimpleStringProperty(null);

	/** Use equals method for check buffering. */
	private boolean							equalsBuffering	= true;

	/** Listener for buffered map. */
	private final MapChangeListener<K, E>	thisListener;

	/** Listener for subject map. */
	private final MapChangeListener<K, E>	subjectListener;

	/** Weak listener wrapper of subject listener. */
	private final MapChangeListener<K, E>	weakSubjectListener;

	/** Revert in progress. */
	private boolean							reverting		= false;

	/** Revert in progress. */
	private boolean							committing		= false;

	/**
	 * @param newSubject
	 *            subject map to buffer
	 */
	public ObservableMapBuffering(final ObservableMap<K, E> newSubject)
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
	public ObservableMapBuffering(final Class<?> newBeanType, final String newPropertyName,
			final ObservableMap<K, E> newSubject)
	{
		super(FXCollections.observableHashMap());

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

		putAll(subject);

		thisListener = c -> {
			// TODO: Workaround for https://bugs.openjdk.java.net/browse/JDK-8136465
			if (!reverting)
			{
				if (equalsBuffering)
				{
					buffering.setValue(!equalsSubject());
				}
				else
				{
					buffering.setValue(true);
				}
				validate();
			}
		};

		subjectListener = c -> {
			// TODO: Workaround for https://bugs.openjdk.java.net/browse/JDK-8136465
			if (!committing)
			{
				if (!buffering.getValue())
				{
					revert();
				}
			}
		};

		addListener(thisListener);

		weakSubjectListener = new WeakMapChangeListener<K, E>(subjectListener);
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
	public ObservableMap<K, E> getSubject()
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
		// TODO: Workaround for https://bugs.openjdk.java.net/browse/JDK-8136465
		// Use committing flag instead of removeListener + addListener
		committing = true;
		// subject.removeListener(weakSubjectListener);
		try
		{
			buffering.setValue(false);
			entrySet().stream().forEachOrdered(e -> subject.put(e.getKey(), e.getValue()));
			subject.entrySet().removeAll(subject.entrySet()
					.parallelStream()
					.filter(e -> !containsKey(e.getKey()))
					.collect(Collectors.toSet()));
		}
		finally
		{
			committing = false;
		}
		// subject.addListener(weakSubjectListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#revert()
	 */
	@Override
	public void revert()
	{
		// TODO: Workaround for https://bugs.openjdk.java.net/browse/JDK-8136465
		// Use reverting flag instead of removeListener + addListener
		reverting = true;
		// removeListener(thisListener);
		try
		{
			buffering.setValue(false);
			subject.entrySet().stream().forEachOrdered(e -> put(e.getKey(), e.getValue()));
			entrySet().removeAll(entrySet().parallelStream()
					.filter(e -> !subject.containsKey(e.getKey()))
					.collect(Collectors.toSet()));
		}
		finally
		{
			reverting = false;
		}
		// addListener(thisListener);
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
			equals = entrySet().parallelStream().allMatch(e -> subject.entrySet().contains(e));
		}

		return equals;
	}
}
