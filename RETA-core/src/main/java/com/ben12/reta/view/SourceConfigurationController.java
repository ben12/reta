// Package : com.ben12.reta.view
// File : SourceConfigurationController.java
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
package com.ben12.reta.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import com.google.common.base.Splitter;

import com.ben12.reta.api.RETAParseException;
import com.ben12.reta.beans.property.buffering.Buffering;
import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.control.MessageDialog;
import com.ben12.reta.view.validation.ValidationDecorator;

/**
 * Requirement source configuration controller.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class SourceConfigurationController
{
	/** the {@link BufferingManager} instance. */
	private BufferingManager						bufferingManager;

	/** Keeps a reference on buffered properties. */
	private final List<Buffering<?>>				bufferedProperties	= new ArrayList<>();

	/** Input requirement source. */
	private InputRequirementSource					requirementSource	= null;

	/** Input requirement source list. */
	private ObservableList<InputRequirementSource>	sources				= null;

	/** Input requirement source buffered name list. */
	private ObservableList<ObjectProperty<String>>	sourcesName			= null;

	/** Error translation {@link ResourceBundle}. */
	private final ResourceBundle					errors				= ResourceBundle
			.getBundle("com/ben12/reta/constraints/" + ResourceBundleMessageInterpolator.USER_VALIDATION_MESSAGES);

	/** The input requirement source {@link TitledPane}. */
	@FXML
	private TitledPane								titledPane;

	/** Input requirement source name field. */
	@FXML
	private ValidationDecorator<TextField>			name;

	/** Input requirement source covered field. */
	@FXML
	private ValidationDecorator<TextField>			covers;

	/** Plug-in configuration pane. */
	@FXML
	private HBox									pluginPane;

	/** Use limit in preview check-box. */
	@FXML
	private CheckBox								useLimit;

	/** Preview limit field. */
	@FXML
	private Spinner<Integer>						previewLimit;

	/** Preview button. */
	@FXML
	private Button									preview;

	/**
	 * Bind simple {@link TextInputControl}.
	 * 
	 * @param input
	 *            {@link ValidationDecorator} of {@link TextInputControl}
	 * @param property
	 *            {@link SimpleObjectPropertyBuffering} to bind to the <code>input</code>
	 * @param <N>
	 *            {@link TextInputControl} implementation type
	 */
	private <N extends TextInputControl> void bindTextInputControl(final ValidationDecorator<N> input,
			final SimpleObjectPropertyBuffering<String> property)
	{
		bufferedProperties.add(property);
		input.getChild().textProperty().bindBidirectional(property);
		input.bindValidation(property);
	}

	/**
	 * Bind {@link ObservableListBuffering} to {@link TextInputControl} using {@link StringConverter}.
	 * 
	 * @param input
	 *            {@link ValidationDecorator} of {@link TextInputControl}
	 * @param property
	 *            {@link ObservableListBuffering} to bind to the <code>input</code>
	 * @param converter
	 *            {@link StringConverter} to use
	 * @param <T>
	 *            {@link ObservableListBuffering} element type
	 * @param <N>
	 *            {@link TextInputControl} implementation type
	 */
	private <T, N extends TextInputControl> void bindTextInputControl(final ValidationDecorator<N> input,
			final ObservableListBuffering<T> property, final StringConverter<ObservableList<T>> converter)
	{
		bufferedProperties.add(property);
		input.getChild().textProperty().bindBidirectional(property, converter);
		input.bindValidation(property);
	}

	/**
	 * Bind the view to the model.
	 * 
	 * @param newBufferingManager
	 *            the {@link BufferingManager} to use
	 * @param newRequirementSource
	 *            the {@link InputRequirementSource} to configure
	 * @param newSources
	 *            the input requirement source buffered list
	 * @param newSourcesName
	 *            the input requirement source buffered name list
	 * @param callBacks
	 *            {@link Callback} list where add own {@link Callback}
	 * @param mainCallBack
	 *            the main {@link Callback} to use when source name is modified
	 * @return the source name property
	 * @throws NoSuchMethodException
	 *             No such method exception
	 */
	public ObjectProperty<String> bind(final BufferingManager newBufferingManager,
			final InputRequirementSource newRequirementSource, final ObservableList<InputRequirementSource> newSources,
			final ObservableList<ObjectProperty<String>> newSourcesName,
			final ObservableList<Callback<InputRequirementSource, Void>> callBacks,
			final Callback<InputRequirementSource, Void> mainCallBack) throws NoSuchMethodException
	{
		requirementSource = newRequirementSource;
		bufferingManager = newBufferingManager;
		sources = newSources;
		sourcesName = newSourcesName;

		// Input requirement source name
		final SimpleObjectPropertyBuffering<String> nameProperty = new SimpleObjectPropertyBuffering<String>(
				requirementSource.nameProperty())
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering#validate()
			 */
			@Override
			public void validate()
			{
				super.validate();

				// must be unique
				final boolean unique = (sourcesName.stream().filter(s -> Objects.equals(s.get(), get())).count() == 1);
				if (!unique)
				{
					if (infoValidityProperty().isNull().get())
					{
						infoValidityProperty().set("");
					}
					else if (!validityProperty().get())
					{
						infoValidityProperty().set(infoValidityProperty().get() + '\n');
					}
					validityProperty().set(false);
					infoValidityProperty().set(infoValidityProperty().get() + errors.getString("not.unique"));
				}
			}
		};
		bufferingManager.add(nameProperty);
		titledPane.textProperty().bind(nameProperty);
		bindTextInputControl(name, nameProperty);

		nameProperty.addListener(e -> mainCallBack.call(requirementSource));

		final ObservableListBuffering<InputRequirementSource> coversProperty = bufferingManager
				.bufferingList(requirementSource, InputRequirementSource.COVERS);
		bindTextInputControl(covers, coversProperty, new InputRequirementSourceStringConverter());

		callBacks.add(c -> {
			// force to refresh validation using InputRequirementSourceStringConverter
			final String value = covers.getChild().getText();
			covers.getChild().setText("");
			covers.getChild().setText(value);

			nameProperty.validate();
			return null;
		});

		previewLimit.disableProperty().bind(useLimit.selectedProperty().not());
		previewLimit.getValueFactory().setConverter(new IntegerStringConverter()
		{
			@Override
			public Integer fromString(final String stringValue)
			{
				Integer value = null;
				try
				{
					value = super.fromString(stringValue);
				}
				catch (final NumberFormatException e)
				{
					if (previewLimit.getEditor().isUndoable())
					{
						previewLimit.getEditor().undo();
						value = fromString(previewLimit.getEditor().getText());
						if (value == null)
						{
							throw e;
						}
					}
				}
				return value;
			}
		});
		previewLimit.getEditor().focusedProperty().addListener((e, oldValue, newValue) -> {
			if (!newValue)
			{
				previewLimit.getEditor().fireEvent(new ActionEvent());
			}
		});
		preview.disableProperty()
				.bind(bufferingManager.bufferingProperty().or(Bindings.not(bufferingManager.validProperty())));

		final Node pluginNode = requirementSource.getProvider()
				.createSourceConfigurationEditor(requirementSource.getConfiguration(), bufferingManager);
		HBox.setHgrow(pluginNode, Priority.ALWAYS);
		pluginPane.getChildren().add(pluginNode);

		return nameProperty;
	}

	/**
	 * Action event to preview requirement parsing.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void preview(final ActionEvent event)
	{
		try
		{
			final int limit;
			if (useLimit.isSelected())
			{
				limit = previewLimit.getValue();
			}
			else
			{
				limit = Integer.MAX_VALUE;
			}

			final StringBuilder sourceText = new StringBuilder(Math.min(limit, 2 * 1024));

			RETAAnalysis.getInstance().parse(requirementSource, sourceText, limit);

			final ResourceBundle labels = ResourceBundle.getBundle("com/ben12/reta/view/Labels");

			final Window parent = titledPane.getScene().getWindow();
			final Dialog<Void> previewDialog = new Dialog<>();
			previewDialog.initOwner(parent);
			previewDialog.setTitle(labels.getString("preview.title"));
			previewDialog.setHeaderText(null);
			previewDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

			final FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("SourcePreviewUI.fxml"));
			loader.setResources(labels);
			final Pane root = (Pane) loader.load();

			final SourcePreviewController previewController = loader.getController();
			previewController.analysedTextProperty().setValue(sourceText.toString());
			previewController.resultTextProperty().setValue(requirementSource.toString());

			previewDialog.getDialogPane().setContent(root);
			previewDialog.setResizable(true);

			previewDialog.show();
		}
		catch (final RETAParseException | IOException e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
			MessageDialog.showErrorMessage(null, e.getMessage(), e);
		}
	}

	/**
	 * Disconnect the input requirement configuration from the {@link BufferingManager}.
	 */
	public void disconnect()
	{
		bufferingManager.removeAll(bufferedProperties);
	}

	/**
	 * Reconnect the input requirement configuration to the {@link BufferingManager}.
	 */
	public void reconnect()
	{
		bufferingManager.addAll(bufferedProperties);
	}

	/**
	 * Convert comma separated source name to {@link InputRequirementSource} list.
	 * 
	 * @author Benoît Moreau (ben.12)
	 */
	public final class InputRequirementSourceStringConverter
			extends StringConverter<ObservableList<InputRequirementSource>>
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see javafx.util.StringConverter#fromString(java.lang.String)
		 */
		@Override
		public ObservableList<InputRequirementSource> fromString(final String value)
		{
			final List<String> inputNames = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(value);
			final Collection<InputRequirementSource> result;
			if (sources.size() == sourcesName.size())
			{
				result = inputNames.stream().map(name -> {
					final AtomicInteger index = new AtomicInteger();
					return sources.stream().filter(e -> {
						final int id = index.getAndIncrement();
						return sourcesName.get(id).get().equals(name);
					}).findFirst().orElse(null);
				}).collect(Collectors.toCollection(LinkedHashSet::new));
			}
			else
			{
				result = new LinkedHashSet<>();
			}
			return FXCollections.observableArrayList(result);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javafx.util.StringConverter#toString(java.lang.Object)
		 */
		@Override
		public String toString(final ObservableList<InputRequirementSource> sourceList)
		{
			return sourceList.stream()
					.filter(Objects::nonNull)
					.map(InputRequirementSource::getName)
					.collect(Collectors.joining(", "));
		}
	}
}
