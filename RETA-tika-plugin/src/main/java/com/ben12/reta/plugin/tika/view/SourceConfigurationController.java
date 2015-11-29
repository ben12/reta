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
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
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
import javafx.util.converter.IntegerStringConverter;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.property.buffering.Buffering;
import com.ben12.reta.beans.property.buffering.BufferingManager;
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

	/** Error translation {@link ResourceBundle}. */
	private final ResourceBundle										errors				= ResourceBundle
			.getBundle("com/ben12/reta/plugin/tika/beans/constraints/"
					+ ResourceBundleMessageInterpolator.USER_VALIDATION_MESSAGES);

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

	/** Requirement attribute name and matching groups table. */
	@FXML
	private ValidationDecorator<MapTableView<String, Integer>>			attributesTable;

	/** Requirement attribute name table column. */
	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, String>	reqAttNameColumn;

	/** Requirement attribute matching groups table column. */
	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, Integer>	reqAttGroupColumn;

	/** Field to add new requirement attribute name. */
	@FXML
	private TextField													newAttribute;

	/** Button to add new requirement attribute name. */
	@FXML
	private Button														addNewAttribute;

	/** Button to remove selected requirement attribute. */
	@FXML
	private Button														deleteAttribute;

	/** Requirement attributes name and matching groups buffered. */
	private ObservableMapBuffering<String, Integer>						attributeMap;

	/** Requirement reference attribute name and matching groups table. */
	@FXML
	private ValidationDecorator<MapTableView<String, Integer>>			referencesTable;

	/** Requirement reference attribute name table column. */
	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, String>	refAttNameColumn;

	/** Requirement reference attribute matching groups table column. */
	@FXML
	private TableColumn<MapTableView<String, Integer>.Entry, Integer>	refAttGroupColumn;

	/** Field to add new requirement reference attribute name. */
	@FXML
	private TextField													newReference;

	/** Button to add new requirement reference attribute name. */
	@FXML
	private Button														addNewReference;

	/** Button to remove selected requirement reference attribute. */
	@FXML
	private Button														deleteReference;

	/** Requirement reference attributes name and matching groups buffered. */
	private ObservableMapBuffering<String, Integer>						referenceMap;

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
	 * Bind the view to the model.
	 * 
	 * @param newBufferingManager
	 *            the {@link BufferingManager} to use
	 * @param newSourceConfiguration
	 *            the {@link TikaSourceConfiguration} to bind to the view
	 */
	public void bind(final BufferingManager newBufferingManager, final TikaSourceConfiguration newSourceConfiguration)
	{
		bufferingManager = newBufferingManager;

		final SimpleObjectPropertyBuffering<String> sourcePathBuffered = bufferingManager
				.buffering(newSourceConfiguration.sourcePathProperty());
		bindTextInputControl(sourcePath, sourcePathBuffered);

		// Input requirement source path filter
		if (filter != null)
		{
			bindTextInputControl(filter, bufferingManager.buffering(newSourceConfiguration.filterProperty()));
		}

		// Input requirement source requirement start regex
		final SimpleObjectPropertyBuffering<String> reqStartProperty = bufferingManager
				.buffering(newSourceConfiguration.reqStartProperty());
		bindTextInputControl(reqStart, reqStartProperty);

		// Input requirement source requirement end regex
		bindTextInputControl(reqEnd, bufferingManager.buffering(newSourceConfiguration.reqEndProperty()));
		reqEnd.disableProperty().bind(reqStart.getChild().textProperty().isEqualTo(""));

		// Input requirement source requirement reference regex
		final SimpleObjectPropertyBuffering<String> refReqProperty = bufferingManager
				.buffering(newSourceConfiguration.reqRefProperty());
		bindTextInputControl(reqRef, refReqProperty);

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, String>, ObservableValue<String>> cvpKey = (
				cdf) -> new ReadOnlyStringWrapper(cdf.getValue().getKey());

		final Callback<CellDataFeatures<MapTableView<String, Integer>.Entry, Integer>, ObservableValue<Integer>> cvpValue = (
				cdf) -> {
			return cdf.getValue().valueProperty();
		};

		attributeMap = new ObservableMapBuffering<String, Integer>(TikaSourceConfiguration.class,
				TikaSourceConfiguration.ATTRIBUTES_GROUP, newSourceConfiguration.getAttributesGroup())
		{
			@Override
			public void validate()
			{
				super.validate();

				try
				{
					final String regex = reqStartProperty.get();
					final Pattern pattern = Pattern.compile(regex);
					final Matcher matcher = pattern.matcher("");
					final int groupCount = matcher.groupCount();
					final List<Integer> invalidGroups = values().stream()
							.filter(group -> group != null && (group > groupCount || group < 0))
							.collect(Collectors.toList());

					if (!invalidGroups.isEmpty())
					{
						validityProperty().set(false);
						infoValidityProperty().set(String.format(errors.getString("bad.regex.group"),
								invalidGroups.stream().map(n -> n.toString()).collect(Collectors.joining(", ")),
								regex));
					}
				}
				catch (final PatternSyntaxException e)
				{
					// Invalid regex
				}
			}
		};
		bufferingManager.add(attributeMap);
		bufferedProperties.add(attributeMap);
		attributesTable.getChild().setMapItems(attributeMap);
		Platform.runLater(() -> attributesTable.getChild().sort());
		attributeMap.addListener((MapChangeListener<String, Integer>) (c) -> {
			attributesTable.getChild().sort();
			final int selection = attributesTable.getChild().getSelectionModel().getSelectedIndex();
			if (selection >= 0)
			{
				attributesTable.getChild().scrollTo(selection);
				attributesTable.getChild().refresh();
			}
		});
		attributesTable.bindValidation(attributeMap);
		reqStartProperty.addListener((c, o, n) -> attributeMap.validate());

		reqAttNameColumn.setCellValueFactory(cvpKey);
		reqAttGroupColumn.setCellValueFactory(cvpValue);
		reqAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		referenceMap = new ObservableMapBuffering<String, Integer>(TikaSourceConfiguration.class,
				TikaSourceConfiguration.REF_ATTRIBUTES_GROUP, newSourceConfiguration.getRefAttributesGroup())
		{
			@Override
			public void validate()
			{
				super.validate();

				try
				{
					final String regex = refReqProperty.get();
					final Pattern pattern = Pattern.compile(regex);
					final Matcher matcher = pattern.matcher("");
					final int groupCount = matcher.groupCount();
					final List<Integer> invalidGroups = values().stream()
							.filter(group -> group != null && (group > groupCount || group < 0))
							.collect(Collectors.toList());

					if (!invalidGroups.isEmpty())
					{
						validityProperty().set(false);
						infoValidityProperty().set(String.format(errors.getString("bad.regex.group"),
								invalidGroups.stream().map(n -> n.toString()).collect(Collectors.joining(", ")),
								regex));
					}
				}
				catch (final PatternSyntaxException e)
				{
					// Invalid regex
				}
			}
		};
		bufferingManager.add(referenceMap);
		bufferedProperties.add(referenceMap);
		referencesTable.getChild().setMapItems(referenceMap);
		Platform.runLater(() -> referencesTable.getChild().sort());
		referenceMap.addListener((MapChangeListener<String, Integer>) (c) -> {
			Platform.runLater(() -> {
				referencesTable.getChild().sort();
				final int selection = referencesTable.getChild().getSelectionModel().getSelectedIndex();
				if (selection >= 0)
				{
					referencesTable.getChild().scrollTo(selection);
					referencesTable.getChild().refresh();
				}
			});
		});
		referencesTable.bindValidation(referenceMap);
		refReqProperty.addListener((c, o, n) -> referenceMap.validate());

		refAttNameColumn.setCellValueFactory(cvpKey);
		refAttGroupColumn.setCellValueFactory(cvpValue);
		refAttGroupColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

		addNewAttribute.disableProperty().bind(newAttribute.textProperty().isEmpty());
		addNewReference.disableProperty().bind(newReference.textProperty().isEmpty());

		final ReadOnlyObjectProperty<MapTableView<String, Integer>.Entry> selectedAttribute = attributesTable.getChild()
				.getSelectionModel()
				.selectedItemProperty();
		deleteAttribute.disableProperty().bind(selectedAttribute.isNull()
				.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(SourceConfiguration.ATTRIBUTE_ID))
				.or(Bindings.selectString(selectedAttribute, "key").isEqualTo(SourceConfiguration.ATTRIBUTE_TEXT)));
		deleteReference.disableProperty()
				.bind(referencesTable.getChild().getSelectionModel().selectedItemProperty().isNull());
	}

	/**
	 * Action event use to choose the requirement source path for a file.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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
			} , sourcePath.getChild().textProperty());
			final ObjectBinding<String> currentFileName = Bindings.createObjectBinding(() -> {
				final File file = new File(sourcePath.getChild().getText());
				String fileName = null;
				if (file.isFile())
				{
					fileName = file.getName();
				}
				return fileName;
			} , sourcePath.getChild().textProperty());
			fileChooser.initialDirectoryProperty().bind(currentDir);
			fileChooser.initialFileNameProperty().bind(currentFileName);
		}
		final File file = fileChooser.showOpenDialog(sourcePath.getScene().getWindow());
		if (file != null)
		{
			sourcePath.getChild().setText(file.getPath());
		}
	}

	/**
	 * Action event use to choose the requirement source path for a folder.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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
		} , sourcePath.getChild().textProperty());
		folderChooser.initialDirectoryProperty().bind(currentDir);
		final File file = folderChooser.showDialog(null);
		if (file != null)
		{
			sourcePath.getChild().setText(file.getPath());
		}
	}

	/**
	 * Action event use to delete the selected requirement attribute.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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

	/**
	 * Action event use to add a new requirement attribute.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void addNewAttribute(final ActionEvent event)
	{
		final String name = newAttribute.getText();
		if (!name.isEmpty() && !attributeMap.containsKey(name))
		{
			attributeMap.put(name, null);
		}
	}

	/**
	 * Action event use to delete the selected requirement reference attribute.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void deleteReference(final ActionEvent event)
	{
		final MapTableView<String, Integer>.Entry entry = referencesTable.getChild()
				.getSelectionModel()
				.getSelectedItem();
		if (entry != null)
		{
			referenceMap.remove(entry.getKey());
		}
	}

	/**
	 * Action event use to add a new requirement reference attribute.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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
