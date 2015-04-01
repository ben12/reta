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

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * @author Benoît Moreau (ben.12)
 */
public final class MessageDialog extends Stage
{
	/** Return value if YES is chosen. */
	public static final int	YES_OPTION		= 0;

	/** Return value if OK is chosen. */
	public static final int	OK_OPTION		= 0;

	/** Return value if NO is chosen. */
	public static final int	NO_OPTION		= 1;

	/** Return value if CANCEL is chosen. */
	public static final int	CANCEL_OPTION	= 2;

	/** Default message dialog parent. */
	private static Window	defaultParent	= null;

	/**
	 * Selected option.
	 * 
	 * @see #YES_OPTION
	 * @see #NO_OPTION
	 * @see #CANCEL_OPTION
	 * @see #OK_OPTION
	 */
	private int				option			= CANCEL_OPTION;

	/**
	 * Selected response.
	 */
	private Object			result			= null;

	/**
	 * @param parent
	 *            dialog parent
	 */
	public MessageDialog(final Window parent)
	{
		super();
		initOwner(parent == null ? defaultParent : parent);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.UTILITY);
		setResizable(false);
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
	 * Show message dialog and wait.
	 */
	public void showAndWaitDialog()
	{
		sizeToScene();
		Platform.runLater(() -> centerOnOwner());
		showAndWait();
	}

	/**
	 * Show the message dialog.
	 */
	public void showDialog()
	{
		sizeToScene();
		show();
		centerOnOwner();
	}

	/**
	 * Center the message dialog on the parent window.
	 */
	private void centerOnOwner()
	{
		final Window parent = getOwner();
		if (parent != null)
		{
			final double px = parent.getX();
			final double py = parent.getY();
			final double pwidth = parent.getScene().getWidth();
			final double pheight = parent.getScene().getHeight();

			final double width = getScene().getWidth();
			final double height = getScene().getHeight();

			final double x = px + (pwidth / 2.0) - (width / 2.0);
			final double y = py + (pheight / 2.0) - (height / 2.0);

			setX(x);
			setY(y);
		}
	}

