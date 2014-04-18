// Package : com.ben12.reta.model
// File : Requirement.java
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

package com.ben12.reta.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Benoît Moreau (ben.12)
 */
public class Requirement implements Comparable<Requirement>
{
	public static final String		ATTRIBUTE_TEXT		= "Text";

	public static final String		ATTRIBUTE_ID		= "Id";

	public static final String		ATTRIBUTE_VERSION	= "Version";

	private InputRequirementSource	source				= null;

	private String					id;

	private String					version				= "";

	private String					text				= "";

	private String					content				= "";

	private Map<String, String>		attributes			= null;

	private Set<Requirement>		references			= null;

	private Set<Requirement>		referredBy			= null;

	public InputRequirementSource getSource()
	{
		return source;
	}

	public void setSource(InputRequirementSource source)
	{
		this.source = source;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public String getAttribut(String name)
	{
		String att = null;
		switch (name)
		{
		case ATTRIBUTE_TEXT:
			att = getText();
			break;
		case ATTRIBUTE_ID:
			att = getId();
			break;
		case ATTRIBUTE_VERSION:
			att = getVersion();
			break;
		default:
			if (attributes != null)
			{
				att = attributes.get(name);
			}
			break;
		}
		return att;
	}

	public void putAttribut(String name, String value)
	{
		switch (name)
		{
		case ATTRIBUTE_TEXT:
			setText(value);
			break;
		case ATTRIBUTE_ID:
			setId(value);
			break;
		case ATTRIBUTE_VERSION:
			setVersion(value);
			break;
		default:
			if (attributes == null && value != null)
			{
				attributes = new HashMap<>(1);
			}
			if (attributes != null)
			{
				this.attributes.put(name, value);
			}
			break;
		}
	}

	public void addReference(Requirement reference)
	{
		if (references == null)
		{
			references = new TreeSet<>();
		}
		// replace reference
		references.remove(reference);
		references.add(reference);
	}

	public int getReferenceCount()
	{
		return (references == null ? 0 : references.size());
	}

	public Iterable<Requirement> getReferenceIterable()
	{
		return (references == null ? new ArrayList<Requirement>(0) : references);
	}

	public void addReferredBy(Requirement reference)
	{
		if (referredBy == null)
		{
			referredBy = new TreeSet<>();
		}
		// replace reference
		referredBy.remove(reference);
		referredBy.add(reference);
	}

	public int getReferredByCount()
	{
		return (referredBy == null ? 0 : referredBy.size());
	}

	public Iterable<Requirement> getReferredByIterable()
	{
		return (referredBy == null ? new ArrayList<Requirement>(0) : referredBy);
	}

	@Override
	public int compareTo(Requirement other)
	{
		int comp = 0;
		if (other == null)
		{
			comp = -1;
		}
		else
		{
			comp = id.compareTo(other.id);
			if (comp == 0 && (version != null || other.version != null))
			{
				if (version == null && other.version != null)
				{
					comp = -1;
				}
				else if (version != null && other.version == null)
				{
					comp = 1;
				}
				else
				{
					comp = version.compareTo(other.version);
				}
			}
		}
		return comp;
	}

	@Override
	public boolean equals(Object obj)
	{
		boolean equals = false;
		if (this == obj)
		{
			equals = true;
		}
		else if (obj instanceof Requirement)
		{
			Requirement other = (Requirement) obj;
			equals = Objects.equals(id, other.id);
			equals = equals && Objects.equals(version, other.version);
		}
		return equals;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, version);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("Requirement \"");
		builder.append(text);
		builder.append("\"\n\tId: ");
		builder.append(id);
		builder.append("\n\tVersion: ");
		builder.append(version);
		builder.append("\n");
		if (attributes != null)
		{
			for (Map.Entry<String, String> entry : attributes.entrySet())
			{
				builder.append("\t");
				builder.append(entry.getKey());
				builder.append(": ");
				builder.append(entry.getValue());
				builder.append("\n");
			}
		}
		builder.append("\tReferences: ");
		builder.append("\n");
		if (references != null)
		{
			for (Requirement ref : references)
			{
				builder.append("\t\t");
				builder.append(ref.getId());
				builder.append(" version:");
				builder.append(ref.getVersion());
				builder.append("\n");
			}
		}
		builder.append("\tContent: ");
		builder.append(content);

		return builder.toString();
	}
}
