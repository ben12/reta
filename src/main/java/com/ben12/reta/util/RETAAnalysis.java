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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import com.ben12.reta.constraints.PathExists;
import com.ben12.reta.constraints.PathExists.KindOfPath;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.Requirement;
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
	/** Buffer size (2 Mo), this is also the maximum requirement text length. */
	private static final int							BUFFER_SIZE			= 2 * 1024 * 1024;

	public static final String							REQUIREMENT_SOURCES	= "requirementSources";

	private static RETAAnalysis							instance			= null;

	private final Logger								logger				= Logger.getLogger(RETAAnalysis.class.getName());

	private final Map<String, InputRequirementSource>	requirementSources	= new LinkedHashMap<>();

	private File										config				= null;

	@NotNull
	@PathExists(kind = KindOfPath.DIRECTORY, parent = true)
	private Path										output				= null;

	// TODO maybe useful to see unknown references and mismatch versions
	// private final Comparator<Requirement> reqCompId = (req1, req2) -> req1.getId().compareTo(req2.getId());

	private RETAAnalysis()
	{
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
	public Path getOutput()
	{
		return output;
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(Path output)
	{
		this.output = output;
	}

	public void configure(File iniFile)
	{
		config = iniFile;

		try
		{
			requirementSources.clear();

			Wini ini = new Wini();
			ini.getConfig().setFileEncoding(Charset.forName("CP1252"));
			ini.load(iniFile);

			String output = ini.get("GENERAL", "output");
			if (output != null)
			{
				this.output = Paths.get(output);
				String fileName = this.output.getFileName().toString();
				if (!"xlsx".equals(Files.getFileExtension(fileName)))
				{
					logger.warning("output extension changed for xlsx");
					Path parent = this.output.getParent();
					if (parent == null)
					{
						this.output = Paths.get(Files.getNameWithoutExtension(fileName) + ".xlsx");
					}
					else
					{
						this.output = parent.resolve(Files.getNameWithoutExtension(fileName) + ".xlsx");
					}
				}
			}
			else
			{
				this.output = Paths.get(iniFile.getParent(), Files.getNameWithoutExtension(iniFile.getName()) + ".xlsx");
			}

			Map<InputRequirementSource, List<String>> coversMap = new LinkedHashMap<>();

			String attributesStr = ini.get("GENERAL", "requirement.attributes");
			Iterable<String> attributes = Collections.emptyList();
			if (!Strings.isNullOrEmpty(attributesStr))
			{
				attributes = Splitter.on(',').trimResults().omitEmptyStrings().split(attributesStr);
			}

			String refAttributesStr = ini.get("GENERAL", "references.attributes");
			Iterable<String> refAttributes = Collections.emptyList();
			if (!Strings.isNullOrEmpty(refAttributesStr))
			{
				refAttributes = Splitter.on(',').trimResults().omitEmptyStrings().split(refAttributesStr);
			}

			String documentsStr = ini.get("GENERAL", "inputs");
			if (!Strings.isNullOrEmpty(documentsStr))
			{
				Iterable<String> documents = Splitter.on(',').trimResults().omitEmptyStrings().split(documentsStr);

				for (String doc : documents)
				{
					Section section = ini.get(doc);
					if (section == null)
					{
						logger.warning("No section defined for input " + doc);
						continue;
					}
					String source = section.get("path", "");
					String filter = section.get("filter", "");
					if (!source.isEmpty())
					{
						InputRequirementSource requirementSource = new InputRequirementSource(doc, Paths.get(source),
								filter);

						try
						{
							String startRegex = section.get("requirement.start.regex", "");
							String endRegex = section.get("requirement.end.regex", "");
							Integer textIndex = section.get("requirement.start." + Requirement.ATTRIBUTE_TEXT
									+ ".index", Integer.class, 0);
							Integer idIndex = section.get("requirement.start." + Requirement.ATTRIBUTE_ID + ".index",
									Integer.class, null);
							Integer versionIndex = section.get("requirement.start." + Requirement.ATTRIBUTE_VERSION
									+ ".index", Integer.class, null);

							if (startRegex.isEmpty())
							{
								if (!endRegex.isEmpty())
								{
									logger.severe("requirement.start.regex is mandatory for section " + doc);
									continue;
								}
							}
							else if (endRegex.isEmpty())
							{
								logger.severe("requirement.end.regex is mandatory for section " + doc);
								continue;
							}
							else if (idIndex == null)
							{
								logger.severe("requirement.start.Id.index is mandatory for section " + doc);
								continue;
							}

							requirementSource.setReqStart(startRegex);
							requirementSource.setReqEnd(endRegex);
							requirementSource.getAttributesGroup().put(Requirement.ATTRIBUTE_TEXT, textIndex);
							requirementSource.getAttributesGroup().put(Requirement.ATTRIBUTE_ID, idIndex);
							if (versionIndex != null)
							{
								requirementSource.getAttributesGroup().put(Requirement.ATTRIBUTE_VERSION, versionIndex);
							}

							for (String att : attributes)
							{
								Integer attIndex = section.get("requirement.start." + att + ".index", Integer.class,
										null);
								if (attIndex != null)
								{
									requirementSource.getAttributesGroup().put(att, attIndex);
								}
							}

							String coversStr = section.get("covers", "");
							List<String> covers = Splitter.on(',')
									.trimResults()
									.omitEmptyStrings()
									.splitToList(coversStr);
							coversMap.put(requirementSource, covers);

							String refRegex = section.get("requirement.ref.regex");
							if (!Strings.isNullOrEmpty(refRegex))
							{
								Integer refIdIndex = section.get("requirement.ref." + Requirement.ATTRIBUTE_ID
										+ ".index", Integer.class, null);
								Integer refVersionIndex = section.get("requirement.ref."
										+ Requirement.ATTRIBUTE_VERSION + ".index", Integer.class, null);

								if (Strings.isNullOrEmpty(refRegex))
								{
									logger.severe("requirement.ref.regex is mandatory for section " + doc);
									continue;
								}
								else if (refIdIndex == null)
								{
									logger.severe("requirement.ref.Id.index is mandatory for section " + doc);
									continue;
								}

								requirementSource.setReqRef(refRegex);
								requirementSource.getRefAttributesGroup().put(Requirement.ATTRIBUTE_ID, refIdIndex);
								if (refVersionIndex != null)
								{
									requirementSource.getRefAttributesGroup().put(Requirement.ATTRIBUTE_VERSION,
											refVersionIndex);
								}

								for (String att : refAttributes)
								{
									Integer attIndex = section.get("requirement.ref." + att + ".index", Integer.class,
											null);
									if (attIndex != null)
									{
										requirementSource.getRefAttributesGroup().put(att, attIndex);
									}
								}
							}
							else if (!covers.isEmpty())
							{
								logger.warning("requirement.ref.regex is mandatory for section " + doc
										+ " with covers defined.");
							}

							requirementSources.put(doc, requirementSource);
						}
						catch (IllegalArgumentException e)
						{
							logger.log(Level.SEVERE, "Invalid value for section " + doc, e);
						}
					}
					else
					{
						logger.warning("No source defined for input " + doc);
					}
				}

				coversMap.entrySet().stream().forEach(entry -> {
					InputRequirementSource requirementSource = entry.getKey();
					entry.getValue().stream().forEach(cover -> {
						InputRequirementSource coverRequirementSource = requirementSources.get(cover);
						if (coverRequirementSource != null)
						{
							requirementSource.getCovers().add(coverRequirementSource);
							coverRequirementSource.getCoversBy().put(requirementSource, 0.0);
						}
						else
						{
							logger.warning(requirementSource.getName() + " covers an unknown input " + cover);
						}
					});
				});
			}
			else
			{
				logger.warning("No inputs defined in GENERAL section.");
			}
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, "Invalid config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, "Error reading config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
	}

	public void saveConfig()
	{
		saveConfig(config);
	}

	public void saveConfig(File iniFile)
	{
		Wini ini = new Wini();
		ini.getConfig().setFileEncoding(Charset.forName("CP1252"));

		try
		{
			Section generalSection = ini.add("GENERAL");

			generalSection.add("output", output.toString());

			List<String> inputs = new ArrayList<>();
			Set<String> reqAttributes = new HashSet<>();
			Set<String> refAttributes = new HashSet<>();

			for (InputRequirementSource requirementSource : requirementSources.values())
			{
				String name = requirementSource.getName();
				inputs.add(name);

				Section sourceSection = ini.add(name);

				sourceSection.put("path", requirementSource.getSourcePath());
				sourceSection.put("filter", requirementSource.getFilter());

				String reqStart = requirementSource.getReqStart();
				if (!Strings.isNullOrEmpty(reqStart))
				{
					Map<String, Integer> reqAttributesGroup = requirementSource.getAttributesGroup();
					reqAttributes.addAll(reqAttributesGroup.keySet());

					sourceSection.put("requirement.start.regex", reqStart);

					for (Entry<String, Integer> reqAttributeGroup : reqAttributesGroup.entrySet())
					{
						if (reqAttributeGroup.getValue() != null)
						{
							sourceSection.put("requirement.start." + reqAttributeGroup.getKey() + ".index",
									reqAttributeGroup.getValue());
						}
					}
					sourceSection.put("requirement.end.regex", requirementSource.getReqEnd());
				}

				String reqRef = requirementSource.getReqRef();
				if (!Strings.isNullOrEmpty(reqRef))
				{
					Map<String, Integer> refAttributesGroup = requirementSource.getRefAttributesGroup();
					refAttributes.addAll(refAttributesGroup.keySet());

					sourceSection.put("requirement.ref.regex", reqRef);

					for (Entry<String, Integer> refAttributeGroup : refAttributesGroup.entrySet())
					{
						if (refAttributeGroup.getValue() != null)
						{
							sourceSection.put("requirement.ref." + refAttributeGroup.getKey() + ".index",
									refAttributeGroup.getValue());
						}
					}

					sourceSection.put(
							"covers",
							requirementSource.getCovers()
									.stream()
									.map(InputRequirementSource::getName)
									.collect(Collectors.joining(",")));
				}
			}

			generalSection.put("inputs", inputs.stream().collect(Collectors.joining(",")));

			List<String> excludes = Arrays.asList(Requirement.ATTRIBUTE_ID, Requirement.ATTRIBUTE_TEXT,
					Requirement.ATTRIBUTE_VERSION);
			generalSection.put("requirement.attributes", reqAttributes.stream()
					.filter(a -> !excludes.contains(a))
					.collect(Collectors.joining(",")));
			generalSection.put("references.attributes", refAttributes.stream()
					.filter(a -> !excludes.contains(a))
					.collect(Collectors.joining(",")));

			ini.store(iniFile);
			config = iniFile;
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, "Invalid config file " + iniFile, e);
			// TODO show error as dialog or dimmer panel
		}
	}

	public void parse(DoubleProperty progress) throws IOException
	{
		final IOException[] ex = { null };
		final AtomicInteger count = new AtomicInteger(0);

		requirementSources.values()
				.parallelStream()
				.forEach(
						requirementSource -> {
							try
							{
								parse(requirementSource);
								synchronized (progress)
								{
									progress.set((double) count.incrementAndGet() / requirementSources.size());
								}
							}
							catch (IOException e)
							{
								ex[0] = new IOException("Parsing source \"" + requirementSource.getName() + "\": "
										+ e.getLocalizedMessage(), e);
							}
						});

		if (ex[0] != null)
		{
			throw ex[0];
		}
	}

	private void parse(InputRequirementSource requirementSource) throws IOException
	{
		logger.info("Start parsing " + requirementSource.getName());

		requirementSource.getRequirements().clear();

		Pattern patternStart = null;
		Pattern patternEnd = null;
		Pattern patternRef = null;
		if (!Strings.isNullOrEmpty(requirementSource.getReqRef()))
		{
			patternRef = Pattern.compile(requirementSource.getReqRef(), Pattern.MULTILINE);
		}
		if (!Strings.isNullOrEmpty(requirementSource.getReqStart())
				&& !Strings.isNullOrEmpty(requirementSource.getReqEnd()))
		{
			patternStart = Pattern.compile(requirementSource.getReqStart(), Pattern.MULTILINE);
			patternEnd = Pattern.compile(requirementSource.getReqEnd(), Pattern.MULTILINE);

			parseMultiRequirementByFile(requirementSource, patternStart, patternEnd, patternRef);
		}
		else
		{
			parseReferencesInFiles(requirementSource, patternRef);
		}

		logger.info("End parsing " + requirementSource.getName());
	}

	/**
	 * @param newRequirementSource
	 * @param newPatternRef
	 * @throws IOException
	 */
	private void parseReferencesInFiles(InputRequirementSource requirementSource, Pattern patternRef)
			throws IOException
	{
		final ConcatReader reader = getReader(requirementSource);
		Path root = requirementSource.getSourcePath();
		if (!root.isAbsolute())
		{
			Path cfgRoot = config.getAbsoluteFile().getParentFile().toPath();
			root = cfgRoot.resolve(root);
		}
		final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
		StringBuilder builder = new StringBuilder(3 * BUFFER_SIZE);
		int r = reader.read(buffer);
		while (r >= 0)
		{
			buffer.flip();
			builder.append(buffer, 0, r);
			buffer.clear();

			Path prevPath = reader.getCurrentPath();
			r = reader.read(buffer);
			Path currentPath = reader.getCurrentPath();

			// If all file content read
			if (!prevPath.equals(currentPath))
			{
				String relPath = root.relativize(prevPath).toString();
				Requirement requirement = new Requirement(requirementSource);
				requirement.setId(relPath);
				requirement.setText(relPath);

				Matcher matcherRef = patternRef.matcher(builder);
				parseReferences(requirementSource, requirement, matcherRef);

				if (requirement.getReferenceCount() > 0)
				{
					requirementSource.getRequirements().add(requirement);
				}

				builder.setLength(0);
			}
		}
	}

	/**
	 * @param requirementSource
	 * @param patternStart
	 * @param patternEnd
	 * @param patternRef
	 * @throws IOException
	 */
	private void parseMultiRequirementByFile(InputRequirementSource requirementSource, Pattern patternStart,
			Pattern patternEnd, Pattern patternRef) throws IOException
	{
		boolean requirementStarted = false;
		Requirement requirement = null;
		final ConcatReader reader = getReader(requirementSource);
		final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
		StringBuilder builder = new StringBuilder(3 * BUFFER_SIZE);
		int r = reader.read(buffer);
		while (r >= 0)
		{
			buffer.flip();
			builder.append(buffer, 0, r);
			buffer.clear();

			Matcher matcherStart = patternStart.matcher(builder);
			Matcher matcherEnd = patternEnd.matcher(builder);

			while (matcherStart.find(0))
			{
				requirementStarted = true;

				boolean endMatch = matcherEnd.find(matcherStart.end());
				if (endMatch)
				{
					int pos = matcherStart.start();
					int endPos = matcherEnd.start();
					// search for last requirement start before the requirement end.
					String req = matcherStart.group();
					boolean hasNextStart = matcherStart.find(matcherStart.end());
					while (hasNextStart && matcherStart.start() < endPos)
					{
						logger.info(requirementSource.getName() + ": Ignore matching requirement without end :" + req);
						pos = matcherStart.start();
						req = matcherStart.group();
						hasNextStart = matcherStart.find(matcherStart.end());
					}
					matcherStart.find(pos);
				}

				if (requirement != null && patternRef != null)
				{
					// search for references between last requirement end and new requirement start
					Matcher matcherRef = patternRef.matcher(builder);
					matcherRef.region(0, matcherStart.start());
					parseReferences(requirementSource, requirement, matcherRef);
					builder.replace(0, matcherStart.start(), "");
					matcherStart.find(0);
				}

				if (matcherEnd.find(matcherStart.end()))
				{
					String reqContent = builder.substring(matcherStart.end(), matcherEnd.start());

					requirement = new Requirement(requirementSource);
					requirement.setContent(reqContent);

					for (Map.Entry<String, Integer> attEntry : requirementSource.getAttributesGroup().entrySet())
					{
						String attName = attEntry.getKey();
						Integer group = attEntry.getValue();
						if (group != null && group <= matcherStart.groupCount())
						{
							String reqAtt = Strings.nullToEmpty(matcherStart.group(group));
							requirement.putAttribute(attName, reqAtt);
						}
					}

					if (patternRef != null)
					{
						Matcher matcherRef = patternRef.matcher(reqContent);
						parseReferences(requirementSource, requirement, matcherRef);
					}

					requirementSource.getRequirements().add(requirement);

					builder.replace(0, matcherEnd.end(), "");
					requirementStarted = false;
				}
				else
				{
					break;
				}
			}

			// if no requirement from 2 buffer size
			if (!requirementStarted && builder.length() >= 2 * BUFFER_SIZE
					&& (requirement == null || patternRef == null))
			{
				builder.delete(0, builder.length() - BUFFER_SIZE);
			}

			r = reader.read(buffer);
		}
	}

	private void parseReferences(InputRequirementSource requirementSource, Requirement requirement, Matcher matcherRef)
	{
		while (matcherRef.find())
		{
			String refText = Strings.nullToEmpty(matcherRef.group(0));

			Requirement reference = new Requirement();
			reference.setText(refText);

			for (Map.Entry<String, Integer> attEntry : requirementSource.getRefAttributesGroup().entrySet())
			{
				String attName = attEntry.getKey();
				Integer group = attEntry.getValue();
				if (group != null && group <= matcherRef.groupCount())
				{
					String reqAtt = Strings.nullToEmpty(matcherRef.group(group));
					reference.putAttribute(attName, reqAtt);
				}
			}

			requirement.addReference(reference);
		}
	}

	private ConcatReader getReader(InputRequirementSource requirementSource) throws IOException
	{
		ConcatReader concatReader = new ConcatReader();
		Path srcPath = requirementSource.getSourcePath();
		if (!srcPath.isAbsolute())
		{
			Path root = config.getAbsoluteFile().getParentFile().toPath();
			srcPath = root.resolve(srcPath);
		}
		if (srcPath.toFile().isFile())
		{
			concatReader.add(srcPath);
		}
		else
		{
			Pattern patternfilter = null;
			String filter = requirementSource.getFilter();
			if (!Strings.isNullOrEmpty(filter))
			{
				patternfilter = Pattern.compile(filter);
			}
			fillReaders(concatReader, srcPath, srcPath, patternfilter);
		}
		return concatReader;
	}

	private void fillReaders(ConcatReader concatReader, Path base, Path root, Pattern filter) throws IOException
	{
		DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(root);
		Iterator<Path> iterator = stream.iterator();
		while (iterator.hasNext())
		{
			Path path = iterator.next();
			if (path.toFile().isFile())
			{
				if (filter == null || filter.matcher(base.relativize(path).toString()).find())
				{
					concatReader.add(path);
				}
			}
			else if (path.startsWith(root) && !path.equals(root))
			{
				fillReaders(concatReader, base, path, filter);
			}
		}
	}

	public void analyse() throws IOException
	{
		for (InputRequirementSource source : requirementSources.values())
		{
			logger.info("Start analyse " + source.getName());

			List<InputRequirementSource> covers = source.getCovers();
			TreeSet<Requirement> reqSource = source.getRequirements();
			for (InputRequirementSource coverSource : covers)
			{
				TreeSet<Requirement> reqCover = coverSource.getRequirements();
				TreeSet<Requirement> reqCoverred = new TreeSet<>(reqCover);
				for (Requirement req : reqSource)
				{
					Set<Requirement> realReqCovers = new TreeSet<>();
					Iterable<Requirement> refs = req.getReferences();
					for (Requirement reqRef : refs)
					{
						Requirement found = reqCover.ceiling(reqRef);
						if (found != null && found.equals(reqRef))
						{
							reqCoverred.remove(found);
							realReqCovers.add(found);
							found.addReferredBy(req);
						}
					}
					for (Requirement realReqCover : realReqCovers)
					{
						req.addReference(realReqCover);
					}
				}
				double coverage = (double) (reqCover.size() - reqCoverred.size()) / reqCover.size();
				coverSource.getCoversBy().put(source, coverage);
			}

			logger.info("End analyse " + source.getName());
		}
	}

	public void writeExcel(Window parent) throws IOException, InvalidFormatException
	{
		logger.info("Start write excel output");

		Path outputFile = output;
		if (!outputFile.isAbsolute())
		{
			Path root = config.getAbsoluteFile().getParentFile().toPath();
			outputFile = root.resolve(outputFile);
		}

		// test using template
		InputStream is = getClass().getResourceAsStream("/com/ben12/reta/resources/template/template.xlsx");
		ExcelTransformer transformer = new ExcelTransformer();
		List<String> sheetNames = new ArrayList<>();
		List<String> sheetTemplateNames = new ArrayList<>();
		for (InputRequirementSource requirementSource : requirementSources.values())
		{
			sheetTemplateNames.add("DOCUMENT");
			sheetTemplateNames.add("COVERAGE");
			sheetNames.add(requirementSource.getName());
			sheetNames.add(requirementSource.getName() + " coverage");
		}

		List<Map<String, Object>> sheetValues = new ArrayList<>();
		for (InputRequirementSource source : requirementSources.values())
		{
			Map<String, Object> values = new HashMap<>();
			values.put("source", source);
			values.put("null", null);
			values.put("line", "\n");

			Set<String> attributes = new LinkedHashSet<>();
			attributes.add(Requirement.ATTRIBUTE_ID);
			if (source.getAttributesGroup().containsKey(Requirement.ATTRIBUTE_VERSION))
			{
				attributes.add(Requirement.ATTRIBUTE_VERSION);
			}
			attributes.addAll(source.getAttributesGroup().keySet());
			attributes.remove(Requirement.ATTRIBUTE_TEXT);
			values.put("attributes", attributes);

			Set<String> refAttributes = new LinkedHashSet<>();
			refAttributes.add(Requirement.ATTRIBUTE_ID);
			if (source.getRefAttributesGroup().containsKey(Requirement.ATTRIBUTE_VERSION))
			{
				refAttributes.add(Requirement.ATTRIBUTE_VERSION);
			}
			refAttributes.addAll(source.getRefAttributesGroup().keySet());
			refAttributes.remove(Requirement.ATTRIBUTE_TEXT);
			values.put("refAttributes", refAttributes);

			sheetValues.add(values);
			sheetValues.add(values);
		}

		Workbook wb = transformer.transform(is, sheetTemplateNames, sheetNames, sheetValues);
		int sheetCount = wb.getNumberOfSheets();
		for (int i = 0; i < sheetCount; i++)
		{
			Sheet sheet = wb.getSheetAt(i);
			int columns = 0;
			for (int j = 0; j <= sheet.getLastRowNum(); j++)
			{
				Row row = sheet.getRow(j);
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
		catch (FileNotFoundException e)
		{
			int confirm = MessageDialog.showQuestionMessage(null, "Excel output file must be closed.");

			if (confirm == MessageDialog.OK_OPTION)
			{
				try (FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
				{
					wb.write(fos);
				}
				catch (IOException e2)
				{
					throw e2;
				}
			}
			else
			{
				throw e;
			}
		}

		logger.info("End write excel output");
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (InputRequirementSource reqSource : requirementSources.values())
		{
			builder.append(reqSource.toString());
			builder.append('\n');
		}
		return builder.toString();
	}
}
