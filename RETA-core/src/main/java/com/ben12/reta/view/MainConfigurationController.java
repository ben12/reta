// Package : com.ben12.reta.view
// File : MainConfigurationController.java
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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;

import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.control.MessageDialog;
import com.ben12.reta.view.validation.ValidationDecorator;
import com.google.common.io.Files;

/**
 * @author Benoît Moreau (ben.12)
 */
public class MainConfigurationController implements Initializable
{
	private final BufferingManager											bufferingManager	= new BufferingManager();

	private final SimpleObjectPropertyBuffering<String>						bufferedOutput;

	private final ObservableList<InputRequirementSource>					sources				= FXCollections.observableArrayList();

	private final ObservableList<InputRequirementSource>					bufferedSources;

	private final ObservableList<ObjectProperty<String>>					sourcesName			= FXCollections.observableArrayList();

	private final ObservableList<ObjectProperty<String>>					bufferedSourcesName;

	private final ObservableList<Callback<InputRequirementSource, Void>>	nameChangeCallBacks	= FXCollections.observableArrayList();

	private final Callback<InputRequirementSource, Void>					refreshAll;

	private ResourceBundle													labels				= null;

	private List<TitledPane>												panes				= new ArrayList<>();

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
	private ValidationDecorator<TextField>									outputFile;

