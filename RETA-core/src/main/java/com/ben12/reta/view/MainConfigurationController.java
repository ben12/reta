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

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.Pair;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.w3c.dom.Element;

import com.google.common.base.Objects;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import com.ben12.reta.beans.property.buffering.BufferingManager;
import com.ben12.reta.beans.property.buffering.ObservableListBuffering;
import com.ben12.reta.model.GraphData;
import com.ben12.reta.model.GraphData.Link;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.RequirementImpl;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.util.DOMUtils;
import com.ben12.reta.util.RETAAnalysis;
import com.ben12.reta.view.control.MessageDialog;
import com.ben12.reta.view.validation.ValidationDecorator;

/**
 * Main configuration controller.
 * 
 * @author Beno�t Moreau (ben.12)
 */
public class MainConfigurationController implements Initializable
{
	/** Class logger. */
	private static final Logger												LOGGER				= Logger
			.getLogger(MainConfigurationController.class.getName());

	/** The {@link BufferingManager} instance. */
	private final BufferingManager											bufferingManager	= new BufferingManager();

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

	/** Last export file. */
	private File															lastExport			= null;

	/** Graph data. */
	private GraphData														graph;

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

	/** Run analysis button. */
	@FXML
	private Button															export;

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

	/** TabPane result. */
	@FXML
	private TabPane															resultTabs;

	/** Graph result. */
	@FXML
	private WebView															webview;

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

			sourcesName.clear();
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
			run.disableProperty().bind(Bindings.not(bufferingManager.validProperty()));
			export.setDisable(true);

