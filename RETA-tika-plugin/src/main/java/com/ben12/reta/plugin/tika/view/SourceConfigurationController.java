// Package : com.ben12.reta.plugin.tika.view
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
package com.ben12.reta.plugin.tika.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.property.buffering.Buffering;
import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.beans.property.buffering.ObservableMapBuffering;
import com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.plugin.tika.model.TikaSourceConfiguration;
import com.ben12.reta.plugin.tika.view.control.MapTableView;
import com.ben12.reta.view.validation.ValidationDecorator;

/**
 * @author Benoît Moreau (ben.12)
 */
public class SourceConfigurationController
{
	/** Buffering manager. */
	private BufferingManager											bufferingManager;

	/** File chooser. */
	private FileChooser													fileChooser			= null;

	/** Keeps a reference on buffered properties. */
	private final List<Buffering<?>>									bufferedProperties	= new ArrayList<>();

	/** Source path text field. */
	@FXML
	private ValidationDecorator<TextField>								sourcePath;

	/** Filter text field. */
	@FXML
	private ValidationDecorator<TextField>								filter;

	/** Requirement start regex text field. */
	@FXML
	private ValidationDecorator<TextField>								reqStart;

	/** Requirement end regex text field. */
	@FXML
	private ValidationDecorator<TextField>								reqEnd;

	/** Reference regex text field. */
	@FXML
	private ValidationDecorator<TextField>								reqRef;

	@FXML
	private ValidationDecorator<MapTableView<String, Integer>>			attributesTable;

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

	public void bind(final BufferingManager newBufferingManager, final TikaSourceConfiguration newSourceConfiguration)
			throws NoSuchMethodException
	{
		bufferingManager = newBufferingManager;

		final SimpleObjectPropertyBuffering<String> sourcePathBuffered = bufferingManager.bufferingString(
				newSourceConfiguration, TikaSourceConfiguration.SOURCE_PATH);
		initializeLabeled(sourcePath, sourcePathBuffered);

		// Input requirement source path filter
		if (filter != null)
		{
			initializeLabeled(filter,
					bufferingManager.bufferingString(newSourceConfiguration, TikaSourceConfiguration.FILTER));
		}

		// Input requirement source requirement start regex
		initializeLabeled(reqStart,
				bufferingManager.bufferingString(newSourceConfiguration, TikaSourceConfiguration.REQ_START));

		// Input requirement source requirement end regex
		initializeLabeled(reqEnd,
				bufferingManager.bufferingString(newSourceConfiguration, TikaSourceConfiguration.REQ_END));
		reqEnd.disableProperty().bind(reqStart.getChild().textProperty().isEqualTo(""));

		// Input requirement source requirement reference regex
		initializeLabeled(reqRef,
				bufferingManager.bufferingString(newSourceConfiguration, TikaSourceConfiguration.REQ_REF));

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, String>, ObservableValue<String>> cvpKey = (
				cdf) -> new ReadOnlyStringWrapper(cdf.getValue().getKey());

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, Integer>, ObservableValue<Integer>> cvpValue = (
				cdf) -> {
			return cdf.getValue().valueProperty();
		};

		attributeMap = bufferingManager.buffering(FXCollections.observableMap(newSourceConfiguration.getAttributesGroup()));
		bufferedProperties.add(attributeMap);
		attributesTable.getChild().setMapItems(attributeMap);
		attributesTable.bindValidation(attributeMap);

		reqAttNameColumn.setCellValueFactory(cvpKey);
		reqAttGroupColumn.setCellValueFactory(cvpValue);
		reqAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		referenceMap = bufferingManager.buffering(FXCollections.observableMap(newSourceConfiguration.getRefAttributesGroup()));
		bufferedProperties.add(referenceMap);
		referencesTable.setMapItems(referenceMap);

		refAttNameColumn.setCellValueFactory(cvpKey);
		refAttGroupColumn.setCellValueFactory(cvpValue);
		refAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		addNewAttribute.disableProperty().bind(newAttribute.textProperty().isEmpty());
		addNewReference.disableProperty().bind(newReference.textProperty().isEmpty());

		final ReadOnlyObjectProperty<MapTableView<String, Integer>.Entry> selectedAttribute = attributesTable.getChild()
				.getSelectionModel()
				.selectedItemProperty();
		deleteAttribute.disableProperty()
				.bind(selectedAttribute.isNull()
						.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(SourceConfiguration.ATTRIBUTE_ID))
						.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(
								SourceConfiguration.ATTRIBUTE_TEXT)));
		deleteReference.disableProperty().bind(referencesTable.getSelectionModel().selectedItemProperty().isNull());
	}

	@FXML
	protected void chooseSourcePathFile(final ActionEvent event)
	{
		if (fileChooser == null)
		{
			fileChooser = new FileChooser();
			final ObjectBinding<File> currentDir = Bindings.createObjectBinding(() -> {
				File file = new File(sourcePath.getChild().getText());
				if (file.isFile() || !file.exists())
				{
					file = file.getParentFile();
				}
				if (!file.exists())
				{
					file = null;
				}
				return file;
			}, sourcePath.getChild().textProperty());
			final ObjectBinding<String> currentFileName = Bindings.createObjectBinding(() -> {
				final File file = new File(sourcePath.getChild().getText());
				String fileName = null;
				if (file.isFile())
				{
					fileName = file.getName();
				}
				return fileName;
			}, sourcePath.getChild().textProperty());
			fileChooser.initialDirectoryProperty().bind(currentDir);
			fileChooser.initialFileNameProperty().bind(currentFileName);
		}
		final File file = fileChooser.showOpenDialog(sourcePath.getScene().getWindow());
		if (file != null)
		{
			sourcePath.getChild().setText(file.getPath());
		}
	}

	@FXML
	protected void chooseSourcePathFolder(final ActionEvent event)
	{
		final DirectoryChooser folderChooser = new DirectoryChooser();
		final ObjectBinding<File> currentDir = Bindings.createObjectBinding(() -> {
			File file = new File(sourcePath.getChild().getText());
			if (file.isFile() || !file.exists())
			{
				file = file.getParentFile();
			}
			if (!file.exists())
			{
				file = null;
			}
			return file;
		}, sourcePath.getChild().textProperty());
		folderChooser.initialDirectoryProperty().bind(currentDir);
		final File file = folderChooser.showDialog(null);
		if (file != null)
		{
			sourcePath.getChild().setText(file.getPath());
		}
	}

	@FXML
	protected void deleteAttribute(final ActionEvent event)
	{
		final MapTableView<String, Integer>.Entry entry = attributesTable.getChild()
				.getSelectionModel()
				.getSelectedItem();
		if (entry != null)
		{
			attributeMap.remove(entry.getKey());
		}
	}

	@FXML
	protected void addNewAttribute(final ActionEvent event)
	{
		final String name = newAttribute.getText();
		if (!name.isEmpty() && !attributeMap.containsKey(name))
		{
			attributeMap.put(name, null);
		}
	}

	@FXML
	protected void deleteReference(final ActionEvent event)
	{
		final MapTableView<String, Integer>.Entry entry = referencesTable.getSelectionModel().getSelectedItem();
		if (entry != null)
		{
			referenceMap.remove(entry.getKey());
		}
	}

	@FXML
	protected void addNewReference(final ActionEvent event)
	{
		final String name = newReference.getText();
		if (!name.isEmpty() && !referenceMap.containsKey(name))
		{
			referenceMap.put(name, null);
		}
	}

}
