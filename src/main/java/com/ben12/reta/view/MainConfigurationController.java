// Package : com.ben12.reta.view
// File : MainConfigurationController.java
// 
// Copyright (C) 2014 Beno�t Moreau (ben.12)
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.StringConverter;

import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.buffering.BufferingManager;
import com.ben12.reta.view.buffering.ObservableListBuffering;
import com.google.common.base.Strings;

/**
 * @author Beno�t Moreau (ben.12)
 */
public class MainConfigurationController implements Initializable
{
	private final BufferingManager											bufferingManager	= new BufferingManager();

	private final ObjectProperty<File>										bufferedOutput;

	private final ObservableList<InputRequirementSource>					sources				= FXCollections.observableArrayList();

	private final ObservableList<InputRequirementSource>					bufferedSources;

	private final ObservableList<ObjectProperty<String>>					sourcesName			= FXCollections.observableArrayList();

	private final ObservableList<ObjectProperty<String>>					bufferedSourcesName;

	private final ObservableList<Callback<InputRequirementSource, Void>>	nameChangeCallBacks	= FXCollections.observableArrayList();

	private final ObservableList<Callback<InputRequirementSource, Void>>	bufferedNameChangeCallBacks;

	private final Callback<InputRequirementSource, Void>					refreshAll			= (InputRequirementSource e) -> {
																									nameChangeCallBacks.stream()
																											.forEach(
																													c -> c.call(e));
																									return null;
																								};

	private List<TitledPane>												panes;

	@FXML
	private Parent															root;

	@FXML
	private Accordion														sourceConfigurations;

	@FXML
	private Button															save;

	@FXML
	private Button															cancel;

	@FXML
	private Button															run;

	@FXML
	private Button															delete;

	@FXML
	private Button															upSource;

	@FXML
	private Button															downSource;

	@FXML
	private TextField														outputFile;

	/**
	 * @throws NoSuchMethodException
	 */
	public MainConfigurationController() throws NoSuchMethodException
	{
		bufferedSources = new ObservableListBuffering<InputRequirementSource>(RETAAnalysis.class,
				RETAAnalysis.REQUIREMENT_SOURCES, sources);
		bufferingManager.add((ObservableListBuffering<InputRequirementSource>) bufferedSources);

		bufferedSourcesName = bufferingManager.buffering(sourcesName);
		bufferedNameChangeCallBacks = bufferingManager.buffering(nameChangeCallBacks);
		bufferedOutput = bufferingManager.bufferingObject(RETAAnalysis.getInstance(), "output");
	}

	/**
	 * 
	 */
	private void rebuild()
	{
		while (bufferedSources.size() > 0)
		{
			removeSource(0);
		}

		outputFile.setText(RETAAnalysis.getInstance().getOutput().getPath());

		bufferingManager.commit();
		panes = new ArrayList<>();

		initialize(null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		try
		{
			sources.addAll(RETAAnalysis.getInstance().getRequirementSources().values());

			for (InputRequirementSource requirementSource : sources)
			{
				sourcesName.add(addSource(requirementSource));
			}

			panes = new ArrayList<>(sourceConfigurations.getPanes());

			outputFile.textProperty().bindBidirectional(bufferedOutput, new StringConverter<File>()
			{
				@Override
				public File fromString(String pathname)
				{
					return (Strings.isNullOrEmpty(pathname) ? null : new File(pathname));
				}

				@Override
				public String toString(File file)
				{
					return (file != null ? file.getPath() : null);
				}
			});

			delete.disableProperty().bind(sourceConfigurations.expandedPaneProperty().isNull());
			upSource.disableProperty().bind(sourceConfigurations.expandedPaneProperty().isNull());
			downSource.disableProperty().bind(sourceConfigurations.expandedPaneProperty().isNull());

			save.disableProperty().bind(
					Bindings.not(bufferingManager.bufferingProperty()).or(
							Bindings.not(bufferingManager.validProperty())));
			cancel.disableProperty().bind(Bindings.not(bufferingManager.bufferingProperty()));
			run.disableProperty().bind(
					bufferingManager.bufferingProperty().or(Bindings.not(bufferingManager.validProperty())));
		}
		catch (Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
	}

	private ObjectProperty<String> addSource(InputRequirementSource requirementSource) throws IOException,
			NoSuchMethodException
	{
		FXMLLoader loader = new FXMLLoader();
		loader.setBuilderFactory(new JavaFXBuilderFactory());
		loader.setLocation(MainConfigurationController.class.getResource("SourceConfigurationUI.fxml"));
		TitledPane inputPane = (TitledPane) loader.load();
		SourceConfigurationController controller = loader.getController();
		ObjectProperty<String> sourceName = controller.bind(bufferingManager, requirementSource, bufferedSources,
				bufferedSourcesName, nameChangeCallBacks, refreshAll);
		sourceConfigurations.getPanes().add(inputPane);
		sourceConfigurations.setExpandedPane(inputPane);
		inputPane.getProperties().put("reta.controller", controller);
		return sourceName;
	}

	@FXML
	protected void newSource(ActionEvent event) throws NoSuchMethodException, IOException
	{
		InputRequirementSource requirementSource = new InputRequirementSource("NEW", "", "");

		bufferedSourcesName.add(addSource(requirementSource));

		bufferedSources.add(requirementSource);
		refreshAll.call(requirementSource);
	}

	@FXML
	protected void deleteSource(ActionEvent event)
	{
		int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 0)
		{
			InputRequirementSource requirementSource = removeSource(index);
			refreshAll.call(requirementSource);
		}
	}

	private InputRequirementSource removeSource(int index)
	{
		TitledPane pane = sourceConfigurations.getPanes().remove(index);
		InputRequirementSource requirementSource = bufferedSources.remove(index);
		bufferedSourcesName.remove(index);
		bufferedNameChangeCallBacks.remove(index);
		if (index < bufferedSources.size())
		{
			sourceConfigurations.setExpandedPane(sourceConfigurations.getPanes().get(index));
		}
		else if (index > 0 && index - 1 < bufferedSources.size())
		{
			sourceConfigurations.setExpandedPane(sourceConfigurations.getPanes().get(index - 1));
		}
		SourceConfigurationController controller = (SourceConfigurationController) pane.getProperties().get(
				"reta.controller");
		controller.disconnect();
		return requirementSource;
	}

	@FXML
	protected void upSource(ActionEvent event)
	{
		int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 1)
		{
			TitledPane p = sourceConfigurations.getPanes().remove(index);
			sourceConfigurations.getPanes().add(index - 1, p);
			sourceConfigurations.setExpandedPane(p);
			ObjectProperty<String> sourceName = bufferedSourcesName.remove(index);
			Callback<InputRequirementSource, Void> callback = bufferedNameChangeCallBacks.remove(index);
			InputRequirementSource requirementSource = bufferedSources.remove(index);
			bufferedSourcesName.add(index - 1, sourceName);
			bufferedNameChangeCallBacks.add(index - 1, callback);
			bufferedSources.add(index - 1, requirementSource);
		}
	}

