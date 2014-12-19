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
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;

import com.ben12.reta.api.RETAParseException;
import com.ben12.reta.beans.property.buffering.Buffering;
import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.control.MessageDialog;
import com.ben12.reta.view.validation.ValidationDecorator;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;

/**
 * @author Benoît Moreau (ben.12)
 */
public class SourceConfigurationController
{
	private BufferingManager						bufferingManager;

	/** Keeps a reference on buffered properties. */
	private final List<Buffering<?>>				bufferedProperties	= new ArrayList<>();

	private InputRequirementSource					requirementSource	= null;

	private ObservableList<InputRequirementSource>	sources				= null;

	private ObservableList<ObjectProperty<String>>	sourcesName			= null;

	private final ResourceBundle					errors				= ResourceBundle.getBundle("ValidationMessages");

	@FXML
	private TitledPane								titledPane;

	@FXML
	private ValidationDecorator<TextField>			name;

	@FXML
	private ValidationDecorator<TextField>			covers;

	@FXML
	private TextField								previewLimit;

	@FXML
	private Button									preview;

	private <N extends TextInputControl> void initializeLabeled(final ValidationDecorator<N> input,
			final SimpleObjectPropertyBuffering<String> property)
	{
		bufferedProperties.add(property);
		input.getChild().textProperty().bindBidirectional(property);
		input.bindValidation(property);
	}

	private <T, N extends TextInputControl> void initializeLabeled(final ValidationDecorator<N> input,
			final ObservableListBuffering<T> property, final StringConverter<ObservableList<T>> converter)
	{
		bufferedProperties.add(property);
		input.getChild().textProperty().bindBidirectional(property, converter);
		input.bindValidation(property);
	}

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
				JavaBeanStringPropertyBuilder.create()
						.bean(requirementSource)
						.name(InputRequirementSource.NAME)
						.build())
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
				final boolean unique = (sources.stream()
						.filter(s -> s != requirementSource && Objects.equal(s.getName(), get()))
						.count() == 0);
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
		initializeLabeled(name, nameProperty);

		nameProperty.addListener(e -> mainCallBack.call(requirementSource));

		initializeLabeled(covers, bufferingManager.bufferingList(requirementSource, InputRequirementSource.COVERS),
				new InputRequirementSourceStringConverter());

		callBacks.add(c -> {
			if (c != requirementSource)
			{
				// force to refresh validation
				final String value = covers.getChild().getText();
				covers.getChild().setText("");
				covers.getChild().setText(value);
			}
			return null;
		});

		preview.disableProperty().bind(
				bufferingManager.bufferingProperty().or(Bindings.not(bufferingManager.validProperty())));

		return nameProperty;
	}

	@FXML
	protected void preview(final ActionEvent event)
	{
		try
		{
			final String limitStr = previewLimit.getText();

			int limit;
			try
			{
				limit = Integer.parseInt(limitStr);
			}
			catch (final NumberFormatException e)
			{
				// limitStr empty or not a number
				limit = Integer.MAX_VALUE;
			}

			final StringBuilder sourceText = new StringBuilder(Math.min(limit, 2 * 1024));

			RETAAnalysis.getInstance().parse(requirementSource, sourceText, limit);

			final Stage previewStage = new Stage(StageStyle.UTILITY);
			previewStage.initOwner(titledPane.getScene().getWindow());
			previewStage.initModality(Modality.APPLICATION_MODAL);

			final ResourceBundle labels = ResourceBundle.getBundle("com/ben12/reta/view/Labels");

			final FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("SourcePreviewUI.fxml"));
			loader.setResources(labels);
			final Parent root = (Parent) loader.load();

			final SourcePreviewController previewController = loader.getController();
			previewController.analysedTextProperty().setValue(sourceText.toString());
			previewController.resultTextProperty().setValue(requirementSource.toString());

			previewStage.setScene(new Scene(root));
			previewStage.setTitle(labels.getString("preview.title"));
			previewStage.sizeToScene();
			previewStage.show();
		}
		catch (final RETAParseException | IOException e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
			MessageDialog.showErrorMessage(titledPane.getScene().getWindow(), e.getMessage());
		}
	}

	public void disconnect()
	{
		bufferingManager.removeAll(bufferedProperties);
	}

	public void reconnect()
	{
		bufferingManager.addAll(bufferedProperties);
	}

	public static final class EntryWrapper implements Entry<String, Integer>
	{
		private final Entry<String, Integer>	entry;

		/**
		 * @param newEntry
		 */
		public EntryWrapper(final Entry<String, Integer> newEntry)
		{
			entry = newEntry;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Map.Entry#getKey()
		 */
		@Override
		public String getKey()
		{
			return entry.getKey();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Map.Entry#getValue()
		 */
		@Override
		public Integer getValue()
		{
			return entry.getValue();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Map.Entry#setValue(java.lang.Object)
		 */
		@Override
		public Integer setValue(final Integer newValue)
		{
			return entry.setValue(newValue);
		}
	}

	public final class InputRequirementSourceStringConverter extends
			StringConverter<ObservableList<InputRequirementSource>>
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
					.filter(s -> s != null)
					.map(InputRequirementSource::getName)
					.collect(Collectors.joining(", "));
		}
	}
}
