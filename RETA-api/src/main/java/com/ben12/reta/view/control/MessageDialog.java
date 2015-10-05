// Package : com.ben12.reta.view.control
// File : MessageDialog.java
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Pair;

/**
 * @author Benoît Moreau (ben.12)
 */
public final class MessageDialog
{
	/** Default message dialog parent. */
	private static Window defaultParent = null;

	/**
	 * 
	 */
	private MessageDialog()
	{
	}

	/**
	 * @param newDefaultParent
	 *            the defaultParent to set
	 */
	public static void setDefaultParent(final Window newDefaultParent)
	{
		defaultParent = newDefaultParent;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Error message to show
	 */
	public static void showErrorMessage(final Window parent, final String message)
	{
		showErrorMessage(parent, message, null);
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Error message to show
	 * @param t
	 *            {@link Throwable} shown as details
	 */
	public static void showErrorMessage(final Window parent, final String message, final Throwable t)
	{
		final Alert dialog = new Alert(AlertType.ERROR, message);
		initDialog(parent, dialog);

		if (t != null)
		{
			final StringWriter writter = new StringWriter();
			final PrintWriter print = new PrintWriter(writter);
			t.printStackTrace(print);

			final Label details = new Label(writter.toString());
			final ScrollPane detailsPane = new ScrollPane(details);
			detailsPane.setPrefHeight(Screen.getPrimary().getVisualBounds().getHeight() / 2.0);

			dialog.getDialogPane().setExpandableContent(detailsPane);
		}

		dialog.showAndWait();
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Warning message to show
	 */
	public static void showWarningMessage(final Window parent, final String message)
	{
		final Alert dialog = new Alert(AlertType.WARNING, message);
		initDialog(parent, dialog);
		dialog.showAndWait();
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Confirmation message to show
	 * @return option selected ({@link #YES_OPTION} or {@link #NO_OPTION})
	 */
	public static boolean showConfirmationMessage(final Window parent, final String message)
	{
		final Alert dialog = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
		initDialog(parent, dialog);
		dialog.showAndWait();

		return dialog.getResult() == ButtonType.YES;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Question message to show
	 * @return option selected ({@link #OK_OPTION} or {@link #CANCEL_OPTION})
	 */
	public static boolean showQuestionMessage(final Window parent, final String message)
	{
		final Alert dialog = new Alert(AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
		initDialog(parent, dialog);
		dialog.showAndWait();

		return dialog.getResult() == ButtonType.OK;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Question message to show
	 * @param choices
	 *            possible choices
	 * @return selected choice or null if canceled
	 * @param <T>
	 *            choice type
	 */
	@SafeVarargs
	public static <T> T showOptionsMessage(final Window parent, final String message, final T... choices)
	{
		final List<Pair<T, String>> pairChoices = Arrays.stream(choices)
				.map((c) -> new Pair<T, String>(c, null))
				.collect(Collectors.toList());
		return showOptionsMessage(parent, message, pairChoices);
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Question message to show
	 * @param choices
	 *            possible choices where {@link Pair} value is the choice label displayed
	 * @return selected choice or null if canceled
	 * @param <T>
	 *            choice type
	 */
	public static <T> T showOptionsMessage(final Window parent, final String message,
			final Collection<Pair<T, String>> choices)
	{
		final List<ChoiceWrapper<T>> choiceWrapper = choices.stream()
				.map((c) -> new ChoiceWrapper<T>(c.getKey(), c.getValue()))
				.collect(Collectors.toList());

		final ChoiceDialog<ChoiceWrapper<T>> dialog = new ChoiceDialog<ChoiceWrapper<T>>(choiceWrapper.get(0),
				choiceWrapper);
		initDialog(parent, dialog);
		dialog.setContentText(message);
		dialog.showAndWait();

		final ChoiceWrapper<T> result = dialog.getResult();

		return (result != null ? result.getValue() : null);
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param title
	 *            dialog title
	 * @param message
	 *            dialog message property
	 * @param progress
	 *            progress bar value
	 */
	public static void showProgressBar(final Window parent, final String title, final StringProperty message,
			final DoubleProperty progress)
	{
		if (!Platform.isFxApplicationThread())
		{
			Platform.runLater(() -> {
				showProgressBar(parent, title, message, progress);
			});
			Thread.yield();
			return;
		}

		final Dialog<Void> dialog = new Dialog<>();
		initDialog(parent, dialog);
		dialog.setTitle(title);

		final VBox pane = new VBox(5);
		pane.setPadding(new Insets(5));
		pane.setPrefWidth(500);

		// Info message
		final Label messagePane = new Label(message.get());
		messagePane.setWrapText(true);
		messagePane.setMaxHeight(Integer.MAX_VALUE);
		messagePane.setMaxWidth(500);
		message.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
			messagePane.setText(newValue);
		}));
		VBox.setVgrow(messagePane, Priority.ALWAYS);

		// Progress bar
		final ProgressBar progressBar = new ProgressBar(progress.get());
		progress.addListener((final ObservableValue<? extends Number> observable, final Number oldValue,
				final Number newValue) -> Platform.runLater(() -> progressBar.setProgress(newValue.doubleValue())));
		progressBar.setMaxWidth(Integer.MAX_VALUE);

		// OK button
		dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
		progress.addListener((observable, oldValue, newValue) -> Platform.runLater(
				() -> dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(newValue.doubleValue() < 1.0)));
		dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(progress.get() < 1.0);

		pane.getChildren().addAll(messagePane, progressBar);
		dialog.getDialogPane().setContent(pane);

		// Close only for ended progression
		final EventHandler<Event> onCloseRequest = (e) -> {
			if (progress.get() < 1.0)
			{
				e.consume();
			}
		};
		dialog.setOnCloseRequest((e) -> onCloseRequest.handle(e));
		dialog.getDialogPane().getScene().getWindow().setOnCloseRequest((e) -> onCloseRequest.handle(e));

		dialog.show();
	}

	/**
	 * @param parent
	 *            parent {@link Window}, {@link #defaultParent} is use if <code>null</code>
	 * @param dialog
	 *            {@link Dialog} to initialize
	 */
	private static void initDialog(final Window parent, final Dialog<?> dialog)
	{
		dialog.initOwner(parent == null ? defaultParent : parent);
		dialog.setHeaderText(null);
	}

	/**
	 * {@link ChoiceDialog} choice wrapper.
	 * 
	 * @author Benoît Moreau (ben.12)
	 * @param <T>
	 *            choice type
	 */
	private static final class ChoiceWrapper<T>
	{
		/** Choice value. */
		private final T			value;

		/** Choice label. */
		private final String	label;

		/**
		 * @param newValue
		 *            choice value
		 * @param newLabel
		 *            choice label, maybe null
		 */
		public ChoiceWrapper(final T newValue, final String newLabel)
		{
			value = newValue;
			label = (newLabel == null ? String.valueOf(value) : newLabel);
		}

		/**
		 * @return the value
		 */
		public T getValue()
		{
			return value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return label;
		}
	}
}
