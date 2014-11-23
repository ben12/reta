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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * @author Benoît Moreau (ben.12)
 */
public class MessageDialog extends Stage
{
	/** Return value if YES is chosen. */
	public static final int	YES_OPTION		= 0;

	/** Return value if NO is chosen. */
	public static final int	NO_OPTION		= 1;

	/** Return value if CANCEL is chosen. */
	public static final int	CANCEL_OPTION	= 2;

	/** Return value if OK is chosen. */
	public static final int	OK_OPTION		= 0;

	private int				option			= CANCEL_OPTION;

	/**
	 * 
	 */
	private MessageDialog(Window parent)
	{
		super();
		initOwner(parent);
		initModality(Modality.APPLICATION_MODAL);
		initStyle(StageStyle.UTILITY);
		setResizable(false);
	}

	private void showAndWaitDialog()
	{
		sizeToScene();
		showAndWait();
	}

	private void showDialog()
	{
		sizeToScene();
		show();
	}

	public static void showErrorMessage(Window parent, String message)
	{
		showMessage(parent, "Error", message, null, BUTTON.OK);
	}

	public static int showConfirmationMessage(Window parent, String message)
	{
		return showMessage(parent, "Error", message, null, BUTTON.YES_NO);
	}

	public static int showQuestionMessage(Window parent, String message)
	{
		return showMessage(parent, "Error", message, null, BUTTON.OK_CANCEL);
	}

	public static void showProgressBar(Window parent, String title, StringProperty message, DoubleProperty progress)
	{
		if (!Platform.isFxApplicationThread())
		{
			Platform.runLater(() -> {
				showProgressBar(parent, title, message, progress);
			});
			return;
		}

		MessageDialog dialog = new MessageDialog(parent);
		dialog.setTitle(title);

		VBox pane = new VBox(5);
		pane.setPadding(new Insets(5));
		pane.setMinWidth(500);

		Label messagePane = new Label(message.get());
		messagePane.setWrapText(true);
		messagePane.setMaxHeight(Integer.MAX_VALUE);
		messagePane.setMaxWidth(500);
		message.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> Platform.runLater(() -> {
			messagePane.setText(newValue);
			dialog.sizeToScene();
		}));
		VBox.setVgrow(messagePane, Priority.ALWAYS);

		HBox buttonPane = new HBox(5);
		buttonPane.setAlignment(Pos.CENTER_RIGHT);

		Button ok = new Button("Ok");
		ok.setOnAction((ActionEvent e) -> {
			dialog.close();
		});
		progress.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> Platform.runLater(() -> ok.setDisable(newValue.doubleValue() < 1.0)));
		buttonPane.getChildren().add(ok);

		ProgressBar progressBar = new ProgressBar(progress.get());
		progress.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> Platform.runLater(() -> progressBar.setProgress(newValue.doubleValue())));
		progressBar.setMaxWidth(Integer.MAX_VALUE);

		pane.getChildren().addAll(messagePane, progressBar, buttonPane);
		dialog.setScene(new Scene(pane));
		dialog.setOnCloseRequest((WindowEvent e) -> {
			if (progress.get() < 1.0)
			{
				e.consume();
			}
		});
		dialog.showDialog();
	}

	public static int showMessage(Window parent, String title, String message, Image icon, BUTTON buttons)
	{
		if (!Platform.isFxApplicationThread())
		{
			FutureTask<Integer> result = new FutureTask<Integer>(() -> {
				return Integer.valueOf(showMessage(parent, title, message, icon, buttons));
			});
			Platform.runLater(result);
			try
			{
				return result.get();
			}
			catch (InterruptedException | ExecutionException e)
			{
				return CANCEL_OPTION;
			}
		}

		MessageDialog dialog = new MessageDialog(parent);
		dialog.setTitle(title);

		VBox pane = new VBox(5);
		pane.setPadding(new Insets(5));
		pane.setMinWidth(200);

		Label messagePane = new Label();
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

		HBox buttonPane = new HBox(5);
		buttonPane.setAlignment(Pos.CENTER_RIGHT);
		if (buttons.hasYes())
		{
			Button yes = new Button("Yes");
			yes.setOnAction((ActionEvent e) -> {
				dialog.option = YES_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(yes);
		}
		if (buttons.hasNo())
		{
			Button no = new Button("No");
			no.setOnAction((ActionEvent e) -> {
				dialog.option = NO_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(no);
		}
		if (buttons.hasOk())
		{
			Button ok = new Button("Ok");
			ok.setOnAction((ActionEvent e) -> {
				dialog.option = OK_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(ok);
		}
		if (buttons.hasCancel())
		{
			Button cancel = new Button("Cancel");
			cancel.setOnAction((ActionEvent e) -> {
				dialog.option = CANCEL_OPTION;
				dialog.close();
			});
			buttonPane.getChildren().add(cancel);
		}

		pane.getChildren().addAll(messagePane, buttonPane);
		dialog.setScene(new Scene(pane));
		dialog.showAndWaitDialog();

		return dialog.option;
	}

	public enum BUTTON
	{
		OK(BUTTON.OK_MASK), OK_CANCEL(BUTTON.OK_MASK | BUTTON.CANCEL_MASK), YES_NO(BUTTON.YES_MASK | BUTTON.NO_MASK), YES_NO_CANCEL(
				BUTTON.YES_MASK | BUTTON.NO_MASK | BUTTON.CANCEL_MASK);

		private static final int	OK_MASK		= 0b0001;

		private static final int	CANCEL_MASK	= 0b0010;

		private static final int	YES_MASK	= 0b0100;

		private static final int	NO_MASK		= 0b1000;

		private final int			mask;

		/**
		 * 
		 */
		private BUTTON(int mask)
		{
			this.mask = mask;
		}

		public boolean hasOk()
		{
			return (mask & BUTTON.OK_MASK) != 0;
		}

		public boolean hasCancel()
		{
			return (mask & BUTTON.CANCEL_MASK) != 0;
		}

		public boolean hasYes()
		{
			return (mask & BUTTON.YES_MASK) != 0;
		}

		public boolean hasNo()
		{
			return (mask & BUTTON.NO_MASK) != 0;
		}
	}
}
