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

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableView;

/**
 * @author Benoît Moreau (ben.12)
 */
public class MapTableView<K, V> extends TableView<Map.Entry<K, V>>
{
	private final ObservableList<Map.Entry<K, V>>	obsList;

	private final MapChangeListener<K, V>			mapChange;

	private ObservableMap<K, V>						map;

	public MapTableView()
	{
		this(FXCollections.observableHashMap());
	}

	public MapTableView(ObservableMap<K, V> theMap)
	{
		map = theMap;
		obsList = FXCollections.observableArrayList(map.entrySet());
		setItems(obsList);

		mapChange = (MapChangeListener.Change<? extends K, ? extends V> change) -> {
			if (change.wasAdded() ^ change.wasRemoved())
			{
				if (change.wasAdded())
				{
					obsList.add(map.entrySet()
							.parallelStream()
							.filter(me -> me.getKey().equals(change.getKey()))
							.findFirst()
							.get());
				}
				if (change.wasRemoved())
				{
					obsList.removeIf(me -> me.getKey().equals(change.getKey()));
				}
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
			obsList.addAll(map.entrySet());

			map.addListener(mapChange);
		}
	}
}
