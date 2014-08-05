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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.validation.constraints.NotNull;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.ben12.reta.constraints.PathExists;
import com.ben12.reta.constraints.PathExists.KindOfPath;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.Requirement;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
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
			}
			else
			{
				this.output = Paths.get(iniFile.getParent(), Files.getNameWithoutExtension(iniFile.getName()) + ".xls");
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

	public void parse() throws IOException
	{
		final IOException[] ex = { null };

		requirementSources.values()
				.parallelStream()
				.forEach(
						requirementSource -> {
							try
							{
								parse(requirementSource);
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
							requirement.putAttribut(attName, reqAtt);
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
					reference.putAttribut(attName, reqAtt);
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
					Iterable<Requirement> refs = req.getReferenceIterable();
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

	public void writeExcel() throws IOException
	{
		logger.info("Start write excel output");

		String extension = Files.getFileExtension(output.getFileName().toString());
		final Workbook wb;
		if (extension == "xlsx")
		{
			wb = new XSSFWorkbook();
		}
		else
		{
			wb = new HSSFWorkbook();
		}

		Map<String, CellStyle> styles = createStyles(wb);

		for (InputRequirementSource requirementSource : requirementSources.values())
		{
			Set<String> attributes = requirementSource.getAttributesGroup().keySet();
			int columnCount = attributes.size() + 1;

			Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(requirementSource.getName()));

			// turn off gridlines
			sheet.setDisplayGridlines(false);
			sheet.setPrintGridlines(false);
			sheet.setFitToPage(true);
			sheet.setHorizontallyCenter(true);
			PrintSetup printSetup = sheet.getPrintSetup();
			printSetup.setLandscape(true);

			// the following three statements are required only for HSSF
			sheet.setAutobreaks(true);
			printSetup.setFitHeight((short) 1);
			printSetup.setFitWidth((short) 1);

			int row = 0;

			Row rowTitle = sheet.createRow(row++);
			for (int i = 0; i <= columnCount; i++)
			{
				rowTitle.createCell(i).setCellStyle(styles.get("title"));
			}
			Cell cellTitle = rowTitle.createCell(0);
			cellTitle.setCellValue(requirementSource.getName());
			cellTitle.setCellStyle(styles.get("title"));
			sheet.addMergedRegion(new CellRangeAddress(rowTitle.getRowNum(), rowTitle.getRowNum(), 0, columnCount));

			Row rowFile = sheet.createRow(row++);
			for (int i = 0; i <= columnCount; i++)
			{
				rowFile.createCell(i).setCellStyle(styles.get("file"));
			}
			Cell cellFile = rowFile.createCell(0);
			cellFile.setCellValue(requirementSource.getSourcePath().toString());
			cellFile.setCellStyle(styles.get("file"));
			sheet.addMergedRegion(new CellRangeAddress(rowFile.getRowNum(), rowFile.getRowNum(), 0, columnCount));

			row++;

			row = fillRequirementTable(sheet, styles, row, requirementSource);

			row++;

			for (InputRequirementSource coverSource : requirementSource.getCovers())
			{
				row = fillCoversRequirementTable(sheet, styles, row, requirementSource, coverSource);

				row++;
			}

			for (InputRequirementSource coverBySource : requirementSource.getCoversBy().keySet())
			{
				row = fillCoversByRequirementTable(sheet, styles, row, requirementSource, coverBySource);

				row++;
			}

			row = fillUnknownReferencesTable(sheet, styles, row, requirementSource);

			row++;

			for (int i = 0; i < 20; i++)
			{
				sheet.autoSizeColumn(i);
			}
		}

		Path outputFile = output;
		if (!outputFile.isAbsolute())
		{
			Path root = config.getAbsoluteFile().getParentFile().toPath();
			outputFile = root.resolve(outputFile);
		}

		try (FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
		{
			wb.write(fos);
		}
		catch (FileNotFoundException e)
		{
			final JFrame frame = new JFrame();
			JOptionPane optionPane = new JOptionPane("Excel output file must be closed.", JOptionPane.QUESTION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
			optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, p -> frame.dispose());
			frame.getContentPane().add(optionPane);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			while (frame.isVisible())
			{
				try
				{
					Thread.sleep(50);
				}
				catch (Exception ex)
				{
				}
			}

			if (Objects.equal(optionPane.getValue(), JOptionPane.YES_OPTION))
			{
				try (FileOutputStream fos = new FileOutputStream(outputFile.toFile()))
				{
					wb.write(fos);
				}
				catch (IOException e2)
				{
					logger.log(Level.SEVERE, "Writing output excel file.", e);
					throw new IOException(e);
				}
			}
			else
			{
				logger.log(Level.SEVERE, "Writing output excel file.", e);
				throw new IOException(e);
			}
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, "Writing output excel file.", e);
			throw e;
		}

		logger.info("End write excel output");
	}

	private int fillUnknownReferencesTable(Sheet sheet, Map<String, CellStyle> styles, int row,
			InputRequirementSource requirementSource)
	{
		int count = 0;
		Map<Requirement, List<Requirement>> unknown = new HashMap<>();
		for (Requirement req : requirementSource.getRequirements())
		{
			for (Requirement ref : req.getReferenceIterable())
			{
				if (ref.getSource() == null)
				{
					List<Requirement> refs = unknown.get(req);
					if (refs == null)
					{
						refs = new ArrayList<>();
						unknown.put(req, refs);
					}
					refs.add(ref);
					count++;
				}
			}
		}

		if (count > 0)
		{
			List<String> idAndVersion = Arrays.asList(Requirement.ATTRIBUTE_TEXT, Requirement.ATTRIBUTE_ID,
					Requirement.ATTRIBUTE_VERSION);
			Set<String> attributes = new HashSet<>(requirementSource.getRefAttributesGroup().keySet());
			attributes.removeAll(idAndVersion);

			Row rowReqCount = sheet.createRow(row++);
			for (int i = 1; i <= attributes.size() + 3; i++)
			{
				rowReqCount.createCell(i).setCellStyle(styles.get("unknownRefCount"));
			}
			Cell cellReqCount = rowReqCount.createCell(1);
			cellReqCount.setCellStyle(styles.get("unknownRefCount"));
			cellReqCount.setCellFormula("ROWS(B" + (rowReqCount.getRowNum() + 4) + ":B"
					+ (rowReqCount.getRowNum() + count + 3) + ")");
			sheet.addMergedRegion(new CellRangeAddress(rowReqCount.getRowNum(), rowReqCount.getRowNum(), 1,
					attributes.size() + 3));

			int column = 1;
			Row rowHeader = sheet.createRow(row++);
			Cell cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(requirementSource.getName());
			cellHeader.setCellStyle(styles.get("header"));
			sheet.addMergedRegion(new CellRangeAddress(rowHeader.getRowNum(), rowHeader.getRowNum() + 1,
					cellHeader.getColumnIndex(), cellHeader.getColumnIndex()));

			cellHeader = rowHeader.createCell(column);
			for (int i = cellHeader.getColumnIndex(); i <= cellHeader.getColumnIndex() + attributes.size() + 1; i++)
			{
				rowHeader.createCell(i).setCellStyle(styles.get("header"));
			}
			cellHeader.setCellValue("Reference unknown");
			cellHeader.setCellStyle(styles.get("header"));
			sheet.addMergedRegion(new CellRangeAddress(rowHeader.getRowNum(), rowHeader.getRowNum(),
					cellHeader.getColumnIndex(), cellHeader.getColumnIndex() + attributes.size() + 1));

			rowHeader = sheet.createRow(row++);

			cellHeader = rowHeader.createCell(column - 1);
			cellHeader.setCellStyle(styles.get("header"));

			cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue("ID");
			cellHeader.setCellStyle(styles.get("header"));

			cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue("Version");
			cellHeader.setCellStyle(styles.get("header"));

			for (String attName : attributes)
			{
				cellHeader = rowHeader.createCell(column++);
				cellHeader.setCellValue(attName);
				cellHeader.setCellStyle(styles.get("header"));
			}

			for (Map.Entry<Requirement, List<Requirement>> entry : unknown.entrySet())
			{
				column = 1;

				Row rowReq = sheet.createRow(row++);

				Cell cellReq = rowReq.createCell(column++);
				cellReq.setCellValue(entry.getKey().getText());
				cellReq.setCellStyle(styles.get("cell"));
				sheet.addMergedRegion(new CellRangeAddress(rowReq.getRowNum(), rowReq.getRowNum()
						+ entry.getValue().size() - 1, cellReq.getColumnIndex(), cellReq.getColumnIndex()));

				for (Requirement ref : entry.getValue())
				{
					int c = 0;

					cellReq = rowReq.createCell(column + (c++));
					cellReq.setCellValue(ref.getId());
					cellReq.setCellStyle(styles.get("cell"));

					cellReq = rowReq.createCell(column + (c++));
					cellReq.setCellValue(ref.getVersion());
					cellReq.setCellStyle(styles.get("cell"));

					for (String attName : attributes)
					{
						cellReq = rowReq.createCell(column + (c++));
						cellReq.setCellValue(ref.getAttribut(attName));
						cellReq.setCellStyle(styles.get("cell"));
					}

					rowReq = sheet.createRow(row++);

					cellReq = rowReq.createCell(column - 1);
					cellReq.setCellStyle(styles.get("cell"));
				}

				cellReq = rowReq.createCell(column - 1);
				cellReq.setCellStyle(styles.get("clear"));
				row--;
			}
		}
		return 0;
	}

	private int fillRequirementTable(Sheet sheet, Map<String, CellStyle> styles, int row,
			InputRequirementSource requirementSource)
	{
		List<String> idAndVersion = Arrays.asList(Requirement.ATTRIBUTE_TEXT, Requirement.ATTRIBUTE_ID,
				Requirement.ATTRIBUTE_VERSION);
		Set<String> attributes = new HashSet<>(requirementSource.getAttributesGroup().keySet());
		attributes.removeAll(idAndVersion);

		int reqCount = requirementSource.getRequirements().size();
		Row rowReqCount = sheet.createRow(row++);
		for (int i = 1; i <= attributes.size() + 3; i++)
		{
			rowReqCount.createCell(i).setCellStyle(styles.get("reqCount"));
		}
		Cell cellReqCount = rowReqCount.createCell(1);
		cellReqCount.setCellValue(reqCount);
		cellReqCount.setCellFormula("ROWS(B" + (rowReqCount.getRowNum() + 3) + ":B"
				+ (rowReqCount.getRowNum() + reqCount + 2) + ")");
		cellReqCount.setCellStyle(styles.get("reqCount"));
		sheet.addMergedRegion(new CellRangeAddress(rowReqCount.getRowNum(), rowReqCount.getRowNum(), 1,
				attributes.size() + 3));

		int column = 1;
		Row rowHeader = sheet.createRow(row++);
		Cell cellHeader = rowHeader.createCell(column++);
		cellHeader.setCellValue("ID");
		cellHeader.setCellStyle(styles.get("header"));

		cellHeader = rowHeader.createCell(column++);
		cellHeader.setCellValue("Version");
		cellHeader.setCellStyle(styles.get("header"));

		for (String attName : attributes)
		{
			cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(attName);
			cellHeader.setCellStyle(styles.get("header"));
		}

		cellHeader = rowHeader.createCell(column++);
		cellHeader.setCellValue("Cover by");
		cellHeader.setCellStyle(styles.get("header"));

		for (Requirement req : requirementSource.getRequirements())
		{
			column = 1;

			Row rowRequirement = sheet.createRow(row++);

			Set<String> coverBySources = new LinkedHashSet<>();
			for (Requirement coverBy : req.getReferredByIterable())
			{
				InputRequirementSource coverBySource = coverBy.getSource();
				if (coverBySource != null)
				{
					coverBySources.add(coverBySource.getName());
				}
			}

			CellStyle style = styles.get("cell");
			if (!requirementSource.getCoversBy().isEmpty())
			{
				if (req.getReferredByCount() == 0)
				{
					style = styles.get("redcell");
				}
				else if (requirementSource.getCoversBy().size() > coverBySources.size())
				{
					style = styles.get("yellowcell");
				}
			}

			Cell cellRequirement = rowRequirement.createCell(column++);
			cellRequirement.setCellValue(req.getId());
			cellRequirement.setCellStyle(style);

			cellRequirement = rowRequirement.createCell(column++);
			cellRequirement.setCellValue(Strings.nullToEmpty(req.getVersion()));
			cellRequirement.setCellStyle(style);

			for (String attName : attributes)
			{
				cellRequirement = rowRequirement.createCell(column++);
				cellRequirement.setCellValue(Strings.nullToEmpty(req.getAttribut(attName)));
				cellRequirement.setCellStyle(style);
			}

			cellRequirement = rowRequirement.createCell(column++);
			cellRequirement.setCellValue(Joiner.on('\n').join(coverBySources));
			cellRequirement.setCellStyle(style);
		}

		return row;
	}

	private int fillCoversRequirementTable(Sheet sheet, Map<String, CellStyle> styles, int row,
			InputRequirementSource requirementSource, InputRequirementSource coverSource)
	{
		double coverage = coverSource.getCoversBy().get(requirementSource);

		Row rowTitle = sheet.createRow(row++);
		for (int i = 0; i <= 3; i++)
		{
			rowTitle.createCell(i).setCellStyle(styles.get("reqCount"));
		}
		Cell cellTitle = rowTitle.createCell(0);
		cellTitle.setCellValue(requirementSource.getName() + " cover " + coverSource.getName() + " at "
				+ (coverage * 100) + "%");
		cellTitle.setCellStyle(styles.get("normal"));
		sheet.addMergedRegion(new CellRangeAddress(rowTitle.getRowNum(), rowTitle.getRowNum(), 0, 3));

		if (coverage > 0.0)
		{
			int column = 1;
			Row rowHeader = sheet.createRow(row++);
			Cell cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(requirementSource.getName());
			cellHeader.setCellStyle(styles.get("header"));

			cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(coverSource.getName());
			cellHeader.setCellStyle(styles.get("header"));

			for (Requirement req : requirementSource.getRequirements())
			{
				Iterable<Requirement> iterable = req.getReferenceIterable();
				List<String> cover = new ArrayList<>();
				for (Requirement ref : iterable)
				{
					if (ref.getSource() == coverSource)
					{
						cover.add(ref.getText());
					}
				}
				if (!cover.isEmpty())
				{
					column = 1;

					Row rowRequirement = sheet.createRow(row++);

					Cell cellRequirement = rowRequirement.createCell(column++);
					cellRequirement.setCellValue(req.getText());
					cellRequirement.setCellStyle(styles.get("cell"));

					Cell cellReference = rowRequirement.createCell(column++);
					cellReference.setCellValue(Joiner.on("\n").join(cover));
					cellReference.setCellStyle(styles.get("cell"));
				}
			}
		}

		return row;
	}

	private int fillCoversByRequirementTable(Sheet sheet, Map<String, CellStyle> styles, int row,
			InputRequirementSource requirementSource, InputRequirementSource coverBySource)
	{
		double coverage = requirementSource.getCoversBy().get(coverBySource);

		Row rowTitle = sheet.createRow(row++);
		for (int i = 0; i <= 3; i++)
		{
			rowTitle.createCell(i).setCellStyle(styles.get("reqCount"));
		}
		Cell cellTitle = rowTitle.createCell(0);
		cellTitle.setCellValue(requirementSource.getName() + " is cover by " + coverBySource.getName() + " at "
				+ (coverage * 100) + "%");
		cellTitle.setCellStyle(styles.get("normal"));
		sheet.addMergedRegion(new CellRangeAddress(rowTitle.getRowNum(), rowTitle.getRowNum(), 0, 3));

		if (coverage > 0.0)
		{
			int column = 1;
			Row rowHeader = sheet.createRow(row++);
			Cell cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(requirementSource.getName());
			cellHeader.setCellStyle(styles.get("header"));

			cellHeader = rowHeader.createCell(column++);
			cellHeader.setCellValue(coverBySource.getName());
			cellHeader.setCellStyle(styles.get("header"));

			for (Requirement req : requirementSource.getRequirements())
			{
				Iterable<Requirement> iterable = req.getReferredByIterable();
				List<String> cover = new ArrayList<>();
				for (Requirement ref : iterable)
				{
					if (ref.getSource() == coverBySource)
					{
						cover.add(ref.getText());
					}
				}
				if (!cover.isEmpty())
				{
					column = 1;

					Row rowRequirement = sheet.createRow(row++);

					Cell cellRequirement = rowRequirement.createCell(column++);
					cellRequirement.setCellValue(req.getText());
					cellRequirement.setCellStyle(styles.get("cell"));

					Cell cellReference = rowRequirement.createCell(column++);
					cellReference.setCellValue(Joiner.on("\n").join(cover));
					cellReference.setCellStyle(styles.get("cell"));
				}
			}
		}

		return row;
	}

	private static Map<String, CellStyle> createStyles(Workbook wb)
	{
		Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

		CellStyle style;
		style = wb.createCellStyle();
		styles.put("clear", style);

		Font titleFont = wb.createFont();
		titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(titleFont);
		styles.put("title", style);

		Font fileFont = wb.createFont();
		fileFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(fileFont);
		styles.put("file", style);

		Font headerFont = wb.createFont();
		headerFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(headerFont);
		styles.put("header", style);

		Font tableCellFont = wb.createFont();
		tableCellFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(tableCellFont);
		style.setWrapText(true);
		styles.put("cell", style);

		Font tableRedCellFont = wb.createFont();
		tableRedCellFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFillForegroundColor(IndexedColors.RED.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(tableRedCellFont);
		style.setWrapText(true);
		styles.put("redcell", style);

		Font tableYellowCellFont = wb.createFont();
		tableYellowCellFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = createBorderedStyle(wb);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(tableYellowCellFont);
		style.setWrapText(true);
		styles.put("yellowcell", style);

		Font reqCountFont = wb.createFont();
		reqCountFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(reqCountFont);
		style.setDataFormat(wb.createDataFormat().getFormat(
				"\\N\\o\\m\\b\\r\\e \\d\\'\\e\\x\\i\\g\\e\\n\\c\\e\\s\\: ###,##0"));
		styles.put("reqCount", style);

		Font unknownRefCount = wb.createFont();
		unknownRefCount.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(unknownRefCount);
		style.setDataFormat(wb.createDataFormat().getFormat(
				"\\N\\o\\m\\b\\r\\e \\d\\e \\r\\e\\f\\e\\r\\e\\n\\c\\e\\s \\i\\n\\c\\o\\n\\n\\u\\e\\s\\: ###,##0"));
		styles.put("unknownRefCount", style);

		Font normalFont = wb.createFont();
		normalFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);
		style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setFont(normalFont);
		styles.put("normal", style);

		return styles;
	}

	private static CellStyle createBorderedStyle(Workbook wb)
	{
		CellStyle style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return style;
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
