// Package : com.ben12.reta.plugin.tika.parser
// File : RetaTikaParser.java
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
package com.ben12.reta.plugin.tika.parser;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ben12.reta.api.Requirement;
import com.ben12.reta.api.RequirementSourceManager;
import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.plugin.tika.io.ConcatReader;
import com.ben12.reta.plugin.tika.model.TikaSourceConfiguration;
import com.google.common.base.Strings;

/**
 * @author Benoît Moreau (ben.12)
 */
public class RetaTikaParser
{
	/** Buffer size (2 Mo). */
	private static final int				BUFFER_SIZE	= 2 * 1024 * 1024;

	private static final Logger				LOGGER		= Logger.getLogger(RetaTikaParser.class.getName());

	private final TikaSourceConfiguration	configuration;

	/**
	 * 
	 */
	public RetaTikaParser(final TikaSourceConfiguration theConfiguration)
	{
		configuration = theConfiguration;
	}

	private void parse(final RequirementSourceManager requirementSource) throws IOException
	{
		parse(requirementSource, null, Integer.MAX_VALUE);
	}

	public void parse(final RequirementSourceManager requirementSource, final StringBuilder sourceText, final int limit)
			throws IOException
	{
		LOGGER.info("Start parsing " + requirementSource.getName());

		Pattern patternStart = null;
		Pattern patternEnd = null;
		Pattern patternRef = null;
		if (!Strings.isNullOrEmpty(configuration.getReqRef()))
		{
			patternRef = Pattern.compile(configuration.getReqRef(), Pattern.MULTILINE);
		}
		if (!Strings.isNullOrEmpty(configuration.getReqStart()))
		{
			patternStart = Pattern.compile(configuration.getReqStart(), Pattern.MULTILINE);
			if (!Strings.isNullOrEmpty(configuration.getReqEnd()))
			{
				patternEnd = Pattern.compile(configuration.getReqEnd(), Pattern.MULTILINE);
			}

			parseMultiRequirementByFile(requirementSource, patternStart, patternEnd, patternRef, sourceText, limit);
		}
		else
		{
			parseReferencesInFiles(requirementSource, patternRef, sourceText, limit);
		}

		LOGGER.info("End parsing " + requirementSource.getName());
	}

