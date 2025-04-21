// Package : com.ben12.reta.util
// File : RETAAnalysis.java
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

package com.ben12.reta.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import com.ben12.reta.api.RETAParseException;
import com.ben12.reta.api.RETAParser;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.export.ExcelExporter;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.RequirementImpl;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.view.control.MessageDialog;

/**
 * @author Benoît Moreau (ben.12)
 */
public final class RETAAnalysis
{
	/** {@link RETAAnalysis} logger. */
	private static final Logger								LOGGER				= Logger
			.getLogger(RETAAnalysis.class.getName());

	/** {@link #requirementSources} property name. */
	public static final String								REQUIREMENT_SOURCES	= "requirementSources";

	/** Singleton instance. */
	private static RETAAnalysis								instance			= null;

	/** Input requirement source list. */
	private final ObservableList<InputRequirementSource>	requirementSources	= FXCollections.observableArrayList();

	/** Available plug-ins. */
	private final Map<String, SourceProviderPlugin>			plugins				= new HashMap<>();

	/** Configuration file opened. */
	private File											config				= null;

	// TODO maybe useful to see unknown references and mismatch versions
	// private final Comparator<Requirement> reqCompId = (req1, req2) -> req1.getId().compareTo(req2.getId());

	/**
	 * Constructor.
	 */
	private RETAAnalysis()
	{
		final ServiceLoader<SourceProviderPlugin> serviceLoader = ServiceLoader.load(SourceProviderPlugin.class);
		serviceLoader.forEach(p -> plugins.put(p.getClass().getName(), p));
	}

	/**
	 * @return the singleton instance.
	 */
	public static RETAAnalysis getInstance()
	{
		if (instance == null)
		{
			instance = new RETAAnalysis();
		}
		return instance;
	}

	/**
	 * @return the plugin list
	 */
	public Collection<SourceProviderPlugin> getPluginList()
	{
		return plugins.values();
	}

	/**
	 * @param pluginClass
	 *            plugin class implementation
	 * @return the plugin
	 */
	public SourceProviderPlugin getPlugin(final String pluginClass)
	{
		return plugins.get(pluginClass);
	}

	/**
	 * @return the requirementSources
	 */
	public ObservableList<InputRequirementSource> requirementSourcesProperty()
	{
		return requirementSources;
	}

	/**
	 * @param sourceName
	 *            input requirement source name
	 * @return the {@link InputRequirementSource} with same name
	 */
	public Optional<InputRequirementSource> getRequirementSource(final String sourceName)
	{
		return requirementSourcesProperty().parallelStream()
				.filter((r) -> Objects.equals(r.getName(), sourceName))
				.findAny();
	}

	/**
	 * @return the config
	 */
	public File getConfig()
	{
		return config;
	}

