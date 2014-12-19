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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.property.DoubleProperty;
import javafx.stage.Window;

import javax.validation.constraints.NotNull;

import net.sf.jett.transform.ExcelTransformer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.ben12.reta.api.RETAParseException;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.beans.constraints.PathExists;
import com.ben12.reta.beans.constraints.PathExists.KindOfPath;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.Requirement;
import com.ben12.reta.plugin.SourceProviderPlugin;
import com.ben12.reta.view.control.MessageDialog;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.Files;

// TODO : think of a better implementation
/**
 * @author Benoît Moreau (ben.12)
 */
public final class RETAAnalysis
{

	private static final Logger							LOGGER				= Logger.getLogger(RETAAnalysis.class.getName());

	public static final String							REQUIREMENT_SOURCES	= "requirementSources";

	private static RETAAnalysis							instance			= null;

	private final Map<String, InputRequirementSource>	requirementSources	= new LinkedHashMap<>();

	private final Map<String, SourceProviderPlugin>		plugins				= new HashMap<>();

	private File										config				= null;

	@NotNull(message = "invalid.path")
	@com.ben12.reta.beans.constraints.Path
	@PathExists(kind = KindOfPath.DIRECTORY, parent = true)
	private String										output				= null;

	// TODO maybe useful to see unknown references and mismatch versions
	// private final Comparator<Requirement> reqCompId = (req1, req2) -> req1.getId().compareTo(req2.getId());

	private RETAAnalysis()
	{
		final ServiceLoader<SourceProviderPlugin> serviceLoader = ServiceLoader.load(SourceProviderPlugin.class);
		serviceLoader.forEach(p -> plugins.put(p.getClass().getName(), p));
	}

	public static RETAAnalysis getInstance()
	{
		if (instance == null)
		{
			instance = new RETAAnalysis();
		}
		return instance;
	}

	/**
	 * @return the requirementSources
	 */
	public Map<String, InputRequirementSource> getRequirementSources()
	{
		return requirementSources;
	}

	/**
	 * @return the config
	 */
	public File getConfig()
	{
		return config;
	}

	/**
	 * @return the output
	 */
	public String getOutput()
	{
		return output;
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(final String output)
	{
		this.output = output;
	}

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

			final String output = ini.get("GENERAL", "output");
			if (output != null)
			{
				final Path outPath = Paths.get(output);
				this.output = outPath.normalize().toString();
				final String fileName = outPath.getFileName().toString();
				if (!"xlsx".equals(Files.getFileExtension(fileName)))
				{
					LOGGER.warning("output extension changed for xlsx");
					final Path parent = outPath.getParent();
					if (parent == null)
					{
						this.output = Files.getNameWithoutExtension(fileName) + ".xlsx";
					}
					else
					{
						this.output = parent.resolve(Files.getNameWithoutExtension(fileName) + ".xlsx").toString();
					}
				}
			}
			else
			{
				this.output = Paths.get(iniFile.getParent(), Files.getNameWithoutExtension(iniFile.getName()) + ".xlsx")
						.toString();
			}

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
						continue;
					}

					final String pluginClass = section.get("parser",
							"com.ben12.reta.plugin.tika.TikaSourceProviderPlugin");
					final SourceProviderPlugin plugin = plugins.get(pluginClass);

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

