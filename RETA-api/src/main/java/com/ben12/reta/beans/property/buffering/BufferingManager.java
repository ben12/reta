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
import java.util.ArrayList;
import java.util.List;
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
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import com.ben12.reta.beans.property.validation.PropertyValidation;
import com.google.common.collect.Iterables;

/**
 * @author Benoît Moreau (ben.12)
 */
public class BufferingManager
{
	private final List<WeakReference<Buffering<?>>>	buffers				= new ArrayList<>();

	private final BooleanProperty					buffering			= new SimpleBooleanProperty(false);

	private BooleanExpression						bufferingExpression	= new SimpleBooleanProperty(false);

	private final BooleanProperty					valid				= new SimpleBooleanProperty(true);

	private BooleanExpression						validExpression		= new SimpleBooleanProperty(true);

	private boolean									equalsBuffering		= true;

	public BooleanProperty bufferingProperty()
	{
		return buffering;
	}

	public boolean isBuffering()
	{
		return buffering.get();
	}

	public BooleanProperty validProperty()
	{
		return valid;
	}

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

	public SimpleObjectPropertyBuffering<String> bufferingString(final Object bean, final String propertyName)
			throws NoSuchMethodException
	{
		return buffering(JavaBeanStringPropertyBuilder.create().bean(bean).name(propertyName).build(), bean.getClass(),
				propertyName);
	}

	public SimpleObjectPropertyBuffering<Number> bufferingInteger(final Object bean, final String propertyName)
			throws NoSuchMethodException
	{
		return buffering(JavaBeanIntegerPropertyBuilder.create().bean(bean).name(propertyName).build(),
				bean.getClass(), propertyName);
	}

	public <T extends Object> SimpleObjectPropertyBuffering<T> bufferingObject(final Object bean,
			final String propertyName) throws NoSuchMethodException
	{
		// suppress unchecked conversion (do not use create(), and call bean() and name() separately)
		final JavaBeanObjectPropertyBuilder<T> builder = new JavaBeanObjectPropertyBuilder<>();
		builder.bean(bean).name(propertyName);
		final Property<T> property = builder.build();
		return buffering(property, bean.getClass(), propertyName);
	}

	public <T> PropertyBuffering<T> buffering(final Property<T> p)
	{
		return buffering(p, null, null);
	}

	public <T> SimpleObjectPropertyBuffering<T> buffering(final Property<T> p, final Class<?> beanType,
			final String propertyName)
	{
		final SimpleObjectPropertyBuffering<T> pb = new SimpleObjectPropertyBuffering<>(beanType, propertyName, p);
		add(pb);
		return pb;
	}

	/**
	 * @param b
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
	 *            buffered properties
	 */
	public void addAll(final List<Buffering<?>> properties)
	{
		properties.stream().forEach(p -> add(p));
	}

	/**
	 * @param properties
	 *            buffered properties
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

	public <T> boolean remove(final Buffering<T> p, final boolean rebuild)
	{
		final boolean changed = Iterables.removeIf(buffers, br -> {
			final Buffering<?> b = br.get();
			if (b != null)
			{
				return b == p;
			}
			else
			{
				return true;
			}
		});
		if (changed && rebuild)
		{
			rebuildBufferingExpression();
			rebuildValidExpression();
		}
		return changed;
	}

	public void purge()
	{
		final boolean changed = Iterables.removeIf(buffers, br -> (br.get() == null));
		if (changed)
		{
			rebuildBufferingExpression();
			rebuildValidExpression();
		}
	}

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
	 * @param newSources
	 * @return
	 */
	public <T> ObservableListBuffering<T> bufferingList(final Object bean, final String propertyName)
	{
		ObservableListBuffering<T> lb = null;
		try
		{
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
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
		return lb;
	}

	/**
	 * @param newSources
	 * @return
	 */
	public <T> ObservableListBuffering<T> buffering(final ObservableList<T> list)
	{
		final ObservableListBuffering<T> lb = new ObservableListBuffering<>(list);
		add(lb);
		return lb;
	}

	/**
	 * @param newSources
	 * @return
	 */
	public <T> ObservableSetBuffering<T> bufferingSet(final Object bean, final String propertyName)
	{
		ObservableSetBuffering<T> lb = null;
		try
		{
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
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
		return lb;
	}

	/**
	 * @param newSources
	 * @return
	 */
	public <T> ObservableSetBuffering<T> buffering(final ObservableSet<T> set)
	{
		final ObservableSetBuffering<T> sb = new ObservableSetBuffering<>(set);
		add(sb);
		return sb;
	}

	/**
	 * @param newSources
	 * @return
	 */
	public <K, E> ObservableMapBuffering<K, E> buffering(final ObservableMap<K, E> map)
	{
		final ObservableMapBuffering<K, E> sb = new ObservableMapBuffering<>(map);
		add(sb);
		return sb;
	}

	private <T> T getPropertyValue(final Object bean, final String propertyName, final Class<T> expectedClass)
			throws IntrospectionException, IllegalAccessException, InvocationTargetException
	{
		final BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
		final PropertyDescriptor descriptor = Stream.of(beanInfo.getPropertyDescriptors())
				.filter(p -> p.getName().equals(propertyName))
				.findFirst()
				.get();
		final Object value = descriptor.getReadMethod().invoke(bean);
		return expectedClass.cast(value);
	}
}