			webview.getEngine().getLoadWorker().stateProperty().subscribe(state -> {
				if (state == Worker.State.SUCCEEDED)
				{
					this.customizePlantuml();
				}
			});
		}
		catch (final Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
		}
	}

	private void customizePlantuml()
	{
		final var document = webview.getEngine().getDocument();
		final var groups = DOMUtils.getElementsByTagNameAndAttribute(document, "g", "data-entity");
		for (final var g : groups)
		{
			final var sourceAlias = g.getAttribute("data-entity");
			final var source = graph.getSourceEntities().get(sourceAlias);
			final var texts = DOMUtils.getElementsByTagName(g, "text");

			if (!texts.isEmpty() && Objects.equal(source.getName(), texts.getFirst().getTextContent()))
			{
				final var textEl = texts.get(0);
				final var title = document.createElementNS(textEl.getNamespaceURI(), "title");
				title.setTextContent(source.getConfiguration().getDescription());
				textEl.appendChild(title);
			}

			for (final Element textEl : texts.subList(1, texts.size()))
			{
				final var reqId = textEl.getTextContent();
				final var req = source.getRequirements()
						.stream()
						.filter(r -> Objects.equal(r.getId(), reqId))
						.findFirst();
				if (req.isPresent())
				{
					final var title = document.createElementNS(textEl.getNamespaceURI(), "title");
					title.setTextContent(req.get().getText());
					textEl.appendChild(title);
					if (req.get().getReferredBySource().isEmpty() && !source.getCoversBy().isEmpty())
					{
						textEl.setAttribute("style", "fill: red;");
					}
					else if (!req.get().getReferredBySource().isEmpty()
							&& req.get().getReferredBySource().size() < source.getCoversBy().size())
					{
						textEl.setAttribute("style", "fill: coral;");
					}
				}
			}
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

	protected void commit()
	{
		final RETAAnalysis retaAnalysis = RETAAnalysis.getInstance();

		bufferingManager.commit();
		panes = new ArrayList<>(sourceConfigurations.getPanes());

		retaAnalysis.requirementSourcesProperty().clear();
		for (final InputRequirementSource requirementSource : bufferedSources)
		{
			retaAnalysis.requirementSourcesProperty().add(requirementSource);
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
		if (this.save(event) && bufferingManager.isValid())
		{
			final var task = new Task<Void>()
			{
				private void updateProgress(final double p)
				{
					updateProgress(p, 1.0);
				}

				@Override
				protected Void call() throws Exception
				{
					try
					{
						updateProgress(0.00);

						updateMessage(labels.getString("progress.reading"));
						RETAAnalysis.getInstance().parse(p -> updateProgress(p * 0.50));
						updateProgress(0.50);

						updateMessage(labels.getString("progress.analysing"));
						RETAAnalysis.getInstance().analyse(p -> updateProgress(0.5 + (p * 0.10)));
						updateProgress(0.60);

						updateMessage(labels.getString("progress.graph"));
						graph = buildGraph();
						updateProgress(1.0);

						updateMessage(labels.getString("progress.complete"));
					}
					catch (final Exception e)
					{
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Analysis error", e);
						updateMessage(labels.getString("progress.error") + e.getLocalizedMessage());
					}
					finally
					{
						updateProgress(1.0);
					}

					return null;
				}

				@Override
				protected void succeeded()
				{
					webview.getEngine().loadContent(graph.getHtml());
					buildTabs();
					export.setDisable(false);
				}

				@Override
				protected void failed()
				{
					if (graph != null)
					{
						webview.getEngine().loadContent(graph.getHtml());
						buildTabs();
					}
				}
			};

			MessageDialog.showProgressBar(root.getScene().getWindow(), labels.getString("progress.title"), task);

			graph = null;
			webview.getEngine().loadContent("");
			resultTabs.getTabs().remove(1, resultTabs.getTabs().size());
			export.setDisable(true);

			new Thread(task).start();
		}
	}

	private GraphData buildGraph()
	{
		final Map<String, InputRequirementSource> sourceEntities = new HashMap<>();
		final Map<String, Link> links = new HashMap<>();
		final var graphLines = new ArrayList<String>();

		graphLines.add("@startuml");
		graphLines.add("skinparam classAttributeIconSize 0");

		final var analysis = RETAAnalysis.getInstance();

		final var index = new AtomicInteger(0);
		final var allSources = analysis.requirementSourcesProperty();
		final var isources = allSources.stream()
				.collect(Collectors.toMap(s -> s, s -> index.getAndIncrement(), (a, b) -> a));

		for (final var source : allSources)
		{
			final var i = isources.get(source);
			graphLines.add("object \"**" + source.getName() + "**\" as S" + i + " {");
			final var reqs = source.getRequirements();
			for (final var req : reqs)
			{
				graphLines.add("{field} " + req.getId().replace("\\", "<U+200C>\\<U+200C>"));
			}
			graphLines.add("}");

			sourceEntities.put("S" + i, source);
		}

		for (final var source : allSources)
		{
			final var i = isources.get(source);
			final var reqs = source.getRequirements();
			for (final var req : reqs)
			{
				final var reqId = req.getId().replace("\\", "<U+200C>\\<U+200C>");
				final var refs = req.getReferences();
				for (final var ref : refs)
				{
					final var refSource = ref.getSource();
					if (refSource != null)
					{
						final var refSo = isources.get(refSource);
						final var refId = ref.getId().replace("\\", "<U+200C>\\<U+200C>");
						graphLines.add("\"S" + refSo + "::" + refId + "\" <--- \"S" + i + "::" + reqId + "\"");
					}

					final String dataSourceLine = "" + graphLines.size();
					links.put(dataSourceLine, new Link(dataSourceLine, source, req, refSource, ref));
				}
			}
		}

		graphLines.add("@enduml");

		final var puGraph = String.join("\n", graphLines);
		final var reader = new SourceStringReader(puGraph);
		final var output = new ByteArrayOutputStream();
		try
		{
			reader.outputImage(output, new FileFormatOption(FileFormat.SVG));
		}
		catch (final IOException e)
		{
			LOGGER.log(Level.SEVERE, "Error during Graph generation", e);
		}

		final var svg = new String(output.toByteArray(), StandardCharsets.UTF_8);
		final var html = """
				    <head>
					  <style>
					    body {
					      display: flex;
					      justify-content: center;
					      align-items: center;
					      padding: 4px;
					      min-width: fit-content;
					      min-height: fit-content;
						}
					  </style>
				    </head>
				    <body>
				""" + svg + """
				    </body>
				""";
		return new GraphData(html, sourceEntities, links);
	}

	private void buildTabs()
	{
		final var analysis = RETAAnalysis.getInstance();
		final var allSources = analysis.requirementSourcesProperty();
		for (final var source : allSources)
		{
			final var tab = createSourceTab(source);
			resultTabs.getTabs().add(tab);
		}

		final var tab = createErrorsTab(allSources);
		resultTabs.getTabs().add(tab);
	}

	private Tab createErrorsTab(final List<InputRequirementSource> allSources)
	{
		final var tab = new Tab(labels.getString("errors"));
		tab.setClosable(false);

		final var table = new GridPane();
		table.getStyleClass().add("result-table");
		table.setPadding(new Insets(8));

		final var sourceHeader = new Label(labels.getString("unknownfromsrc"));
		sourceHeader.getStyleClass().addAll("header", "first-row", "first-col");
		final var reqHeader = new Label(labels.getString("unknownfromreq"));
		reqHeader.getStyleClass().addAll("header", "first-row");
		final var refHeader = new Label(labels.getString("unknownreference"));
		refHeader.getStyleClass().addAll("header", "first-row", "last-col");
		table.addRow(0, sourceHeader, reqHeader, refHeader);

		final var total = allSources.stream().mapToInt(s -> s.getAllUknownReferences().size()).sum();

		for (final var source : allSources)
		{
			final var reqs = source.getRequirements();
			final var count = source.getAllUknownReferences().size();
			if (count > 0)
			{
				var row = table.getRowCount();
				final var sourceCell = new Label(source.getName());
				sourceCell.getStyleClass().addAll("first-col");
				table.add(sourceCell, 0, row, 1, count);
				for (final var req : reqs)
				{
					final var refs = req.getReferencesFor(null);
					if (!refs.isEmpty())
					{
						final var reqCell = new Label(req.getText());
						table.add(reqCell, 1, row, 1, refs.size());
						for (final var ref : refs)
						{
							final var refCell = new Label(ref.getText());
							refCell.getStyleClass().addAll("last-col");
							table.add(refCell, 2, row);
							if (row == total)
							{
								sourceCell.getStyleClass().addAll("last-row");
								reqCell.getStyleClass().addAll("last-row");
								refCell.getStyleClass().addAll("last-row");
							}
							row++;
						}
					}
				}
			}
		}

		tab.setContent(new ScrollPane(table));
		return tab;
	}

	@FXML
	protected void export(final ActionEvent event)
	{
		try
		{
			final FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().add(new ExtensionFilter(labels.getString("excel.file.desc"), "*.xlsx"));
			fileChooser.setTitle(labels.getString("output.title"));
			if (lastExport != null)
			{
				fileChooser.setInitialDirectory(lastExport.getParentFile());
				fileChooser.setInitialFileName(lastExport.getName());
			}
			else
			{
				fileChooser.setInitialDirectory(RETAAnalysis.getInstance().getConfig().getParentFile());
			}

			final File file = fileChooser.showSaveDialog(root.getScene().getWindow());
			if (file != null)
			{
				RETAAnalysis.getInstance().writeExcel(file);
				lastExport = file;
			}
		}
		catch (InvalidFormatException | IOException e)
		{
			LOGGER.log(Level.SEVERE, "Exporting excel", e);
		}
	}

	private Tab createSourceTab(final InputRequirementSource source)
	{
		final var tab = new Tab(source.getName());
		tab.setClosable(false);

		final var table = new GridPane();
		table.getStyleClass().add("result-table");
		table.setPadding(new Insets(8));

		final var reqHeader = new Label(labels.getString("requirement"));
		reqHeader.getStyleClass().addAll("header", "first-row", "first-col");
		final var sourceHeader = new Label(labels.getString("source"));
		sourceHeader.getStyleClass().addAll("header", "first-row");
		final var refHeader = new Label(labels.getString("reference"));
		refHeader.getStyleClass().addAll("header", "first-row", "last-col");

		table.addRow(0, reqHeader, sourceHeader, refHeader);

		final var requirements = source.getRequirements();
		final var refSources = source.getCoversBy().keySet();
		var index = 0;
		for (final var requirement : requirements)
		{
			final var reqCell = new Label(requirement.getText());
			reqCell.getStyleClass().add("first-col");
			setReqStyle(source, requirement, reqCell);

			final List<Label> sourceCells = new ArrayList<>();
			final List<List<Label>> refCells = new ArrayList<>();
			for (final var refSource : refSources)
			{
				final var sourceCell = new Label(refSource.getName());
				sourceCells.add(sourceCell);
				final List<Label> cells = new ArrayList<>();
				refCells.add(cells);
				final var refs = requirement.getReferredByRequirementFor(refSource);
				for (final var ref : refs)
				{
					cells.add(new Label(ref.getText()));
				}
				if (refs.isEmpty())
				{
					cells.add(new Label());
				}
				cells.getLast().getStyleClass().add("last-col");
			}
			if (refSources.isEmpty())
			{
				sourceCells.add(new Label());
				final List<Label> cells = new ArrayList<>();
				cells.add(new Label());
				refCells.add(cells);
				cells.getLast().getStyleClass().add("last-col");
			}

			var row = table.getRowCount();
			table.add(reqCell, 0, row, 1, refCells.stream().mapToInt(Collection::size).sum());
			for (int i = 0; i < sourceCells.size(); i++)
			{
				final var refs = refCells.get(i);
				table.add(sourceCells.get(i), 1, row, 1, refs.size());
				for (int c = 0; c < refs.size(); c++, row++)
				{
					table.add(refs.get(c), 2, row);
				}
			}

			index++;
			if (index == requirements.size())
			{
				reqCell.getStyleClass().add("last-row");
				sourceCells.getLast().getStyleClass().add("last-row");
				refCells.getLast().getLast().getStyleClass().add("last-row");
			}
		}

		tab.setContent(new ScrollPane(table));
		return tab;
	}

	private void setReqStyle(final InputRequirementSource source, final RequirementImpl requirement,
			final Label reqCell)
	{
		if (requirement.getReferredBySource().isEmpty() && !source.getCoversBy().isEmpty())
		{
			reqCell.getStyleClass().add("uncovered");
		}
		else if (!requirement.getReferredBySource().isEmpty()
				&& requirement.getReferredBySource().size() < source.getCoversBy().size())
		{
			reqCell.getStyleClass().add("partial");
		}
	}

	/**
	 * Action event to create a new configuration file.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 * @return true if successfully created
	 */
	@FXML
	protected boolean create(final ActionEvent event)
	{
		boolean saved = false;
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
			while (bufferedSources.size() > 0)
			{
				removeSource(0);
			}
			this.commit();
			saved = retaAnalysis.saveConfig(file);
			bufferingManager.validate();
		}
		return saved;
	}

	/**
	 * Action event to save current configuration.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 * @return true if successfully saved
	 */
	@FXML
	protected boolean save(final ActionEvent event)
	{
		final boolean saved;
		final RETAAnalysis retaAnalysis = RETAAnalysis.getInstance();
		if (retaAnalysis.getConfig() != null)
		{
			this.commit();
			saved = retaAnalysis.saveConfig(retaAnalysis.getConfig());
			bufferingManager.validate();
		}
		else
		{
			saved = this.saveAs(event);
		}
		return saved;
	}

	/**
	 * Action event to save current configuration.
	 * 
	 * @param event
	 *            the {@link ActionEvent}
	 * @return true if successfully saved
	 */
	@FXML
	protected boolean saveAs(final ActionEvent event)
	{
		boolean saved = false;
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
			this.commit();
			saved = retaAnalysis.saveConfig(file);
			bufferingManager.validate();
		}
		return saved;
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
			lastExport = null;
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

	/**
	 * @return buffering status
	 */
	public ReadOnlyBooleanProperty bufferingProperty()
	{
		return ReadOnlyBooleanProperty.readOnlyBooleanProperty(bufferingManager.bufferingProperty());
	}
}
