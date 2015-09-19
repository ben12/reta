// Package : com.ben12.reta.view.validation
// File : ValidationDecorator.java
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
package com.ben12.reta.view.validation;

import javafx.beans.DefaultProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;

import com.ben12.reta.beans.property.validation.PropertyValidation;

/**
 * {@link Node} validation decorator.
 * 
 * @param <N>
 *            Decorated node type
 * @author Benoît Moreau (ben.12)
 */
@DefaultProperty("child")
public class ValidationDecorator<N extends Node> extends Region implements PropertyValidation
{
	/** Default CSS style class. */
	private static final String			DEFAULT_STYLE_CLASS	= "validation-decorator";

	/** {@link #valid} property CSS pseudo class. */
	private static final PseudoClass	PSEUDO_CLASS_VALID	= PseudoClass.getPseudoClass("valid");

	/** Default error image icon. */
	private static final Image			ERROR_ICON			= new Image(
																	ValidationDecorator.class.getResourceAsStream("/com/ben12/reta/resources/images/error.png"));

	/** Default error node effect. */
	private static final DropShadow		ERROR_EFFECT		= new DropShadow(5, Color.RED);

	/** Error image icon. */
	private final ImageView				errorIcon;

	/** Decorated node. */
	private N							child;

	/** Error info tool-tip. */
	private final Tooltip				errorTooltip;

	/** Node validity property. */
	private final BooleanProperty		valid				= new BooleanPropertyBase(true)
															{
																@Override
																protected void invalidated()
																{
																	pseudoClassStateChanged(PSEUDO_CLASS_VALID, get());
																}

																@Override
																public Object getBean()
																{
																	return ValidationDecorator.this;
																}

																@Override
																public String getName()
																{
																	return "valid";
																}
															};

	/**
	 * Constructor.
	 */
	public ValidationDecorator()
	{
		getStyleClass().setAll(DEFAULT_STYLE_CLASS);
		errorIcon = new ImageView(ERROR_ICON);
		getChildren().add(errorIcon);
		errorIcon.visibleProperty().bind(valid.not());
		effectProperty().bind(Bindings.createObjectBinding(() -> (valid.get() ? null : ERROR_EFFECT), valid));
		pseudoClassStateChanged(PSEUDO_CLASS_VALID, valid.get());

		errorTooltip = new ErrorTooltip();
		errorTooltip.setFont(Font.font("monospace", errorTooltip.getFont().getSize()));
		Tooltip.install(errorIcon, errorTooltip);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.PropertyValidation#infoValidityProperty()
	 */
	@Override
	public StringProperty infoValidityProperty()
	{
		return errorTooltip.textProperty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ben12.reta.beans.property.validation.PropertyValidation#validityProperty()
	 */
	@Override
	public BooleanProperty validityProperty()
	{
		return valid;
	}

	/**
	 * @param newChild
	 *            the child to set
	 */
	public final void setChild(final N newChild)
	{
		if (child != null && getChildren().size() > 1)
		{
			getChildren().remove(0);
		}
		child = newChild;
		getChildren().add(0, child);
	}

	/**
	 * @return the child
	 */
	public final N getChild()
	{
		return child;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.Parent#layoutChildren()
	 */
	@Override
	protected void layoutChildren()
	{
		final Insets insets = getInsets();
		layoutInArea(errorIcon, insets.getLeft(), insets.getTop(), getWidth(), getHeight(), getBaselineOffset(),
				new Insets(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, -2, -2), false, false, HPos.LEFT, VPos.BOTTOM);
		if (child != null)
		{
			layoutInArea(child, insets.getLeft(), insets.getTop(), getWidth(), getHeight(), getBaselineOffset(), null,
					true, true, HPos.LEFT, VPos.CENTER);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computeMinHeight(double)
	 */
	@Override
	protected double computeMinHeight(final double width)
	{
		double height;
		if (child != null)
		{
			height = getInsets().getTop() + child.minHeight(width) + getInsets().getBottom();
		}
		else
		{
			height = super.computeMinHeight(width);
		}
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computeMinWidth(double)
	 */
	@Override
	protected double computeMinWidth(final double height)
	{
		double width;
		if (child != null)
		{
			width = getInsets().getLeft() + child.minWidth(height) + getInsets().getRight();
		}
		else
		{
			width = super.computeMinWidth(height);
		}
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computeMaxHeight(double)
	 */
	@Override
	protected double computeMaxHeight(final double width)
	{
		double height;
		if (child != null)
		{
			height = getInsets().getTop() + child.maxHeight(width) + getInsets().getBottom();
		}
		else
		{
			height = super.computeMaxHeight(width);
		}
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computeMaxWidth(double)
	 */
	@Override
	protected double computeMaxWidth(final double height)
	{
		double width;
		if (child != null)
		{
			width = getInsets().getLeft() + child.maxWidth(height) + getInsets().getRight();
		}
		else
		{
			width = super.computeMaxWidth(height);
		}
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computePrefHeight(double)
	 */
	@Override
	protected double computePrefHeight(final double width)
	{
		double height;
		if (child != null)
		{
			height = getInsets().getTop() + child.prefHeight(width) + getInsets().getBottom();
		}
		else
		{
			height = super.computePrefHeight(width);
		}
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.layout.Region#computePrefWidth(double)
	 */
	@Override
	protected double computePrefWidth(final double height)
	{
		double width;
		if (child != null)
		{
			width = getInsets().getLeft() + child.prefWidth(height) + getInsets().getRight();
		}
		else
		{
			width = super.computePrefWidth(height);
		}
		return width;
	}

	/**
	 * Error Tool-tip class.
	 * (Workaround against font bug https://javafx-jira.kenai.com/browse/RT-34948)
	 */
	private class ErrorTooltip extends Tooltip
	{
		/** Default font size. */
		private final double	fsize	= getFont().getSize();

		@Override
		protected void show()
		{
			setFont(Font.font("monospace", fsize));
			super.show();
		}

		@Override
		public void show(final Node ownerNode, final double anchorX, final double anchorY)
		{
			setFont(Font.font("monospace", fsize));
			super.show(ownerNode, anchorX, anchorY);
		}

		@Override
		public void show(final Window owner)
		{
			setFont(Font.font("monospace", fsize));
			super.show(owner);
		}

		@Override
		public void show(final Window ownerWindow, final double anchorX, final double anchorY)
		{
			setFont(Font.font("monospace", fsize));
			super.show(ownerWindow, anchorX, anchorY);
		}
	}
}