	/**
	 * @param iniFile
	 *            RETA INI file
	 */
	public void configure(final File iniFile)
	{
		config = iniFile;

		try
		{
			System.setProperty("user.dir", config.getAbsoluteFile().getParentFile().getCanonicalPath());
		}
		catch (final IOException e)
		{
			System.setProperty("user.dir", config.getAbsoluteFile().getParent());
		}

		try
		{
			requirementSources.clear();

			final Wini ini = new Wini();
			ini.getConfig().setFileEncoding(Charset.forName("CP1252"));
			ini.load(iniFile);

			final Map<InputRequirementSource, List<String>> coversMap = new LinkedHashMap<>();

			final String documentsStr = ini.get("GENERAL", "inputs");
			if (!Strings.isNullOrEmpty(documentsStr))
			{
				final Iterable<String> documents = Splitter.on(',')
						.trimResults()
						.omitEmptyStrings()
						.split(documentsStr);

				for (final String doc : documents)
				{
					final Section section = ini.get(doc);
					if (section == null)
					{
						LOGGER.warning("No section defined for input " + doc);
						MessageDialog.showErrorMessage(null, "No description for input named: " + doc);
						continue;
					}

					// Use Tika file source for retro-compatibility (but it may be a folder source)
					final String pluginClass = section.get("plugin",
							"com.ben12.reta.plugin.tika.TikaSourceProviderPlugin");
					final SourceProviderPlugin plugin = plugins.get(pluginClass);
					if (plugin == null)
					{
						// Unsupported plugin
						LOGGER.severe("Unsupported plugin: " + pluginClass);
						MessageDialog.showErrorMessage(null, "Unsupported plugin for " + doc + " : " + pluginClass);
						continue;
					}

					final SourceConfiguration configuration = plugin.loadSourceConfiguration(section);
					if (configuration != null)
					{
						final InputRequirementSource requirementSource = new InputRequirementSource(doc, plugin,
								configuration);

						final String coversStr = section.get("covers", "");
						final List<String> covers = Splitter.on(',')
								.trimResults()
								.omitEmptyStrings()
								.splitToList(coversStr);
						coversMap.put(requirementSource, covers);

						requirementSources.add(requirementSource);
					}
					else
					{
						LOGGER.severe("No source defined for input " + doc);
						MessageDialog.showErrorMessage(null, "Error in source configuration : " + doc);
						continue;
					}
				}

				coversMap.entrySet().stream().forEach(entry -> {
					final InputRequirementSource requirementSource = entry.getKey();
					entry.getValue().stream().forEach(cover -> {
						final Optional<InputRequirementSource> coverRequirementSource = getRequirementSource(cover);
						if (coverRequirementSource.isPresent())
						{
							requirementSource.getCovers().add(coverRequirementSource.get());
						}
						else
						{
							LOGGER.warning(requirementSource.getName() + " covers an unknown input " + cover);
						}
					});
				});
			}
			else
			{
				LOGGER.warning("No inputs defined in GENERAL section.");
			}
		}
		catch (final IOException e)
		{
			LOGGER.log(Level.SEVERE, "Invalid config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
		catch (final Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error reading config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
	}

	/**
	 * @param iniFile
	 *            INI file where save configuration
	 * @return true on success
	 */
	public boolean saveConfig(final File iniFile)
	{
		final Wini ini = new Wini();
		ini.getConfig().setFileEncoding(Charset.forName("CP1252"));

		boolean success = false;
		try
		{
			final Section generalSection = ini.add("GENERAL");

			final List<String> inputs = new ArrayList<>();

			for (final InputRequirementSource requirementSource : requirementSources)
			{
				final String name = requirementSource.getName();
				inputs.add(name);

				final Section sourceSection = ini.add(name);

				sourceSection.put("plugin", requirementSource.getProvider().getClass().getName());

				requirementSource.getProvider()
						.saveSourceConfiguration(requirementSource.getConfiguration(), sourceSection);

				sourceSection.put("covers",
						requirementSource.getCovers()
								.stream()
								.map(InputRequirementSource::getName)
								.collect(Collectors.joining(",")));
			}

			generalSection.put("inputs", inputs.stream().collect(Collectors.joining(",")));

			ini.store(iniFile);
			config = iniFile;

			try
			{
				System.setProperty("user.dir", config.getAbsoluteFile().getParentFile().getCanonicalPath());
			}
			catch (final IOException e)
			{
				System.setProperty("user.dir", config.getAbsoluteFile().getParent());
			}

			success = true;
		}
		catch (final IOException e)
		{
			LOGGER.log(Level.SEVERE, "Invalid config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
		return success;
	}

	/**
	 * Parallel parsing of input requirement sources.
	 * 
	 * @param progress
	 *            progression in percent
	 * @throws RETAParseException
	 *             RETA Parser exception
	 */
	public void parse(final Consumer<Double> progress) throws RETAParseException
	{
		final RETAParseException[] ex = { null };
		final AtomicInteger count = new AtomicInteger(0);

		requirementSources.parallelStream().forEach(requirementSource -> {
			try
			{
				requirementSource.clear();
				final RETAParser parser = requirementSource.getProvider()
						.createParser(requirementSource.getConfiguration());
				parser.parseSource(requirementSource);
				synchronized (progress)
				{
					progress.accept((double) count.incrementAndGet() / requirementSources.size());
				}
			}
			catch (final RETAParseException e)
			{
				ex[0] = new RETAParseException(
						"Parsing source \"" + requirementSource.getName() + "\": " + e.getLocalizedMessage(), e);
			}
		});

		if (ex[0] != null)
		{
			throw ex[0];
		}
	}

	/**
	 * @param requirementSource
	 *            input requirement source
	 * @param sourceText
	 *            source content parsed
	 * @param limit
	 *            source limit size to parse
	 * @throws RETAParseException
	 *             Parsing exception
	 */
	public void parse(final InputRequirementSource requirementSource, final StringBuilder sourceText, final int limit)
			throws RETAParseException
	{
		requirementSource.clear();
		final RETAParser parser = requirementSource.getProvider().createParser(requirementSource.getConfiguration());
		parser.parseSourcePreview(requirementSource, sourceText, limit);
	}

	/**
	 * Analyze the parsing result.
	 * Search requirement references in requirements.
	 * 
	 * @param progress
	 *            progression in percent
	 */
	public void analyse(final Consumer<Double> progress)
	{
		final var counter = new AtomicInteger(0);
		requirementSources.parallelStream().forEach(source -> {
			LOGGER.info("Start analyse " + source.getName());

			final List<InputRequirementSource> covers = source.getCovers();
			final TreeSet<RequirementImpl> reqSource = source.getRequirements();
			for (final InputRequirementSource coverSource : covers)
			{
				final TreeSet<RequirementImpl> reqToCover = coverSource.getRequirements();
				final TreeSet<RequirementImpl> reqNotCoverred = new TreeSet<>(reqToCover);
				for (final RequirementImpl req : reqSource)
				{
					final Set<RequirementImpl> realReqCovers = new TreeSet<>();
					final Iterable<RequirementImpl> refs = req.getReferences();
					for (final RequirementImpl reqRef : refs)
					{
						final RequirementImpl found = reqToCover.ceiling(reqRef);
						if (found != null && found.equals(reqRef))
						{
							reqNotCoverred.remove(found);
							realReqCovers.add(found);
							synchronized (RETAAnalysis.this)
							{
								found.addReferredBy(req);
							}
						}
					}
					for (final RequirementImpl realReqCover : realReqCovers)
					{
						synchronized (RETAAnalysis.this)
						{
							req.addReference(realReqCover);
						}
					}
				}
				final double coverage = (double) (reqToCover.size() - reqNotCoverred.size()) / reqToCover.size();
				synchronized (RETAAnalysis.this)
				{
					coverSource.getCoversBy().put(source, coverage);
				}
			}

			progress.accept((double) counter.incrementAndGet() / requirementSources.size());

			LOGGER.info("End analyse " + source.getName());
		});
	}

	/**
	 * Write Excel file result of requirement traceability analysis.
	 * 
	 * @param outputFile
	 *            output file
	 * @throws IOException
	 *             I/O exception
	 * @throws InvalidFormatException
	 *             Invalid Excel format exception
	 */
	public void writeExcel(final File outputFile) throws IOException, InvalidFormatException
	{
		LOGGER.info("Start write excel output");

		final var exporter = new ExcelExporter();
		final var wb = exporter.export(requirementSources);
		writeExcel(wb, outputFile);

		LOGGER.info("End write excel output");
	}

	/**
	 * @param workbook
	 *            {@link Workbook} to write
	 * @param outputFile
	 *            output file path
	 * @throws IOException
	 *             I/O exception
	 */
	public void writeExcel(final Workbook workbook, final File outputFile) throws IOException
	{
		try (FileOutputStream fos = new FileOutputStream(outputFile))
		{
			workbook.write(fos);
		}
		catch (final FileNotFoundException e)
		{
			if (MessageDialog.showQuestionMessage(null, "Excel output file must be closed."))
			{
				writeExcel(workbook, outputFile);
			}
			else
			{
				throw e;
			}
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		for (final InputRequirementSource reqSource : requirementSources)
		{
			builder.append(reqSource.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
