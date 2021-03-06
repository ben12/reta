// Package : com.ben12.reta.beans.property.buffering
// File : ObservableListBuffering.java
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
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;

import com.ben12.reta.beans.property.validation.BeanPropertyValidation;

/**
 * Observable list buffering.
 * 
 * @param <E>
 *            list elements type
 * @author Beno�t Moreau (ben.12)
 */
public class ObservableListBuffering<E> extends SimpleListProperty<E> implements Buffering<ObservableList<E>>,
		BeanPropertyValidation<ObservableList<E>>
{
	/** Bean type (used for validation). */
	private final Class<?>				beanType;

	/** Bean property name (used for validation). */
	private final String				propertyName;

	/** Buffered list. */
	private final ObservableList<E>		subject;

	/** List is buffering. */
	private final BooleanProperty		buffering		= new SimpleBooleanProperty(false);

	/** List validity. */
	private final BooleanProperty		validity		= new SimpleBooleanProperty(true);

	/** List validity info. */
	private final StringProperty		infoValidity	= new SimpleStringProperty(null);

	/** Use equals method for check buffering. */
	private boolean						equalsBuffering	= true;

	/** Listener for buffered list. */
	private final ListChangeListener<E>	thisListener;

	/** Listener for subject list. */
	private final ListChangeListener<E>	subjectListener;

	/** Weak listener wrapper of subject listener. */
	private final ListChangeListener<E>	weakSubjectListener;

	/**
	 * @param newSubject
	 *            subject list to buffer
	 */
	public ObservableListBuffering(final ObservableList<E> newSubject)
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
	public ObservableListBuffering(final Class<?> newBeanType, final String newPropertyName,
			final ObservableList<E> newSubject)
	{
		super(FXCollections.observableArrayList());

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

		weakSubjectListener = new WeakListChangeListener<E>(subjectListener);
		subject.addListener(weakSubjectListener);

		validate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.buffering.Buffering#getSubject()
	 */
	@Override
	public ObservableList<E> getSubject()
	{
		return subject;
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

		final AtomicInteger index = new AtomicInteger();
		stream().forEachOrdered(b -> {
			final int i = index.getAndIncrement();
			if (i < subject.size())
			{
				final E e = subject.get(i);
				if (e != b)
				{
					subject.set(i, b);
				}
			}
			else
			{
				subject.add(b);
			}
		});

		if (size() < subject.size())
		{
			subject.remove(size(), subject.size());
		}

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

		final AtomicInteger index = new AtomicInteger();
		subject.stream().forEachOrdered(e -> {
			final int i = index.getAndIncrement();
			if (i < size())
			{
				final E b = get(i);
				if (e != b)
				{
					set(i, e);
				}
			}
			else
			{
				add(e);
			}
		});

		if (subject.size() < size())
		{
			remove(subject.size(), size());
		}

		addListener(thisListener);
		validate();
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
	 * @see com.ben12.reta.beans.property.buffering.PropertyValidation#infoValidityProperty()
	 */
	@Override
	public StringProperty infoValidityProperty()
	{
		return infoValidity;
	}

	/**
	 * @return true if buffered value equals subject value
	 */
	private boolean equalsSubject()
	{
		boolean equals = size() == subject.size();
		for (int i = 0; i < size() && equals; i++)
		{
			equals = get(i) == subject.get(i);
		}
		return equals;
	}
}
