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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.util.Pair;

import com.google.common.io.Files;

import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.beans.property.buffering.SimpleObjectPropertyBuffering;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.control.MessageDialog;
import com.ben12.reta.view.validation.ValidationDecorator;

/**
 * Main configuration controller.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class MainConfigurationController implements Initializable
{
	/** Class logger. */
	private static final Logger												LOGGER				= Logger
			.getLogger(MainConfigurationController.class.getName());

	/** The {@link BufferingManager} instance. */
	private final BufferingManager											bufferingManager	= new BufferingManager();

	/** Output file buffered property. */
	private final SimpleObjectPropertyBuffering<String>						bufferedOutput;

	/** Requirement source list. */
	private final ObservableList<InputRequirementSource>					sources				= FXCollections
			.observableArrayList();

	/** Buffered requirement source list. */
	private final ObservableList<InputRequirementSource>					bufferedSources;

	/** Requirement source name property list. */
	private final ObservableList<ObjectProperty<String>>					sourcesName			= FXCollections
			.observableArrayList();

	/** Buffered requirement source name property list. */
	private final ObservableList<ObjectProperty<String>>					bufferedSourcesName;

	/** Callback list used when a source name is changed. */
	private final ObservableList<Callback<InputRequirementSource, Void>>	nameChangeCallBacks	= FXCollections
			.observableArrayList();

	/** Callback used to call all {@link #nameChangeCallBacks}. */
	private final Callback<InputRequirementSource, Void>					refreshAll;

	/** Translations {@link ResourceBundle}. */
	private ResourceBundle													labels				= null;

	/** Requirement source {@link TitledPane} list. */
	private List<TitledPane>												panes				= new ArrayList<>();

	/** Root pane. */
	@FXML
	private Parent															root;

	/** Requirement source {@link Accordion}. */
	@FXML
	private Accordion														sourceConfigurations;

	/** Save configuration button. */
	@FXML
	private Button															save;

	/** Cancel modifications button. */
	@FXML
	private Button															cancel;

	/** Run analysis button. */
	@FXML
	private Button															run;

	/** Delete selected requirement source button. */
	@FXML
	private Button															delete;

	/** Up the selected requirement source button. */
	@FXML
	private Button															upSource;

	/** Down the selected requirement source button. */
	@FXML
	private Button															downSource;

	/** Output file {@link TextField}. */
	@FXML
	private ValidationDecorator<TextField>									outputFile;

	/**
	 * Constructor.
	 */
	public MainConfigurationController()
	{
		refreshAll = (final InputRequirementSource e) -> {
			nameChangeCallBacks.stream().forEach(c -> c.call(e));
			return null;
		};

		bufferedSources = new ObservableListBuffering<>(RETAAnalysis.class, RETAAnalysis.REQUIREMENT_SOURCES, sources);
		bufferingManager.add((ObservableListBuffering<InputRequirementSource>) bufferedSources);

		bufferedSourcesName = bufferingManager.buffering(sourcesName);
		bufferedOutput = bufferingManager.buffering(RETAAnalysis.getInstance().outputProperty());
	}

	/**
	 * Re-build the view current loaded configuration.
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
			sources.addAll(RETAAnalysis.getInstance().requirementSourcesProperty());

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

			delete.disableProperty()
					.bind(sourceConfigurations.expandedPaneProperty()
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

			upSource.disableProperty()
					.bind(sourceConfigurations.expandedPaneProperty().isNull().or(indexOfExpendedPane.lessThan(1)));
			downSource.disableProperty()
					.bind(sourceConfigurations.expandedPaneProperty()
							.isNull()
							.or(indexOfExpendedPane
									.greaterThan(Bindings.size(sourceConfigurations.getPanes()).subtract(2))));

			save.disableProperty().bind(Bindings.not(bufferingManager.validProperty()));
			cancel.disableProperty().bind(Bindings.not(bufferingManager.bufferingProperty()));
			run.disableProperty()
					.bind(bufferingManager.bufferingProperty().or(Bindings.not(bufferingManager.validProperty())));
		}
		catch (final Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
	}

	/**
	 * Add the requirement source in the view.
	 * 
	 * @param requirementSource
	 *            requirement source to add
	 * @return the requirement source name property
	 * @throws IOException
	 *             I/O exception
	 * @throws NoSuchMethodException
	 *             No such method exception
	 */
	private ObjectProperty<String> addSource(final InputRequirementSource requirementSource)
			throws IOException, NoSuchMethodException
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

	/**
	 * Action event to add a new requirement source.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 * @throws IOException
	 *             I/O exception
	 * @throws NoSuchMethodException
	 *             No such method exception
	 */
	@FXML
	protected void newSource(final ActionEvent event) throws NoSuchMethodException, IOException
	{
		final List<SourceProviderPlugin> plugins = new ArrayList<>(RETAAnalysis.getInstance().getPluginList());
		final List<Pair<SourceProviderPlugin, String>> pluginChoices = plugins.stream()
				.map((p) -> new Pair<>(p, p.getSourceName()))
				.collect(Collectors.toList());

		final SourceProviderPlugin pluginSelected = MessageDialog.showOptionsMessage(root.getScene().getWindow(),
				labels.getString("new.input.type"), pluginChoices);

		if (pluginSelected != null)
		{
			final InputRequirementSource requirementSource = new InputRequirementSource("NEW", pluginSelected,
					pluginSelected.createNewSourceConfiguration());

			bufferedSourcesName.add(addSource(requirementSource));

			bufferedSources.add(requirementSource);
			refreshAll.call(requirementSource);
		}
	}

	/**
	 * Action event to remove the selected requirement source.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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

	/**
	 * Remove a requirement source.
	 * 
	 * @param index
	 *            the requirement source index
	 * @return the requirement source removed
	 */
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
		final SourceConfigurationController controller = (SourceConfigurationController) pane.getProperties()
				.get("reta.controller");
		controller.disconnect();
		return requirementSource;
	}

	/**
	 * Action event to move up the selected requirement source.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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

	/**
	 * Action event to move down the selected requirement source.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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

	/**
	 * Action event to save the configuration.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
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

			retaAnalysis.requirementSourcesProperty().clear();
			for (final InputRequirementSource requirementSource : bufferedSources)
			{
				retaAnalysis.requirementSourcesProperty().add(requirementSource);
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

	/**
	 * Action event to cancel the buffered modifications.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void cancel(final ActionEvent event)
	{
		final List<TitledPane> toDisconnected = new ArrayList<>(sourceConfigurations.getPanes());
		toDisconnected.removeAll(panes);

		final List<TitledPane> toReconnect = new ArrayList<>(panes);
		toReconnect.removeAll(sourceConfigurations.getPanes());

		// reconnect before revert (reverts disconnected properties)
		toReconnect.stream()
				.forEach(p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).reconnect());

		bufferingManager.revert();

		// disconnect after revert (reverts to be disconnected properties)
		toDisconnected.stream()
				.forEach(p -> ((SourceConfigurationController) p.getProperties().get("reta.controller")).disconnect());

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

	/**
	 * Action event to run the requirements analysis.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void run(final ActionEvent event)
	{
		final var task = new Task<Void>()
		{
			@Override
			protected Void call() throws Exception
			{
				try
				{
					updateProgress(0.00, 1.0);
					updateMessage(labels.getString("progress.reading"));

					RETAAnalysis.getInstance().parse(p -> updateProgress(p * 0.60, 1.0));
					updateProgress(0.60, 1.0);
					updateMessage(labels.getString("progress.analysing"));

					RETAAnalysis.getInstance().analyse();
					updateProgress(0.70, 1.0);
					updateMessage(labels.getString("progress.writing"));

					RETAAnalysis.getInstance().writeExcel();
					updateProgress(1.0, 1.0);
					updateMessage(labels.getString("progress.complete"));
				}
				catch (final Exception e)
				{
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
					updateMessage(labels.getString("progress.error") + e.getLocalizedMessage());
				}
				finally
				{
					updateProgress(1.0, 1.0);
				}

				return null;
			}
		};

		MessageDialog.showProgressBar(root.getScene().getWindow(), labels.getString("progress.title"), task);

		new Thread(task).start();
	}

	/**
	 * Action event to select the output file.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void selectOutputFile(final ActionEvent event)
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

	/**
	 * Action event to open a new configuration file.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void open(final ActionEvent event)
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter(labels.getString("reta.file.desc"), "*.reta"));
		fileChooser.setTitle(labels.getString("open.title"));
		final File file = fileChooser.showOpenDialog(root.getScene().getWindow());
		open(file);
	}

	/**
	 * Open a configuration file.
	 * 
	 * @param file
	 *            the configuration file to open
	 */
	public void open(final File file)
	{
		if (file != null && file.isFile())
		{
			RETAAnalysis.getInstance().configure(file);
			rebuild();
		}
	}

	/**
	 * Action event to open an URL.
	 * 
	 * @param url
	 *            the URL to open
	 */
	protected void openURL(final String url)
	{
		try
		{
			Desktop.getDesktop().browse(URI.create(url));
		}
		catch (final IOException e)
		{
			LOGGER.log(Level.SEVERE, "Cannot open URL \"" + url + "\".", e);
		}
	}

	/**
	 * Action event to open the support URL.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void openSupport(final ActionEvent event)
	{
		openURL("http://stackoverflow.com/questions/ask?tags=reta");
	}

	/**
	 * Action event to open the support URL.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void openWiki(final ActionEvent event)
	{
		openURL("http://github.com/ben12/reta/wiki");
	}

	/**
	 * Action event to open the support URL.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 */
	@FXML
	protected void openProject(final ActionEvent event)
	{
		openURL("http://github.com/ben12/reta");
	}
}
