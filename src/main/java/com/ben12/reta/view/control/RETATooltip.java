// Package : com.ben12.reta.view.control
// File : RETATooltip.java
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

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;

import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public class RETATooltip extends Tooltip
{
	public RETATooltip()
	{
		this("");
	}

	public RETATooltip(String text)
	{
		super(text);

		textProperty().addListener((observable, oldValue, newValue) -> {
			if (Strings.isNullOrEmpty(newValue))
			{
				hide();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.stage.PopupWindow#show(javafx.scene.Node, double, double)
	 */
	@Override
	public void show(Node ownerNode, double anchorX, double anchorY)
	{
		if (!Strings.isNullOrEmpty(getText()))
		{
			super.show(ownerNode, anchorX, anchorY);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.stage.PopupWindow#show(javafx.stage.Window)
	 */
	@Override
	public void show(Window owner)
	{
		if (!Strings.isNullOrEmpty(getText()))
		{
			super.show(owner);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.stage.PopupWindow#show(javafx.stage.Window, double, double)
	 */
	@Override
	public void show(Window ownerWindow, double anchorX, double anchorY)
	{
		if (!Strings.isNullOrEmpty(getText()))
		{
			super.show(ownerWindow, anchorX, anchorY);
		}
	}
}