	@FXML
	protected void downSource(ActionEvent event)
	{
		int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 0 && index + 1 < bufferedSources.size())
		{
			TitledPane p = sourceConfigurations.getPanes().remove(index);
			sourceConfigurations.getPanes().add(index + 1, p);
			sourceConfigurations.setExpandedPane(p);
			ObjectProperty<String> sourceName = bufferedSourcesName.remove(index);
			Callback<InputRequirementSource, Void> callback = bufferedNameChangeCallBacks.remove(index);
			InputRequirementSource requirementSource = bufferedSources.remove(index);
			bufferedSourcesName.add(index + 1, sourceName);
			bufferedNameChangeCallBacks.add(index + 1, callback);
			bufferedSources.add(index + 1, requirementSource);
		}
	}

	@FXML
	protected void save(ActionEvent event)
	{
		bufferingManager.commit();
		panes = new ArrayList<>(sourceConfigurations.getPanes());

		RETAAnalysis.getInstance().getRequirementSources().clear();
		for (InputRequirementSource requirementSource : bufferedSources)
		{
			RETAAnalysis.getInstance().getRequirementSources().put(requirementSource.getName(), requirementSource);
		}
	}

	@FXML
	protected void cancel(ActionEvent event)
	{
		List<TitledPane> toDisconnected = new ArrayList<TitledPane>(sourceConfigurations.getPanes());
		toDisconnected.removeAll(panes);

		List<TitledPane> toReconnect = new ArrayList<TitledPane>(panes);
		toReconnect.removeAll(sourceConfigurations.getPanes());

		// reconnect before revert (reverts disconnected properties)
		toReconnect.stream().forEach(
				p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).reconnect());

		bufferingManager.revert();

		// disconnect after revert (reverts to be disconnected properties)
		toDisconnected.stream().forEach(
				p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).disconnect());

		TitledPane pane = sourceConfigurations.getExpandedPane();

		sourceConfigurations.getPanes().clear();
		sourceConfigurations.getPanes().addAll(panes);
		if (sourceConfigurations.getPanes().contains(pane))
		{
			sourceConfigurations.setExpandedPane(pane);
		}
	}

	@FXML
	protected void run(ActionEvent event) throws IOException
	{
		RETAAnalysis.getInstance().parse();
		RETAAnalysis.getInstance().analyse();
		RETAAnalysis.getInstance().writeExcel();
	}

	@FXML
	protected void selectOutputFile(ActionEvent e)
	{
		File currentFile = bufferedOutput.get();
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Excel file", "*.xls", "*.xlsx"));
		fileChooser.setTitle("RETA analysis output file");
		if (currentFile != null)
		{
			fileChooser.setInitialDirectory(currentFile.getParentFile());
			fileChooser.setInitialFileName(currentFile.getName());
		}
		File file = fileChooser.showSaveDialog(root.getScene().getWindow());

		if (file != null)
		{
			bufferedOutput.set(file);
		}
	}

	@FXML
	protected void open(ActionEvent e)
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("RETA analysis description", "*.reta"));
		fileChooser.setTitle("RETA analysis");
		File file = fileChooser.showOpenDialog(root.getScene().getWindow());

		if (file != null && file.isFile())
		{
			RETAAnalysis.getInstance().configure(file);
			rebuild();
		}
	}
}