// Package : com.ben12.reta.util
// File : DOMUtils.java
// 
// Copyright (C) 2025 Benoît Moreau (ben.12)
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Benoît Moreau (ben.12)
 */
public class DOMUtils
{
	public static List<Element> getElementsByTagNameAndAttribute(final Document doc, final String tag,
			final String attr)
	{
		final var elements = getElementsByTagName(doc, tag);
		elements.removeIf(e -> !e.hasAttribute(attr));
		return elements;
	}

	public static List<Element> getElementsByTagName(final Document doc, final String name)
	{
		final var nodeList = doc.getElementsByTagName(name);
		return getElements(nodeList);
	}

	public static List<Element> getElementsByTagName(final Element el, final String name)
	{
		final var nodeList = el.getElementsByTagName(name);
		return getElements(nodeList);
	}

	public static List<Element> getElements(final NodeList nodeList)
	{
		return IntStream.range(0, nodeList.getLength())
				.mapToObj(nodeList::item)
				.filter(n -> n instanceof Element)
				.map(Element.class::cast)
				.collect(Collectors.toCollection(ArrayList::new));
	}

}