						requirementSources.put(doc, requirementSource);
					}
					else
					{
						LOGGER.warning("No source defined for input " + doc);
					}
				}

				coversMap.entrySet().stream().forEach(entry -> {
					final InputRequirementSource requirementSource = entry.getKey();
					entry.getValue().stream().forEach(cover -> {
						final InputRequirementSource coverRequirementSource = requirementSources.get(cover);
						if (coverRequirementSource != null)
						{
							requirementSource.getCovers().add(coverRequirementSource);
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

	public void saveConfig()
	{
		saveConfig(config);
	}

	public void saveConfig(final File iniFile)
	{
		final Wini ini = new Wini();
		ini.getConfig().setFileEncoding(Charset.forName("CP1252"));

		try
		{
			final Section generalSection = ini.add("GENERAL");

			generalSection.add("output", output.toString());

			final List<String> inputs = new ArrayList<>();

			for (final InputRequirementSource requirementSource : requirementSources.values())
			{
				final String name = requirementSource.getName();
				inputs.add(name);

				final Section sourceSection = ini.add(name);

				sourceSection.put("plugin", requirementSource.getProvider().getClass().getName());

				requirementSource.getProvider().saveSourceConfiguration(requirementSource.getConfiguration(),
						sourceSection);

				sourceSection.put("covers", requirementSource.getCovers()
						.stream()
						.map(InputRequirementSource::getName)
						.collect(Collectors.joining(",")));
			}

			generalSection.put("inputs", inputs.stream().collect(Collectors.joining(",")));

			ini.store(iniFile);
			config = iniFile;
		}
		catch (final IOException e)
		{
			LOGGER.log(Level.SEVERE, "Invalid config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
	}

	public void parse(final DoubleProperty progress) throws RETAParseException
	{
		final RETAParseException[] ex = { null };
		final AtomicInteger count = new AtomicInteger(0);

		requirementSources.values()
				.parallelStream()
				.forEach(
						requirementSource -> {
							try
							{
								parse(requirementSource, null, -1);
								synchronized (progress)
								{
									progress.set((double) count.incrementAndGet() / requirementSources.size());
								}
							}
							catch (final RETAParseException e)
							{
								ex[0] = new RETAParseException("Parsing source \"" + requirementSource.getName()
										+ "\": " + e.getLocalizedMessage(), e);
							}
						});

		if (ex[0] != null)
		{
			throw ex[0];
		}
	}

	/**
	 * @param requirementSource
	 * @param sourceText
	 * @param limit
	 */
	public void parse(final InputRequirementSource requirementSource, final StringBuilder sourceText, final int limit)
			throws RETAParseException
	{
		requirementSource.clear();

		// TODO
	}

	public void analyse() throws IOException
	{
		for (final InputRequirementSource source : requirementSources.values())
		{
			LOGGER.info("Start analyse " + source.getName());

			final List<InputRequirementSource> covers = source.getCovers();
			final TreeSet<Requirement> reqSource = source.getRequirements();
			for (final InputRequirementSource coverSource : covers)
			{
				final TreeSet<Requirement> reqCover = coverSource.getRequirements();
				final TreeSet<Requirement> reqCoverred = new TreeSet<>(reqCover);
				for (final Requirement req : reqSource)
				{
					final Set<Requirement> realReqCovers = new TreeSet<>();
					final Iterable<Requirement> refs = req.getReferences();
					for (final Requirement reqRef : refs)
					{
						final Requirement found = reqCover.ceiling(reqRef);
						if (found != null && found.equals(reqRef))
						{
							reqCoverred.remove(found);
							realReqCovers.add(found);
							found.addReferredBy(req);
						}
					}
					for (final Requirement realReqCover : realReqCovers)
					{
						req.addReference(realReqCover);
					}
				}
				final double coverage = (double) (reqCover.size() - reqCoverred.size()) / reqCover.size();
				coverSource.getCoversBy().put(source, coverage);
			}

			LOGGER.info("End analyse " + source.getName());
		}
	}

	public void writeExcel(final Window parent) throws IOException, InvalidFormatException
	{
		LOGGER.info("Start write excel output");

		Path outputFile = Paths.get(output);
		if (!outputFile.isAbsolute())
		{
			final Path root = config.getAbsoluteFile().getParentFile().toPath();
			outputFile = root.resolve(outputFile);
		}

		// test using template
		final InputStream is = getClass().getResourceAsStream("/com/ben12/reta/resources/template/template.xlsx");
		final ExcelTransformer transformer = new ExcelTransformer();
		final List<String> sheetNames = new ArrayList<>();
		final List<String> sheetTemplateNames = new ArrayList<>();
		for (final InputRequirementSource requirementSource : requirementSources.values())
		{
			sheetTemplateNames.add("DOCUMENT");
			sheetTemplateNames.add("COVERAGE");
			sheetNames.add(requirementSource.getName());
			sheetNames.add(requirementSource.getName() + " coverage");
		}

		final List<Map<String, Object>> sheetValues = new ArrayList<>();
		for (final InputRequirementSource source : requirementSources.values())
		{
			final Map<String, Object> values = new HashMap<>();
			values.put("source", source);
			values.put("null", null);
			values.put("line", "\n");

			final Set<String> attributes = new LinkedHashSet<>();
			attributes.add(SourceConfiguration.ATTRIBUTE_ID);
			if (source.getAllAttributes().contains(SourceConfiguration.ATTRIBUTE_VERSION))
			{
				attributes.add(SourceConfiguration.ATTRIBUTE_VERSION);
			}
			attributes.addAll(source.getAllAttributes());
			attributes.remove(SourceConfiguration.ATTRIBUTE_TEXT);
			values.put("attributes", attributes);

			final Set<String> refAttributes = new LinkedHashSet<>();
			refAttributes.add(SourceConfiguration.ATTRIBUTE_ID);
			if (source.getAllAttributes().contains(SourceConfiguration.ATTRIBUTE_VERSION))
			{
				refAttributes.add(SourceConfiguration.ATTRIBUTE_VERSION);
			}
			refAttributes.addAll(source.getAllAttributes());
			refAttributes.remove(SourceConfiguration.ATTRIBUTE_TEXT);
			values.put("refAttributes", refAttributes);

			sheetValues.add(values);
			sheetValues.add(values);
		}

		final Workbook wb = transformer.transform(is, sheetTemplateNames, sheetNames, sheetValues);
		final int sheetCount = wb.getNumberOfSheets();
		for (int i = 0; i < sheetCount; i++)
		{
			final Sheet sheet = wb.getSheetAt(i);
			int columns = 0;
			for (int j = 0; j <= sheet.getLastRowNum(); j++)
			{
				final Row row = sheet.getRow(j);
				if (row != null)
				{
					row.setHeight((short) -1);
					columns = Math.max(columns, row.getLastCellNum() + 1);
				}
			}
			for (int j = 0; j < columns; j++)
			{
				sheet.autoSizeColumn(j);
			}
		}

		try (FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
		{
			wb.write(fos);
		}
		catch (final FileNotFoundException e)
		{
			final int confirm = MessageDialog.showQuestionMessage(null, "Excel output file must be closed.");

			if (confirm == MessageDialog.OK_OPTION)
			{
				try (FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
				{
					wb.write(fos);
				}
				catch (final IOException e2)
				{
					throw e2;
				}
			}
			else
			{
				throw e;
			}
		}

		LOGGER.info("End write excel output");
	}

	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		for (final InputRequirementSource reqSource : requirementSources.values())
		{
			builder.append(reqSource.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
