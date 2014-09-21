// Package : com.ben12.reta.view.control
// File : MapTableView.java
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
package com.ben12.reta.view.control;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableView;

/**
 * @author Benoît Moreau (ben.12)
 */
public class MapTableView<K, V> extends TableView<MapTableView<K, V>.Entry>
{
	private final ObservableList<Entry>		obsList;

	private final MapChangeListener<K, V>	mapChange;

	private ObservableMap<K, V>				map;

	public MapTableView()
	{
		this(FXCollections.observableHashMap());
	}

	public MapTableView(ObservableMap<K, V> theMap)
	{
		map = theMap;
		obsList = FXCollections.observableArrayList(map.entrySet()
				.stream()
				.map(me -> new Entry(me))
				.collect(Collectors.toList()));
		setItems(obsList);

		mapChange = (MapChangeListener.Change<? extends K, ? extends V> change) -> {
			if (!change.wasAdded() && change.wasRemoved())
			{
				obsList.removeIf(me -> me.getKey().equals(change.getKey()));
			}
			else if (change.wasAdded() && !change.wasRemoved())
			{
				AtomicInteger index = new AtomicInteger(-1);
				Map.Entry<K, V> entry = map.entrySet().stream().filter(me -> {
					index.getAndIncrement();
					return me.getKey().equals(change.getKey());
				}).findFirst().get();
				obsList.add(index.get(), new Entry(entry));
			}
			else
			{
				Platform.runLater(() -> getParent().requestLayout());
			}
		};

		map.addListener(mapChange);
	}

	/**
	 * @param map
	 *            the map to set
	 */
	public void setMapItems(ObservableMap<K, V> newMap)
	{
		if (map != newMap)
		{
			map.removeListener(mapChange);

			map = (newMap == null ? FXCollections.observableHashMap() : newMap);

			obsList.clear();
			obsList.addAll(map.entrySet().stream().map(me -> new Entry(me)).collect(Collectors.toList()));

			map.addListener(mapChange);
		}
	}

	public class Entry implements MapChangeListener<K, V>
	{
		private final Map.Entry<K, V>				entry;

		private final Property<V>					value	= new SimpleObjectProperty<>();

		private final ReadOnlyObjectPropertyBase<K>	key		= new ReadOnlyObjectPropertyBase<K>()
															{
																@Override
																public Object getBean()
																{
																	return Entry.this;
																}

																@Override
																public String getName()
																{
																	return "key";
																}

																@Override
																public K get()
																{
																	return entry.getKey();
																}
															};

		private final ChangeListener<V>				changeListener;

		/**
		 * 
		 */
		public Entry(Map.Entry<K, V> mapEntry)
		{
			entry = mapEntry;
			value.setValue(entry.getValue());
			changeListener = (observable, oldValue, newValue) -> {
				map.removeListener(Entry.this);
				entry.setValue(newValue);
				map.addListener(Entry.this);
			};
			value.addListener(changeListener);
			map.addListener(this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javafx.collections.MapChangeListener#onChanged(javafx.collections.MapChangeListener.Change)
		 */
		@Override
		public void onChanged(javafx.collections.MapChangeListener.Change<? extends K, ? extends V> change)
		{
			if (change.getKey().equals(entry.getKey()) && change.wasAdded())
			{
				value.removeListener(changeListener);
				value.setValue(change.getValueAdded());
				value.addListener(changeListener);
			}
		}

		public K getKey()
		{
			return entry.getKey();
		}

		public ReadOnlyProperty<K> keyProperty()
		{
			return key;
		}

		public Property<V> valueProperty()
		{
			return value;
		}
	}
}
