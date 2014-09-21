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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.Requirement;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.buffering.Buffering;
import com.ben12.reta.view.buffering.BufferingManager;
import com.ben12.reta.view.buffering.ObservableListBuffering;
import com.ben12.reta.view.buffering.ObservableMapBuffering;
import com.ben12.reta.view.buffering.PropertyBufferingValidation;
import com.ben12.reta.view.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.view.control.MapTableView;
import com.ben12.reta.view.validation.ValidationUtils;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public class SourceConfigurationController
{
	private BufferingManager											bufferingManager;

	/** Keeps a reference on buffered properties. */
	private final List<Buffering<?>>									bufferedProperties	= new ArrayList<>();

	private ObservableList<InputRequirementSource>						sources				= null;

	private ObservableList<ObjectProperty<String>>						sourcesName			= null;

	@FXML
	private TitledPane													titledPane;

	@FXML
	private TextField													name;

	@FXML
	private ImageView													nameValidity;

	@FXML
	private TextField													sourcePath;

	@FXML
	private ImageView													sourcePathValidity;

	@FXML
	private TextField													filter;

	@FXML
	private ImageView													filterValidity;

	@FXML
	private TextField													reqStart;

	@FXML
	private ImageView													reqStartValidity;

	@FXML
	private TextField													reqEnd;

	@FXML
	private ImageView													reqEndValidity;

	@FXML
	private TextField													reqRef;

	@FXML
	private ImageView													reqRefValidity;

	@FXML
	private TextField													covers;

	@FXML
	private ImageView													coversValidity;

	@FXML
	private MapTableView<String, Integer>								attributesTable;

	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, String>	reqAttNameColumn;

	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, Integer>	reqAttGroupColumn;

	@FXML
	private TextField													newAttribute;

	@FXML
	private Button														addNewAttribute;

	@FXML
	private Button														deleteAttribute;

	private ObservableMapBuffering<String, Integer>						attributeMap;

	@FXML
	private MapTableView<String, Integer>								referencesTable;

	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, String>	refAttNameColumn;

	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, Integer>	refAttGroupColumn;

	@FXML
	private TextField													newReference;

	@FXML
	private Button														addNewReference;

	@FXML
	private Button														deleteReference;

	private ObservableMapBuffering<String, Integer>						referenceMap;

	private FileChooser													fileChooser			= null;

	public void initializeLabeled(TextInputControl input, ImageView validity,
			SimpleObjectPropertyBuffering<String> property)
	{
		bufferedProperties.add(property);
		input.textProperty().bindBidirectional(property);
		ValidationUtils.bindValidationLabel(input, validity, property);
	}

	public <T> void initializeLabeled(TextInputControl input, ImageView validity, ObservableListBuffering<T> property,
			StringConverter<ObservableList<T>> converter)
	{
		bufferedProperties.add(property);
		input.textProperty().bindBidirectional(property, converter);
		ValidationUtils.bindValidationLabel(input, validity, property);
	}

	public <T> void initializeLabeled(TextInputControl input, ImageView validity,
			PropertyBufferingValidation<T> property, StringConverter<T> converter)
	{
		bufferedProperties.add(property);
		input.textProperty().bindBidirectional(property, converter);
		ValidationUtils.bindValidationLabel(input, validity, property);
	}

	public ObjectProperty<String> bind(BufferingManager newBufferingManager, InputRequirementSource requirementSource,
			ObservableList<InputRequirementSource> newSources, ObservableList<ObjectProperty<String>> newSourcesName,
			ObservableList<Callback<InputRequirementSource, Void>> callBacks,
			Callback<InputRequirementSource, Void> mainCallBack) throws NoSuchMethodException
	{
		bufferingManager = newBufferingManager;
		sources = newSources;
		sourcesName = newSourcesName;

		// Input requirement source name
		SimpleObjectPropertyBuffering<String> nameProperty = new SimpleObjectPropertyBuffering<String>(
				JavaBeanStringPropertyBuilder.create()
						.bean(requirementSource)
						.name(InputRequirementSource.NAME)
						.build())
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see com.ben12.reta.view.buffering.SimpleObjectPropertyBuffering#validate()
			 */
			@Override
			public void validate()
			{
				super.validate();

				// must be unique
				boolean unique = (sources.stream()
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
					infoValidityProperty().set(infoValidityProperty().get() + "Must be unique");
				}
			}
		};
		bufferingManager.add(nameProperty);
		titledPane.textProperty().bind(nameProperty);
		initializeLabeled(name, nameValidity, nameProperty);

		nameProperty.addListener(e -> mainCallBack.call(requirementSource));

		// Input requirement source path
		StringConverter<Path> pathStringConverter = new StringConverter<Path>()
		{
			@Override
			public Path fromString(String pathname)
			{
				return (Strings.isNullOrEmpty(pathname) ? null : Paths.get(pathname));
			}

			@Override
			public String toString(Path file)
			{
				return (file != null ? file.toString() : null);
			}
		};

		final SimpleObjectPropertyBuffering<Path> sourcePathBuffered = bufferingManager.bufferingObject(
				requirementSource, InputRequirementSource.SOURCE_PATH);
		initializeLabeled(sourcePath, sourcePathValidity, sourcePathBuffered, pathStringConverter);

		// Input requirement source path filter
		initializeLabeled(filter, filterValidity,
				bufferingManager.bufferingString(requirementSource, InputRequirementSource.FILTER));
		filter.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			Path path = sourcePathBuffered.getValue();
			boolean isDirectory = true;
			if (path != null && !path.isAbsolute())
			{
				File config = RETAAnalysis.getInstance().getConfig();
				if (config != null)
				{
					path = config.getAbsoluteFile().getParentFile().toPath().resolve(path);
				}
				else
				{
					path = null;
				}
			}
			if (path != null)
			{
				isDirectory = !Files.exists(path) || Files.isDirectory(path);
			}
			return !isDirectory;
		}, sourcePathBuffered));

		// Input requirement source requirement start regex
		initializeLabeled(reqStart, reqStartValidity,
				bufferingManager.bufferingString(requirementSource, InputRequirementSource.REQ_START));

		// Input requirement source requirement end regex
		initializeLabeled(reqEnd, reqEndValidity,
				bufferingManager.bufferingString(requirementSource, InputRequirementSource.REQ_END));
		reqEnd.disableProperty().bind(reqStart.textProperty().isEqualTo(""));

		// Input requirement source requirement reference regex
		initializeLabeled(reqRef, reqRefValidity,
				bufferingManager.bufferingString(requirementSource, InputRequirementSource.REQ_REF));

		initializeLabeled(covers, coversValidity,
				bufferingManager.bufferingList(requirementSource, InputRequirementSource.COVERS),
				new InputRequirementSourceStringConverter());

		callBacks.add(c -> {
			if (c != requirementSource)
			{
				// force to refresh validation
				String value = covers.getText();
				covers.setText("");
				covers.setText(value);
			}
			return null;
		});

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, String>, ObservableValue<String>> cvpKey = (
				cdf) -> new ReadOnlyStringWrapper(cdf.getValue().getKey());

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, Integer>, ObservableValue<Integer>> cvpValue = (
				cdf) -> {
			return cdf.getValue().valueProperty();
		};

		attributeMap = bufferingManager.buffering(FXCollections.observableMap(requirementSource.getAttributesGroup()));
		bufferedProperties.add(attributeMap);
		attributesTable.setMapItems(attributeMap);

		reqAttNameColumn.setCellValueFactory(cvpKey);
		reqAttGroupColumn.setCellValueFactory(cvpValue);
		reqAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		referenceMap = bufferingManager.buffering(FXCollections.observableMap(requirementSource.getRefAttributesGroup()));
		bufferedProperties.add(referenceMap);
		referencesTable.setMapItems(referenceMap);

		refAttNameColumn.setCellValueFactory(cvpKey);
		refAttGroupColumn.setCellValueFactory(cvpValue);
		refAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		addNewAttribute.disableProperty().bind(newAttribute.textProperty().isEmpty());
		addNewReference.disableProperty().bind(newReference.textProperty().isEmpty());

		ReadOnlyObjectProperty<MapTableView<String, Integer>.Entry> selectedAttribute = attributesTable.getSelectionModel()
				.selectedItemProperty();
		deleteAttribute.disableProperty().bind(
				selectedAttribute.isNull()
						.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(Requirement.ATTRIBUTE_ID))
						.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(Requirement.ATTRIBUTE_TEXT)));
		deleteReference.disableProperty().bind(referencesTable.getSelectionModel().selectedItemProperty().isNull());

		return nameProperty;
	}

	@FXML
	protected void chooseSourcePathFile(ActionEvent event)
	{
		if (fileChooser == null)
		{
			fileChooser = new FileChooser();
			ObjectBinding<File> currentDir = Bindings.createObjectBinding(() -> {
				File file = new File(sourcePath.getText());
				if (file.isFile() || !file.exists())
				{
					file = file.getParentFile();
				}
				if (!file.exists())
				{
					file = null;
				}
				return file;
			}, sourcePath.textProperty());
			ObjectBinding<String> currentFileName = Bindings.createObjectBinding(() -> {
				File file = new File(sourcePath.getText());
				String fileName = null;
				if (file.isFile())
				{
					fileName = file.getName();
				}
				return fileName;
			}, sourcePath.textProperty());
			fileChooser.initialDirectoryProperty().bind(currentDir);
			fileChooser.initialFileNameProperty().bind(currentFileName);
		}
		File file = fileChooser.showOpenDialog(titledPane.getScene().getWindow());
		if (file != null)
		{
			sourcePath.setText(file.getPath());
		}
	}

	@FXML
	protected void chooseSourcePathFolder(ActionEvent event)
	{
		DirectoryChooser folderChooser = new DirectoryChooser();
		ObjectBinding<File> currentDir = Bindings.createObjectBinding(() -> {
			File file = new File(sourcePath.getText());
			if (file.isFile() || !file.exists())
			{
				file = file.getParentFile();
			}
			if (!file.exists())
			{
				file = null;
			}
			return file;
		}, sourcePath.textProperty());
		folderChooser.initialDirectoryProperty().bind(currentDir);
		File file = folderChooser.showDialog(null);
		if (file != null)
		{
			sourcePath.setText(file.getPath());
		}
	}

	@FXML
	protected void deleteAttribute(ActionEvent event)
	{
		MapTableView<String, Integer>.Entry entry = attributesTable.getSelectionModel().getSelectedItem();
		if (entry != null)
		{
			attributeMap.remove(entry.getKey());
		}
	}

	@FXML
	protected void addNewAttribute(ActionEvent event)
	{
		String name = newAttribute.getText();
		if (!name.isEmpty() && !attributeMap.containsKey(name))
		{
			attributeMap.put(name, null);
		}
	}

	@FXML
	protected void deleteReference(ActionEvent event)
	{
		MapTableView<String, Integer>.Entry entry = referencesTable.getSelectionModel().getSelectedItem();
		if (entry != null)
		{
			referenceMap.remove(entry.getKey());
		}
	}

	@FXML
	protected void addNewReference(ActionEvent event)
	{
		String name = newReference.getText();
		if (!name.isEmpty() && !referenceMap.containsKey(name))
		{
			referenceMap.put(name, null);
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
		public EntryWrapper(Entry<String, Integer> newEntry)
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
		public Integer setValue(Integer newValue)
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
		public ObservableList<InputRequirementSource> fromString(String value)
		{
			List<String> inputNames = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(value);
			final Collection<InputRequirementSource> result;
			if (sources.size() == sourcesName.size())
			{
				result = inputNames.stream().map(name -> {
					AtomicInteger index = new AtomicInteger();
					return sources.stream().filter(e -> {
						int id = index.getAndIncrement();
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
		public String toString(ObservableList<InputRequirementSource> sourceList)
		{
			return sourceList.stream()
					.filter(s -> s != null)
					.map(InputRequirementSource::getName)
					.collect(Collectors.joining(", "));
		}
	}
}