	/**
	 * @param newRequirementSource
	 * @param newPatternRef
	 * @throws IOException
	 */
	private void parseReferencesInFiles(final RequirementSourceManager requirementSource, final Pattern patternRef,
			final StringBuilder sourceText, int limit) throws IOException
	{
		final ConcatReader reader = getReader();
		Path root = Paths.get(configuration.getSourcePath());
		if (!root.isAbsolute())
		{
			root = Paths.get(System.getProperty("user.dir")).resolve(root);
		}
		final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
		final StringBuilder builder = new StringBuilder(3 * BUFFER_SIZE);
		int r = reader.read(buffer);
		while (r >= 0 && (limit == Integer.MAX_VALUE || limit > 0))
		{
			buffer.flip();
			builder.append(buffer, 0, Math.min(limit, r));
			if (sourceText != null)
			{
				buffer.rewind();
				sourceText.append(buffer, 0, Math.min(limit, r));
			}
			buffer.clear();
			if (limit != Integer.MAX_VALUE)
			{
				limit -= r;
			}

			final Path prevPath = reader.getCurrentPath();
			r = reader.read(buffer);
			final Path currentPath = reader.getCurrentPath();

			// If all file content read
			if (!prevPath.equals(currentPath))
			{
				final String relPath = root.relativize(prevPath).toString();
				final Matcher matcherRef = patternRef.matcher(builder);

				final Requirement requirement = requirementSource.addRequirement(relPath, relPath, null, null,
						Collections.emptyMap());

				parseReferences(requirementSource, requirement, matcherRef);

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
	private void parseMultiRequirementByFile(final RequirementSourceManager requirementSource,
			final Pattern patternStart, final Pattern patternEnd, final Pattern patternRef,
			final StringBuilder sourceText, int limit) throws IOException
	{
		boolean requirementStarted = false;
		Requirement requirement = null;
		final ConcatReader reader = getReader();
		final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
		final StringBuilder builder = new StringBuilder(3 * BUFFER_SIZE);
		int r = reader.read(buffer);
		while (r >= 0 && (limit == Integer.MAX_VALUE || limit > 0))
		{
			buffer.flip();
			builder.append(buffer, 0, Math.min(limit, r));
			if (sourceText != null)
			{
				buffer.rewind();
				sourceText.append(buffer, 0, Math.min(limit, r));
			}
			buffer.clear();
			if (limit != Integer.MAX_VALUE)
			{
				limit -= r;
			}

			final Path path = reader.getCurrentPath();
			r = reader.read(buffer);
			final Path newPath = reader.getCurrentPath();

			final Matcher matcherStart = patternStart.matcher(builder);
			final Matcher matcherEnd = (patternEnd != null ? patternEnd.matcher(builder) : null);

			while (matcherStart.find(0))
			{
				requirementStarted = true;

				// find start is at the end of buffer and we are not at the eof.
				if (matcherStart.end() == builder.length() && r >= 0 && path.equals(newPath))
				{
					// requirement could be partial
					break;
				}

				boolean endMatch = false;
				if (matcherEnd != null)
				{
					endMatch = matcherEnd.find(matcherStart.end());
				}

				if (endMatch)
				{
					int pos = matcherStart.start();
					final int endPos = matcherEnd.start();
					// search for last requirement start before the requirement end.
					String req = matcherStart.group();
					boolean hasNextStart = matcherStart.find(matcherStart.end());
					while (hasNextStart && matcherStart.start() < endPos)
					{
						LOGGER.warning(requirementSource.getName() + " (" + path
								+ "): \nIgnore matching requirement without end :" + req);
						pos = matcherStart.start();
						req = matcherStart.group();
						hasNextStart = matcherStart.find(matcherStart.end());
					}
					matcherStart.find(pos);
				}

				if (requirement != null && patternRef != null)
				{
					// search for references between last requirement end and new requirement start
					final Matcher matcherRef = patternRef.matcher(builder);
					matcherRef.region(0, matcherStart.start());
					parseReferences(requirementSource, requirement, matcherRef);
					builder.replace(0, matcherStart.start(), "");
					matcherStart.find(0);
				}

				if (matcherEnd == null || matcherEnd.find(matcherStart.end()))
				{
					requirement = extractRequirement(requirementSource, builder, path, matcherStart, matcherEnd,
							patternRef);
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
		}

		if (requirement != null && patternRef != null)
		{
			// search for references between last requirement end and end of file
			final Matcher matcherRef = patternRef.matcher(builder);
			parseReferences(requirementSource, requirement, matcherRef);
			builder.setLength(0);
		}
	}

	/**
	 * @param requirementSource
	 * @param builder
	 * @param path
	 * @param matcherStart
	 * @param matcherEnd
	 * @param patternRef
	 * @return
	 */
	public Requirement extractRequirement(final RequirementSourceManager requirementSource,
			final StringBuilder builder, final Path path, final Matcher matcherStart, final Matcher matcherEnd,
			final Pattern patternRef)
	{
		Requirement requirement;
		int endPos = matcherStart.end();
		int endEndPos = matcherStart.end();
		if (matcherEnd != null)
		{
			endPos = matcherEnd.start();
			endEndPos = matcherEnd.end();
		}

		final String reqContent = builder.substring(matcherStart.end(), endPos);
		final Map<String, String> attributes = new HashMap<>();

		for (final Map.Entry<String, Integer> attEntry : configuration.getAttributesGroup().entrySet())
		{
			final String attName = attEntry.getKey();
			final Integer group = attEntry.getValue();
			if (group != null && group <= matcherStart.groupCount())
			{
				final String reqAtt = Strings.nullToEmpty(matcherStart.group(group));
				attributes.put(attName, reqAtt);
			}
		}

		requirement = requirementSource.addRequirement(attributes.remove(SourceConfiguration.ATTRIBUTE_TEXT),
				attributes.remove(SourceConfiguration.ATTRIBUTE_ID),
				attributes.remove(SourceConfiguration.ATTRIBUTE_VERSION), reqContent, attributes);

		if (requirement != null)
		{
			if (patternRef != null)
			{
				final Matcher matcherRef = patternRef.matcher(reqContent);
				parseReferences(requirementSource, requirement, matcherRef);
			}
		}
		else
		{
			LOGGER.warning(requirementSource.getName() + " (" + path + "): \nIgnore duplicate matching requirement :"
					+ matcherStart.group());
		}

		builder.replace(0, endEndPos, "");
		return requirement;
	}

	private void parseReferences(final RequirementSourceManager requirementSource, final Requirement requirement,
			final Matcher matcherRef)
	{
		while (matcherRef.find())
		{
			final String refText = Strings.nullToEmpty(matcherRef.group(0));
			final Map<String, String> attributes = new HashMap<>();

			for (final Map.Entry<String, Integer> attEntry : configuration.getRefAttributesGroup().entrySet())
			{
				final String attName = attEntry.getKey();
				final Integer group = attEntry.getValue();
				if (group != null && group <= matcherRef.groupCount())
				{
					final String reqAtt = Strings.nullToEmpty(matcherRef.group(group));
					attributes.put(attName, reqAtt);
				}
			}

			requirement.addReference(refText, attributes.remove(SourceConfiguration.ATTRIBUTE_ID),
					attributes.remove(SourceConfiguration.ATTRIBUTE_VERSION), attributes);
		}
	}

	private ConcatReader getReader() throws IOException
	{
		final ConcatReader concatReader = new ConcatReader();
		Path srcPath = Paths.get(configuration.getSourcePath());
		if (!srcPath.isAbsolute())
		{
			srcPath = Paths.get(System.getProperty("user.dir")).resolve(srcPath);
		}
		if (srcPath.toFile().isFile())
		{
			concatReader.add(srcPath);
		}
		else
		{
			Pattern patternfilter = null;
			final String filter = configuration.getFilter();
			if (!Strings.isNullOrEmpty(filter))
			{
				patternfilter = Pattern.compile(filter);
			}
			fillReaders(concatReader, srcPath, srcPath, patternfilter);
		}
		return concatReader;
	}

	private void fillReaders(final ConcatReader concatReader, final Path base, final Path root, final Pattern filter)
			throws IOException
	{
		final DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(root);
		final Iterator<Path> iterator = stream.iterator();
		while (iterator.hasNext())
		{
			final Path path = iterator.next();
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

}