	/**
	 * @param iconType
	 *            icon type
	 * @return the FX icon to use (built from swing icon returned by {@link UIManager#getIcon(java.lang.Object)}).
	 */
	private static Image getIcon(final Object iconType)
	{
		Image icon = null;
		final Icon swingIcon = UIManager.getIcon(iconType);
		if (swingIcon != null)
		{
			final BufferedImage bimg = new BufferedImage(swingIcon.getIconWidth(), swingIcon.getIconHeight(),
					BufferedImage.TYPE_INT_RGB);
			swingIcon.paintIcon(null, bimg.getGraphics(), 0, 0);
			icon = SwingFXUtils.toFXImage(bimg, new WritableImage(swingIcon.getIconWidth(), swingIcon.getIconHeight()));
		}
		return icon;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Error message to show
	 */
	public static void showErrorMessage(final Window parent, final String message)
	{
		showMessage(parent, "Error", message, null, getIcon("OptionPane.errorIcon"), BUTTON.OK);
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Warning message to show
	 */
	public static void showWarningMessage(final Window parent, final String message)
	{
		showMessage(parent, "Warning", message, null, getIcon("OptionPane.warningIcon"), BUTTON.OK);
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Confirmation message to show
	 * @return option selected ({@link #YES_OPTION} or {@link #NO_OPTION})
	 */
	public static int showConfirmationMessage(final Window parent, final String message)
	{
		final MessageDialog dialog = showMessage(parent, "Confirmation", message, null,
				getIcon("OptionPane.questionIcon"), BUTTON.YES_NO);
		return dialog.option;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Question message to show
	 * @return option selected ({@link #OK_OPTION} or {@link #CANCEL_OPTION})
	 */
	public static int showQuestionMessage(final Window parent, final String message)
	{
		final MessageDialog dialog = showMessage(parent, "Question", message, null, getIcon("OptionPane.questionIcon"),
				BUTTON.OK_CANCEL);
		return dialog.option;
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param message
	 *            Question message to show
	 * @param options
	 *            possible choices
	 * @return selected choice or null if canceled
	 */
	public static Object showOptionsMessage(final Window parent, final String message, final Object[] options)
	{
		final MessageDialog dialog = showMessage(parent, "Choice", message, options,
				getIcon("OptionPane.questionIcon"), BUTTON.OK_CANCEL);
		return dialog.result;
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
			return;
		}

		final MessageDialog dialog = new MessageDialog(parent);
		dialog.setTitle(title);

		final VBox pane = new VBox(5);
		pane.setPadding(new Insets(5));
		pane.setMinWidth(500);

		final Label messagePane = new Label(message.get());
		messagePane.setWrapText(true);
		messagePane.setMaxHeight(Integer.MAX_VALUE);
		messagePane.setMaxWidth(500);
		message.addListener((final ObservableValue<? extends String> observable, final String oldValue,
				final String newValue) -> Platform.runLater(() -> {
			messagePane.setText(newValue);
			dialog.sizeToScene();
		}));
		VBox.setVgrow(messagePane, Priority.ALWAYS);

		final HBox buttonPane = new HBox(5);
		buttonPane.setAlignment(Pos.CENTER_RIGHT);

		final Button ok = new Button("Ok");
		ok.setOnAction((final ActionEvent e) -> {
			dialog.close();
		});
		progress.addListener((final ObservableValue<? extends Number> observable, final Number oldValue,
				final Number newValue) -> Platform.runLater(() -> ok.setDisable(newValue.doubleValue() < 1.0)));
		buttonPane.getChildren().add(ok);

		final ProgressBar progressBar = new ProgressBar(progress.get());
		progress.addListener((final ObservableValue<? extends Number> observable, final Number oldValue,
				final Number newValue) -> Platform.runLater(() -> progressBar.setProgress(newValue.doubleValue())));
		progressBar.setMaxWidth(Integer.MAX_VALUE);

		pane.getChildren().addAll(messagePane, progressBar, buttonPane);
		dialog.setScene(new Scene(pane));
		dialog.setOnCloseRequest((final WindowEvent e) -> {
			if (progress.get() < 1.0)
			{
				e.consume();
			}
		});
		dialog.showDialog();
	}

	/**
	 * @param parent
	 *            dialog parent, if null default parent is used
	 * @param title
	 *            dialog title
	 * @param message
	 *            dialog message
	 * @param options
	 *            option values
	 * @param icon
	 *            dialog icon
	 * @param buttons
	 *            dialog buttons
	 * @return {@link MessageDialog} shown
	 */
	public static MessageDialog showMessage(final Window parent, final String title, final String message,
			final Object[] options, final Image icon, final BUTTON buttons)
	{
		final MessageDialog dialog;
		if (!Platform.isFxApplicationThread())
		{
			final FutureTask<MessageDialog> result = new FutureTask<MessageDialog>(() -> showMessage(parent, title,
					message, options, icon, buttons));
			Platform.runLater(result);
			MessageDialog fdialog;
			try
			{
				fdialog = result.get();
			}
			catch (InterruptedException | ExecutionException e)
			{
				fdialog = null;
			}
			dialog = fdialog;
		}
		else
		{
			dialog = new MessageDialog(parent);
			dialog.setTitle(title);

			final VBox pane = new VBox(5);
			pane.setPadding(new Insets(5));
			pane.setMinWidth(200);

			final Label messagePane = new Label();
			if (icon != null)
			{
				messagePane.setGraphic(new ImageView(icon));
				messagePane.setGraphicTextGap(10);
			}
			messagePane.setText(message);
			messagePane.setWrapText(true);
			messagePane.setMaxHeight(Integer.MAX_VALUE);
			messagePane.setMaxWidth(Integer.MAX_VALUE);
			VBox.setVgrow(messagePane, Priority.ALWAYS);
			pane.getChildren().addAll(messagePane);

			final ComboBox<Object> optionsField;
			if (options != null)
			{
				optionsField = new ComboBox<Object>();
				optionsField.getItems().addAll(options);
				VBox.setVgrow(optionsField, Priority.ALWAYS);
				pane.getChildren().addAll(optionsField);
			}
			else
			{
				optionsField = null;
			}

			final HBox buttonPane = createButtonPane(dialog, optionsField, buttons);
			pane.getChildren().addAll(buttonPane);

			dialog.setScene(new Scene(pane));
			dialog.showAndWaitDialog();
		}

		return dialog;
	}

	/**
	 * @param dialog
	 *            {@link MessageDialog} instance
	 * @param optionsField
	 *            options field
	 * @param buttons
	 *            buttons to show
	 * @return button pane
	 */
	private static HBox createButtonPane(final MessageDialog dialog, final ComboBox<Object> optionsField,
			final BUTTON buttons)
	{
		final HBox buttonPane = new HBox(5);
		buttonPane.setAlignment(Pos.CENTER_RIGHT);
		if (buttons.hasYes())
		{
			final Button yes = new Button("Yes");
			yes.setOnAction(e -> {
				dialog.option = YES_OPTION;
				if (optionsField != null)
				{
					dialog.result = optionsField.getValue();
				}
				dialog.close();
			});
			buttonPane.getChildren().add(yes);
		}
		if (buttons.hasNo())
		{
			final Button no = new Button("No");
			no.setOnAction(e -> {
				dialog.option = NO_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(no);
		}
		if (buttons.hasOk())
		{
			final Button ok = new Button("Ok");
			ok.setOnAction(e -> {
				dialog.option = OK_OPTION;
				if (optionsField != null)
				{
					dialog.result = optionsField.getValue();
				}
				dialog.close();
			});
			buttonPane.getChildren().add(ok);
		}
		if (buttons.hasCancel())
		{
			final Button cancel = new Button("Cancel");
			cancel.setOnAction(e -> {
				dialog.option = CANCEL_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(cancel);
		}
		return buttonPane;
	}

	/**
	 * @author Benoît Moreau (ben.12)
	 */
	public enum BUTTON
	{
		/** Ok button. */
		OK(BUTTON.OK_MASK),
		/** Ok/Cancel buttons. */
		OK_CANCEL(BUTTON.OK_MASK | BUTTON.CANCEL_MASK),
		/** Yes/No buttons. */
		YES_NO(BUTTON.YES_MASK | BUTTON.NO_MASK),
		/** Ok/No/Cancel buttons. */
		YES_NO_CANCEL(BUTTON.YES_MASK | BUTTON.NO_MASK | BUTTON.CANCEL_MASK);

		/** Ok button mask. */
		private static final int	OK_MASK		= 0b0001;

		/** Cancel button mask. */
		private static final int	CANCEL_MASK	= 0b0010;

		/** Yes button mask. */
		private static final int	YES_MASK	= 0b0100;

		/** No button mask. */
		private static final int	NO_MASK		= 0b1000;

		/** Current buttons mask. */
		private final int			mask;

		/**
		 * @param theMask
		 *            buttons mask
		 */
		private BUTTON(final int theMask)
		{
			mask = theMask;
		}

		/**
		 * @return Ok button mask selected
		 */
		public boolean hasOk()
		{
			return (mask & BUTTON.OK_MASK) != 0;
		}

		/**
		 * @return Cancel button mask selected
		 */
		public boolean hasCancel()
		{
			return (mask & BUTTON.CANCEL_MASK) != 0;
		}

		/**
		 * @return Yes button mask selected
		 */
		public boolean hasYes()
		{
			return (mask & BUTTON.YES_MASK) != 0;
		}

		/**
		 * @return No button mask selected
		 */
		public boolean hasNo()
		{
			return (mask & BUTTON.NO_MASK) != 0;
		}
	}
}
