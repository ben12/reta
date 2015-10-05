// Package : com.ben12.reta.beans.property.buffering
// File : BufferingManager.java
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import com.google.common.collect.Iterables;

import com.ben12.reta.beans.property.validation.PropertyValidation;

/**
 * Buffering manager.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class BufferingManager
{
	/** List of buffered value. */
	private final List<WeakReference<Buffering<?>>>	buffers				= new ArrayList<>();

	/** Has buffering values. */
	private final BooleanProperty					buffering			= new SimpleBooleanProperty(false);

	/** Buffering expression for all buffered values. */
	private BooleanExpression						bufferingExpression	= new SimpleBooleanProperty(false);

	/** Buffered values validity. */
	private final BooleanProperty					valid				= new SimpleBooleanProperty(true);

	/** Validity expression for all buffered values. */
	private BooleanExpression						validExpression		= new SimpleBooleanProperty(true);

	/** Use equals method for check buffering. */
	private boolean									equalsBuffering		= true;

	/**
	 * @return buffering property
	 */
	public BooleanProperty bufferingProperty()
	{
		return buffering;
	}

	/**
	 * @return has buffering values
	 */
	public boolean isBuffering()
	{
		return buffering.get();
	}

	/**
	 * @return valid property
	 */
	public BooleanProperty validProperty()
	{
		return valid;
	}

	/**
	 * @return validity of buffered values
	 */
	public boolean isValid()
	{
		return valid.get();
	}

	/**
	 * @param newEqualsBuffering
	 *            the equalsBuffering to set
	 */
	public void setEqualsBuffering(final boolean newEqualsBuffering)
	{
		final boolean old = equalsBuffering;
		equalsBuffering = newEqualsBuffering;
		if (old != equalsBuffering)
		{
			boolean purge = false;
			for (final WeakReference<Buffering<?>> br : buffers)
			{
				final Buffering<?> b = br.get();
				if (b != null)
				{
					b.setEqualsBuffering(equalsBuffering);
				}
				else
				{
					purge = true;
				}
			}
			if (purge)
			{
				purge();
			}
		}
	}

	/**
	 * @return the equalsBuffering
	 */
	public boolean isEqualsBuffering()
	{
		return equalsBuffering;
	}

	/**
	 * Commits changes.
	 */
	public void commit()
	{
		boolean purge = false;
		for (final WeakReference<Buffering<?>> br : buffers)
		{
			final Buffering<?> b = br.get();
			if (b != null)
			{
				b.commit();
			}
			else
			{
				purge = true;
			}
		}
		if (purge)
		{
			purge();
		}
	}

	/**
	 * Revert changes.
	 */
	public void revert()
	{
		boolean purge = false;
		for (final WeakReference<Buffering<?>> br : buffers)
		{
			final Buffering<?> b = br.get();
			if (b != null)
			{
				b.revert();
			}
			else
			{
				purge = true;
			}
		}
		if (purge)
		{
			purge();
		}
	}

	/**
	 * @param p
	 *            property to buffer
	 * @return {@link PropertyBuffering} for the property
	 * @param <T>
	 *            buffered value type
	 */
	public <T> SimpleObjectPropertyBuffering<T> buffering(final Property<T> p)
	{
		return buffering(p, null, null);
	}

	/**
	 * @param p
	 *            property to buffer
	 * @param beanType
	 *            bean type
	 * @param propertyName
	 *            bean property name
	 * @return {@link SimpleObjectPropertyBuffering} for the property
	 * @param <T>
	 *            buffered value type
	 */
	public <T> SimpleObjectPropertyBuffering<T> buffering(final Property<T> p, final Class<?> beanType,
			final String propertyName)
	{
		final SimpleObjectPropertyBuffering<T> pb = new SimpleObjectPropertyBuffering<>(beanType, propertyName, p);
		add(pb);
		return pb;
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            bean property name (property value class must inherit of {@link Property})
	 * @return {@link SimpleObjectPropertyBuffering} for the property
	 * @param <T>
	 *            buffered value type
	 * @throws NoSuchMethodException
	 *             if cannot found getter of the property
	 * @throws InvocationTargetException
	 *             if the underlying getter method throws an exception.
	 * @throws IllegalAccessException
	 *             if the underlying getter method is inaccessible.
	 */
	public <T> SimpleObjectPropertyBuffering<T> buffering(final Object bean, final String propertyName)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		final Property<T> p = getPropertyValue(bean, propertyName);
		final SimpleObjectPropertyBuffering<T> pb = new SimpleObjectPropertyBuffering<>(bean.getClass(), propertyName,
				p);
		add(pb);
		return pb;
	}

	/**
	 * @param b
	 *            {@link Buffering} instance to add in this manager
	 */
	public void add(final Buffering<?> b)
	{
		b.setEqualsBuffering(equalsBuffering);
		buffers.add(new WeakReference<Buffering<?>>(b));

		buffering.unbind();
		bufferingExpression = Bindings.or(b.bufferingProperty(), bufferingExpression);
		buffering.bind(bufferingExpression);

		if (b instanceof PropertyValidation)
		{
			valid.unbind();
			validExpression = Bindings.and(((PropertyValidation) b).validityProperty(), validExpression);
			valid.bind(validExpression);
		}
	}

	/**
	 * @param properties
	 *            {@link Buffering} instances to add in this manager
	 */
	public void addAll(final List<Buffering<?>> properties)
	{
		properties.stream().forEach(p -> add(p));
	}

	/**
	 * @param properties
	 *            {@link Buffering} instances to remove of this manager
	 */
	public void removeAll(final List<Buffering<?>> properties)
	{
		final AtomicInteger changed = new AtomicInteger(0);
		properties.stream().forEach(p -> changed.addAndGet(remove(p, false) ? 1 : 0));
		if (changed.get() > 0)
		{
			rebuildBufferingExpression();
			rebuildValidExpression();
		}
	}

	/**
	 * @param p
	 *            {@link Buffering} instance to remove of this manager
	 * @param rebuild
	 *            rebuild global buffering and validity properties
	 * @return true if removed, false if not found in buffering list
	 */
	private boolean remove(final Buffering<?> p, final boolean rebuild)
	{
		final boolean changed = Iterables.removeIf(buffers, br -> br.get() == p);
		if (changed && rebuild)
		{
			rebuildBufferingExpression();
			rebuildValidExpression();
		}
		return changed;
	}

	/**
	 * Purge empty weak references in {@link #buffers}.
	 */
	public void purge()
	{
		final boolean changed = Iterables.removeIf(buffers, br -> (br.get() == null));
		if (changed)
		{
			rebuildBufferingExpression();
			rebuildValidExpression();
		}
	}

	/**
	 * Rebuild {@link #bufferingExpression}.
	 */
	private void rebuildBufferingExpression()
	{
		buffering.unbind();
		bufferingExpression = new SimpleBooleanProperty(false);
		boolean purge = false;
		for (final WeakReference<Buffering<?>> br : buffers)
		{
			final Buffering<?> b = br.get();
			if (b != null)
			{
				bufferingExpression = Bindings.or(b.bufferingProperty(), bufferingExpression);
			}
			else
			{
				purge = true;
				break;
			}
		}
		buffering.bind(bufferingExpression);
		if (purge)
		{
			purge();
		}
	}

	/**
	 * Rebuild {@link #validExpression}.
	 */
	private void rebuildValidExpression()
	{
		valid.unbind();
		validExpression = new SimpleBooleanProperty(true);
		boolean purge = false;
		for (final WeakReference<Buffering<?>> br : buffers)
		{
			final Buffering<?> b = br.get();
			if (b != null)
			{
				if (b instanceof PropertyValidation)
				{
					validExpression = Bindings.and(((PropertyValidation) b).validityProperty(), validExpression);
				}
			}
			else
			{
				purge = true;
				break;
			}
		}
		valid.bind(validExpression);
		if (purge)
		{
			purge();
		}
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            bean list property name
	 * @return {@link ObservableListBuffering} for the property
	 * @param <T>
	 *            list element type
	 */
	public <T> ObservableListBuffering<T> bufferingList(final Object bean, final String propertyName)
	{
		ObservableListBuffering<T> lb = null;
		@SuppressWarnings("unchecked")
		final List<T> value = getPropertyValue(bean, propertyName, List.class);
		if (value instanceof ObservableList<?>)
		{
			lb = new ObservableListBuffering<>(bean.getClass(), propertyName, (ObservableList<T>) value);
		}
		else
		{
			lb = new ObservableListBuffering<>(bean.getClass(), propertyName, FXCollections.observableList(value));
		}
		add(lb);
		return lb;
	}

	/**
	 * @param list
	 *            observable list to buffer
	 * @return {@link ObservableListBuffering} for the list
	 * @param <T>
	 *            list element type
	 */
	public <T> ObservableListBuffering<T> buffering(final ObservableList<T> list)
	{
		final ObservableListBuffering<T> lb = new ObservableListBuffering<>(list);
		add(lb);
		return lb;
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            bean set property name
	 * @return {@link ObservableSetBuffering} for the property
	 * @param <T>
	 *            set element type
	 */
	public <T> ObservableSetBuffering<T> bufferingSet(final Object bean, final String propertyName)
	{
		ObservableSetBuffering<T> lb = null;
		@SuppressWarnings("unchecked")
		final Set<T> value = getPropertyValue(bean, propertyName, Set.class);
		if (value instanceof ObservableSet<?>)
		{
			lb = new ObservableSetBuffering<>(bean.getClass(), propertyName, (ObservableSet<T>) value);
		}
		else
		{
			lb = new ObservableSetBuffering<>(bean.getClass(), propertyName, FXCollections.observableSet(value));
		}
		add(lb);
		return lb;
	}

	/**
	 * @param set
	 *            observable set to buffer
	 * @return {@link ObservableSetBuffering} for the set
	 * @param <T>
	 *            set element type
	 */
	public <T> ObservableSetBuffering<T> buffering(final ObservableSet<T> set)
	{
		final ObservableSetBuffering<T> sb = new ObservableSetBuffering<>(set);
		add(sb);
		return sb;
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            bean set property name
	 * @return {@link ObservableMapBuffering} for the property
	 * @param <K>
	 *            map key type
	 * @param <E>
	 *            map value type
	 */
	public <K, E> ObservableMapBuffering<K, E> bufferingMap(final Object bean, final String propertyName)
	{
		ObservableMapBuffering<K, E> lb = null;
		@SuppressWarnings("unchecked")
		final Map<K, E> value = getPropertyValue(bean, propertyName, Map.class);
		if (value instanceof ObservableMap<?, ?>)
		{
			lb = new ObservableMapBuffering<>(bean.getClass(), propertyName, (ObservableMap<K, E>) value);
		}
		else
		{
			lb = new ObservableMapBuffering<>(bean.getClass(), propertyName, FXCollections.observableMap(value));
		}
		add(lb);
		return lb;
	}

	/**
	 * @param map
	 *            observable map to buffer
	 * @return {@link ObservableMapBuffering} for the map
	 * @param <K>
	 *            map key type
	 * @param <E>
	 *            map value type
	 */
	public <K, E> ObservableMapBuffering<K, E> buffering(final ObservableMap<K, E> map)
	{
		final ObservableMapBuffering<K, E> sb = new ObservableMapBuffering<>(map);
		add(sb);
		return sb;
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            property name
	 * @param expectedClass
	 *            property value {@link Class}
	 * @return bean property value
	 * @param <T>
	 *            bean property value type
	 */
	private <T> T getPropertyValue(final Object bean, final String propertyName, final Class<T> expectedClass)
	{
		Object value = null;
		try
		{
			final BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			final PropertyDescriptor descriptor = Stream.of(beanInfo.getPropertyDescriptors())
					.filter(p -> p.getName().equals(propertyName))
					.findFirst()
					.get();
			value = descriptor.getReadMethod().invoke(bean);
		}
		catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| IntrospectionException e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
		return expectedClass.cast(value);
	}

	/**
	 * @param bean
	 *            bean instance
	 * @param propertyName
	 *            property name
	 * @return bean property value
	 * @param <T>
	 *            bean property value type
	 * @throws NoSuchMethodException
	 *             if cannot found getter of the property
	 * @throws InvocationTargetException
	 *             if the underlying getter method throws an exception.
	 * @throws IllegalAccessException
	 *             if the underlying getter method is inaccessible.
	 */
	@SuppressWarnings("unchecked")
	private <T> Property<T> getPropertyValue(final Object bean, final String propertyName)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		final String propertyGetterName = propertyName + "Property";
		final Method m = bean.getClass().getMethod(propertyGetterName);
		final Object value = m.invoke(bean);
		return Property.class.cast(value);
	}
}