	/**
	 * @throws NoSuchMethodException
	 */
	public MainConfigurationController() throws NoSuchMethodException
	{
		refreshAll = (final InputRequirementSource e) -> {
			nameChangeCallBacks.stream().forEach(c -> c.call(e));
			return null;
		};

		bufferedSources = new ObservableListBuffering<InputRequirementSource>(RETAAnalysis.class,
				RETAAnalysis.REQUIREMENT_SOURCES, sources);
		bufferingManager.add((ObservableListBuffering<InputRequirementSource>) bufferedSources);

		bufferedSourcesName = bufferingManager.buffering(sourcesName);
		bufferedOutput = bufferingManager.bufferingString(RETAAnalysis.getInstance(), "output");
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

		try
		{
			sources.clear();
			sources.addAll(RETAAnalysis.getInstance().getRequirementSources().values());

			for (final InputRequirementSource requirementSource : sources)
			{
				sourcesName.add(addSource(requirementSource));
			}

			panes = new ArrayList<>(sourceConfigurations.getPanes());
			if (!panes.isEmpty())
			{
				sourceConfigurations.setExpandedPane(panes.get(0));
			}

			bufferingManager.revert();
		}
		catch (final Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	@Override
	public void initialize(final URL location, final ResourceBundle resources)
	{
		labels = resources;

		try
		{
			outputFile.getChild().textProperty().bindBidirectional(bufferedOutput);
			outputFile.bindValidation(bufferedOutput);

			delete.disableProperty().bind(
					sourceConfigurations.expandedPaneProperty()
							.isNull()
							.or(Bindings.size(sourceConfigurations.getPanes()).lessThan(2)));

			final IntegerBinding indexOfExpendedPane = new IntegerBinding()
			{
				{
					bind(sourceConfigurations.expandedPaneProperty());
				}

				@Override
				public void dispose()
				{
					super.unbind(sourceConfigurations.expandedPaneProperty());
				}

				@Override
				protected int computeValue()
				{
					return sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
				}

				@Override
				public ObservableList<?> getDependencies()
				{
					return FXCollections.singletonObservableList(sourceConfigurations.expandedPaneProperty());
				}
			};

			upSource.disableProperty().bind(
					sourceConfigurations.expandedPaneProperty().isNull().or(indexOfExpendedPane.lessThan(1)));
			downSource.disableProperty().bind(
					sourceConfigurations.expandedPaneProperty()
							.isNull()
							.or(indexOfExpendedPane.greaterThan(Bindings.size(sourceConfigurations.getPanes())
									.subtract(2))));

			save.disableProperty().bind(
					Bindings.not(bufferingManager.bufferingProperty()).or(
							Bindings.not(bufferingManager.validProperty())));
			cancel.disableProperty().bind(Bindings.not(bufferingManager.bufferingProperty()));
			run.disableProperty().bind(
					bufferingManager.bufferingProperty().or(Bindings.not(bufferingManager.validProperty())));

			newSource(null);
		}
		catch (final Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
	}

	private ObjectProperty<String> addSource(final InputRequirementSource requirementSource) throws IOException,
			NoSuchMethodException
	{
		final FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainConfigurationController.class.getResource("SourceConfigurationUI.fxml"));
		loader.setResources(labels);
		final TitledPane inputPane = (TitledPane) loader.load();
		final SourceConfigurationController controller = loader.getController();
		final ObjectProperty<String> sourceName = controller.bind(bufferingManager, requirementSource, bufferedSources,
				bufferedSourcesName, nameChangeCallBacks, refreshAll);
		sourceConfigurations.getPanes().add(inputPane);
		sourceConfigurations.setExpandedPane(inputPane);
		inputPane.getProperties().put("reta.controller", controller);
		return sourceName;
	}

	@FXML
	protected void newSource(final ActionEvent event) throws NoSuchMethodException, IOException
	{
		final InputRequirementSource requirementSource = new InputRequirementSource("NEW", null, null); // TODO

		bufferedSourcesName.add(addSource(requirementSource));

		bufferedSources.add(requirementSource);
		refreshAll.call(requirementSource);
	}

	@FXML
	protected void deleteSource(final ActionEvent event)
	{
		final int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 0)
		{
			final InputRequirementSource requirementSource = removeSource(index);
			refreshAll.call(requirementSource);
		}
	}

	private InputRequirementSource removeSource(final int index)
	{
		final TitledPane pane = sourceConfigurations.getPanes().remove(index);
		final InputRequirementSource requirementSource = bufferedSources.remove(index);
		bufferedSourcesName.remove(index);
		if (index < sourceConfigurations.getPanes().size())
		{
			sourceConfigurations.setExpandedPane(sourceConfigurations.getPanes().get(index));
		}
		else if (index > 0 && index - 1 < sourceConfigurations.getPanes().size())
		{
			sourceConfigurations.setExpandedPane(sourceConfigurations.getPanes().get(index - 1));
		}
		final SourceConfigurationController controller = (SourceConfigurationController) pane.getProperties().get(
				"reta.controller");
		controller.disconnect();
		return requirementSource;
	}

	@FXML
	protected void upSource(final ActionEvent event)
	{
		final int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 1)
		{
			final TitledPane p = sourceConfigurations.getPanes().remove(index);
			sourceConfigurations.getPanes().add(index - 1, p);
			sourceConfigurations.setExpandedPane(p);
			final ObjectProperty<String> sourceName = bufferedSourcesName.remove(index);
			final InputRequirementSource requirementSource = bufferedSources.remove(index);
			bufferedSourcesName.add(index - 1, sourceName);
			bufferedSources.add(index - 1, requirementSource);
		}
	}

	@FXML
	protected void downSource(final ActionEvent event)
	{
		final int index = sourceConfigurations.getPanes().indexOf(sourceConfigurations.getExpandedPane());
		if (index >= 0 && index + 1 < bufferedSources.size())
		{
			final TitledPane p = sourceConfigurations.getPanes().remove(index);
			sourceConfigurations.getPanes().add(index + 1, p);
			sourceConfigurations.setExpandedPane(p);
			final ObjectProperty<String> sourceName = bufferedSourcesName.remove(index);
			final InputRequirementSource requirementSource = bufferedSources.remove(index);
			bufferedSourcesName.add(index + 1, sourceName);
			bufferedSources.add(index + 1, requirementSource);
		}
	}

	@FXML
	protected void save(final ActionEvent event)
	{
		final RETAAnalysis retaAnalysis = RETAAnalysis.getInstance();

		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter(labels.getString("reta.file.desc"), "*.reta"));
		fileChooser.setTitle(labels.getString("save.title"));
		if (retaAnalysis.getConfig() != null)
		{
			fileChooser.setInitialDirectory(retaAnalysis.getConfig().getParentFile());
			fileChooser.setInitialFileName(retaAnalysis.getConfig().getName());
		}

		final File file = fileChooser.showSaveDialog(root.getScene().getWindow());

		if (file != null)
		{
			bufferingManager.commit();
			panes = new ArrayList<>(sourceConfigurations.getPanes());

			retaAnalysis.getRequirementSources().clear();
			for (final InputRequirementSource requirementSource : bufferedSources)
			{
				retaAnalysis.getRequirementSources().put(requirementSource.getName(), requirementSource);
			}

			try
			{
				if (file.isFile())
				{
					Files.copy(retaAnalysis.getConfig(), new File(retaAnalysis.getConfig().getPath() + ".bak"));
				}
				retaAnalysis.saveConfig(file);
			}
			catch (final IOException e)
			{
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
			}
		}
	}

	@FXML
	protected void cancel(final ActionEvent event)
	{
		final List<TitledPane> toDisconnected = new ArrayList<TitledPane>(sourceConfigurations.getPanes());
		toDisconnected.removeAll(panes);

		final List<TitledPane> toReconnect = new ArrayList<TitledPane>(panes);
		toReconnect.removeAll(sourceConfigurations.getPanes());

		// reconnect before revert (reverts disconnected properties)
		toReconnect.stream().forEach(
				p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).reconnect());

		bufferingManager.revert();

		// disconnect after revert (reverts to be disconnected properties)
		toDisconnected.stream().forEach(
				p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).disconnect());

		final TitledPane pane = sourceConfigurations.getExpandedPane();

		sourceConfigurations.getPanes().clear();
		sourceConfigurations.getPanes().addAll(panes);
		if (sourceConfigurations.getPanes().contains(pane))
		{
			sourceConfigurations.setExpandedPane(pane);
		}
		else if (!sourceConfigurations.getPanes().isEmpty())
		{
			sourceConfigurations.setExpandedPane(sourceConfigurations.getPanes().get(0));
		}
	}

	@FXML
	protected void run(final ActionEvent event)
	{
		final StringProperty stepMessage = new SimpleStringProperty("");
		final DoubleProperty progress = new SimpleDoubleProperty(0);
		final DoubleProperty readProgress = new SimpleDoubleProperty(0);

		MessageDialog.showProgressBar(root.getScene().getWindow(), labels.getString("progress.title"), stepMessage,
				progress);

		new Thread(() -> {
			try
			{
				stepMessage.set(labels.getString("progress.reading"));
				progress.bind(readProgress.multiply(0.70));
				RETAAnalysis.getInstance().parse(readProgress);
				progress.unbind();
				progress.set(0.70);
				stepMessage.set(labels.getString("progress.analysing"));
				RETAAnalysis.getInstance().analyse();
				progress.set(0.80);
				stepMessage.set(labels.getString("progress.writing"));
				RETAAnalysis.getInstance().writeExcel(root.getScene().getWindow());
				stepMessage.set(labels.getString("progress.complete"));
			}
			catch (final Exception e)
			{
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
				stepMessage.set(labels.getString("progress.error") + e.getLocalizedMessage());
			}
			finally
			{
				progress.unbind();
				progress.set(1.0);
			}
		}).start();
	}

	@FXML
	protected void selectOutputFile(final ActionEvent e)
	{
		final Path currentFile = Paths.get(bufferedOutput.get());
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter(labels.getString("excel.file.desc"), "*.xlsx"));
		fileChooser.setTitle(labels.getString("output.title"));
		if (currentFile != null)
		{
			fileChooser.setInitialDirectory(currentFile.toFile().getParentFile());
			fileChooser.setInitialFileName(currentFile.getFileName().toString());
		}
		final File file = fileChooser.showSaveDialog(root.getScene().getWindow());

		if (file != null)
		{
			bufferedOutput.set(file.getPath());
		}
	}

	@FXML
	protected void open(final ActionEvent e)
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter(labels.getString("reta.file.desc"), "*.reta"));
		fileChooser.setTitle(labels.getString("open.title"));
		final File file = fileChooser.showOpenDialog(root.getScene().getWindow());
		open(file);
	}

	public void open(final File file)
	{
		if (file != null && file.isFile())
		{
			RETAAnalysis.getInstance().configure(file);
			rebuild();
		}
	}
}
