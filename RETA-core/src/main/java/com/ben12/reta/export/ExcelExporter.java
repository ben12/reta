// Package : com.ben12.reta.export
// File : ExcelExporter.java
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
package com.ben12.reta.export;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellPropertyType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ben12.reta.api.SourceConfiguration;
import com.ben12.reta.model.InputRequirementSource;
import com.ben12.reta.model.RequirementImpl;

/**
 * Excel Exporter.
 * 
 * @author Benoît Moreau (ben.12)
 */
public class ExcelExporter
{
	/**
	 * Table header cell borders.
	 */
	private final Map<CellPropertyType, Object>	tableHeaderProperties	= Map.of(CellPropertyType.BORDER_TOP,
			BorderStyle.THIN, CellPropertyType.BORDER_LEFT, BorderStyle.THIN, CellPropertyType.BORDER_BOTTOM,
			BorderStyle.MEDIUM, CellPropertyType.BORDER_RIGHT, BorderStyle.THIN);

	/**
	 * Table cell borders.
	 */
	private final Map<CellPropertyType, Object>	tableProperties			= Map.of(CellPropertyType.BORDER_TOP,
			BorderStyle.THIN, CellPropertyType.BORDER_LEFT, BorderStyle.THIN, CellPropertyType.BORDER_BOTTOM,
			BorderStyle.THIN, CellPropertyType.BORDER_RIGHT, BorderStyle.THIN);

	/**
	 * The workbook.
	 */
	private final XSSFWorkbook					workbook;

	/**
	 * Title 1 cell style.
	 */
	private final XSSFCellStyle					title1;

	/**
	 * Title 2 cell style.
	 */
	private final XSSFCellStyle					title2;

	/**
	 * Table header cell style.
	 */
	private final XSSFCellStyle					tableHeader;

	/**
	 * Uncovered cell style.
	 */
	private final XSSFCellStyle					uncovered;

	/**
	 * Partially covered cell style.
	 */
	private final XSSFCellStyle					partialCovered;

	/**
	 * Wrapped cell style.
	 */
	private final XSSFCellStyle					wrapped;

	/**
	 * Construct and initialize excel exporter.
	 */
	public ExcelExporter()
	{
		workbook = new XSSFWorkbook();

		title1 = workbook.createCellStyle();
		title1.setFillForegroundColor(new XSSFColor(new byte[] { 79, (byte) 129, (byte) 189 }));
		title1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		final XSSFFont font = workbook.createFont();
		font.setBold(true);
		title1.setFont(font);
		title1.setAlignment(HorizontalAlignment.CENTER);

		title2 = workbook.createCellStyle();
		title2.setFillForegroundColor(new XSSFColor(new byte[] { 49, (byte) 179, (byte) 215 }));
		title2.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		tableHeader = workbook.createCellStyle();
		tableHeader.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 204, (byte) 255, (byte) 204 }));
		tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		tableHeader.setAlignment(HorizontalAlignment.CENTER);

		uncovered = workbook.createCellStyle();
		uncovered.setFillForegroundColor(IndexedColors.RED.getIndex());
		uncovered.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		uncovered.setWrapText(true);

		partialCovered = workbook.createCellStyle();
		partialCovered.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		partialCovered.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		partialCovered.setWrapText(true);

		wrapped = workbook.createCellStyle();
		wrapped.setWrapText(true);
	}

	/**
	 * Export the requirement sources analysis result.
	 * 
	 * @param requirementSources
	 *            the requirement sources analysis result to export
	 * @return the built workbook.
	 */
	public XSSFWorkbook export(final List<InputRequirementSource> requirementSources)
	{
		for (final InputRequirementSource source : requirementSources)
		{
			exportDocumentSheet(source);
			exportDocumentCoverageSheet(source);
		}

		final var sheetCount = workbook.getNumberOfSheets();
		for (int i = 0; i < sheetCount; i++)
		{
			final var sheet = workbook.getSheetAt(i);
			int columns = 0;
			for (int j = 0; j <= sheet.getLastRowNum(); j++)
			{
				final var row = sheet.getRow(j);
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

		return workbook;
	}

	/**
	 * Add a new row after last one.
	 * 
	 * @param sheet
	 *            Sheet where add the row
	 * @return the added row
	 */
	private XSSFRow addNewRow(final XSSFSheet sheet)
	{
		return addNewRow(sheet, 0);
	}

	/**
	 * Add a new row after last one.
	 * 
	 * @param sheet
	 *            Sheet where add the row
	 * @param skip
	 *            row count to skip
	 * @return the added row
	 */
	private XSSFRow addNewRow(final XSSFSheet sheet, final int skip)
	{
		return sheet.createRow(sheet.getLastRowNum() + 1 + skip);
	}

	/**
	 * Add a new cell after last one.
	 * 
	 * @param row
	 *            Row where add the cell
	 * @return the added cell
	 */
	private XSSFCell addNewCell(final XSSFRow row)
	{
		return addNewCell(row, 0);
	}

	/**
	 * Add a new cell after last one.
	 * 
	 * @param row
	 *            Row where add the cell
	 * @param skip
	 *            cell count to skip
	 * @return the added cell
	 */
	private XSSFCell addNewCell(final XSSFRow row, final int skip)
	{
		final var last = row.getLastCellNum();
		return row.createCell(Math.max(last, 0) + skip);
	}

	/**
	 * Columns spanning.
	 * 
	 * @param sheet
	 *            Sheet where span columns
	 * @param cell
	 *            Left cell to span
	 * @param span
	 *            Columns to span
	 */
	private void addColspan(final XSSFSheet sheet, final XSSFCell cell, final int span)
	{
		if (span > 1)
		{
			sheet.addMergedRegion(new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), cell.getColumnIndex(),
					cell.getColumnIndex() + span - 1));
		}
	}

	/**
	 * Rows spanning.
	 * 
	 * @param sheet
	 *            Sheet where span rows
	 * @param cell
	 *            Top cell to span
	 * @param span
	 *            Rows to span
	 */
	private void addRowspan(final XSSFSheet sheet, final XSSFCell cell, final int span)
	{
		if (span > 1)
		{
			sheet.addMergedRegion(new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex() + span - 1,
					cell.getColumnIndex(), cell.getColumnIndex()));
		}
	}

	/**
	 * Export the document sheet.
	 * 
	 * @param source
	 *            source to export
	 */
	private void exportDocumentSheet(final InputRequirementSource source)
	{
		final var sheet = workbook.createSheet(source.getName());

		final Set<String> attributes = new LinkedHashSet<>();
		// ID in first
		attributes.add(SourceConfiguration.ATTRIBUTE_ID);
		// Version in second if defined
		if (source.getRequirementAttributes().contains(SourceConfiguration.ATTRIBUTE_VERSION))
		{
			attributes.add(SourceConfiguration.ATTRIBUTE_VERSION);
		}
		attributes.addAll(source.getRequirementAttributes());
		// Text is a special case
		attributes.remove(SourceConfiguration.ATTRIBUTE_TEXT);

		final var titleRow = addNewRow(sheet);
		final var titleCell = addNewCell(titleRow);
		titleCell.setCellValue(source.getName());
		titleCell.setCellStyle(title1);
		addColspan(sheet, titleCell, attributes.size() + 1);

		final var pathRow = addNewRow(sheet);
		final var pathCell = addNewCell(pathRow);
		pathCell.setCellValue(source.getConfiguration().getDescription());
		pathCell.setCellStyle(title2);
		addColspan(sheet, pathCell, attributes.size() + 1);

		final var counterRow = addNewRow(sheet, 1);
		final var counterCell = addNewCell(counterRow);
		counterCell.setCellValue("Total: " + source.getRequirements().size());
		addColspan(sheet, counterCell, attributes.size() + 1);

		final var tableHeaderRow = addNewRow(sheet);
		for (final var attribute : attributes)
		{
			final var headerAttributeCell = addNewCell(tableHeaderRow);
			headerAttributeCell.setCellValue(attribute);
			headerAttributeCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerAttributeCell, tableHeaderProperties);
		}

		final var headerCoveredByCell = addNewCell(tableHeaderRow);
		headerCoveredByCell.setCellValue("Covered by");
		headerCoveredByCell.setCellStyle(tableHeader);
		CellUtil.setCellStylePropertiesEnum(headerCoveredByCell, tableHeaderProperties);

		for (final var req : source.getRequirements())
		{
			final var tableRow = addNewRow(sheet);

			final CellStyle cellStyle;
			if (req.getReferredBySource().isEmpty() && !source.getCoversBy().isEmpty())
			{
				cellStyle = uncovered;
			}
			else if (!req.getReferredBySource().isEmpty()
					&& req.getReferredBySource().size() < source.getCoversBy().size())
			{
				cellStyle = partialCovered;
			}
			else
			{
				cellStyle = wrapped;
			}

			for (final var attribute : attributes)
			{
				final var attributeCell = addNewCell(tableRow);
				attributeCell.setCellValue(req.getAttribute(attribute));
				attributeCell.setCellStyle(cellStyle);
				CellUtil.setCellStylePropertiesEnum(attributeCell, tableProperties);
			}

			final var coveredByCell = addNewCell(tableRow);
			coveredByCell.setCellValue(req.getReferredBySource()
					.stream()
					.map(InputRequirementSource::getName)
					.collect(Collectors.joining("\n")));
			coveredByCell.setCellStyle(cellStyle);
			CellUtil.setCellStylePropertiesEnum(coveredByCell, tableProperties);

		}
	}

	/**
	 * Export the document coverage sheet.
	 * 
	 * @param source
	 *            source to export
	 */
	private void exportDocumentCoverageSheet(final InputRequirementSource source)
	{
		final var sheet = workbook.createSheet(source.getName() + " coverage");

		final var titleRow = addNewRow(sheet);
		final var titleCell = addNewCell(titleRow);
		titleCell.setCellValue(source.getName());
		titleCell.setCellStyle(title1);
		addColspan(sheet, titleCell, 2);

		final var pathRow = addNewRow(sheet);
		final var pathCell = addNewCell(pathRow);
		pathCell.setCellValue(source.getConfiguration().getDescription());
		pathCell.setCellStyle(title2);
		addColspan(sheet, pathCell, 2);

		exportCovers(source, sheet);
		exportCoverBy(source, sheet);
		exportUnknownReferences(source, sheet);
	}

	/**
	 * Export the "cover" table.
	 * 
	 * @param source
	 *            source to export
	 * @param sheet
	 *            sheet where export
	 */
	private void exportCovers(final InputRequirementSource source, final XSSFSheet sheet)
	{
		for (final var cover : source.getCovers())
		{
			final var rateRow = addNewRow(sheet, 1);
			final var rateCell = addNewCell(rateRow);
			rateCell.setCellValue(source.getName() + " cover " + cover.getName() + " at "
					+ (cover.getCoversBy().get(source) * 100) + " %");
			addColspan(sheet, rateCell, 2);

			final var tableHeaderRow = addNewRow(sheet);

			final var headerSourceCell = addNewCell(tableHeaderRow);
			headerSourceCell.setCellValue(source.getName());
			headerSourceCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerSourceCell, tableHeaderProperties);

			final var headerCoverCell = addNewCell(tableHeaderRow);
			headerCoverCell.setCellValue(cover.getName());
			headerCoverCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerCoverCell, tableHeaderProperties);

			for (final var req : source.getRequirements())
			{
				final var refForReq = req.getReferencesFor(cover);

				if (!refForReq.isEmpty())
				{
					final var tableReqRow = addNewRow(sheet);

					final var reqSourceCell = addNewCell(tableReqRow);
					reqSourceCell.setCellValue(req.getText());
					CellUtil.setCellStylePropertiesEnum(reqSourceCell, tableProperties);

					final var covSourceCell = addNewCell(tableReqRow);
					covSourceCell.setCellValue(
							refForReq.stream().map(RequirementImpl::getText).collect(Collectors.joining("\n")));
					covSourceCell.setCellStyle(wrapped);
					CellUtil.setCellStylePropertiesEnum(covSourceCell, tableProperties);
				}
			}
		}
	}

	/**
	 * Export the "cover by" table.
	 * 
	 * @param source
	 *            source to export
	 * @param sheet
	 *            sheet where export
	 */
	private void exportCoverBy(final InputRequirementSource source, final XSSFSheet sheet)
	{
		for (final var coverBy : source.getCoversBy().entrySet())
		{
			final var rateRow = addNewRow(sheet, 1);
			final var rateCell = addNewCell(rateRow);
			rateCell.setCellValue(source.getName() + " is covered by " + coverBy.getKey().getName() + " at "
					+ (coverBy.getValue() * 100) + " %");
			addColspan(sheet, rateCell, 2);

			final var tableHeaderRow = addNewRow(sheet);

			final var headerSourceCell = addNewCell(tableHeaderRow);
			headerSourceCell.setCellValue(source.getName());
			headerSourceCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerSourceCell, tableHeaderProperties);

			final var headerCoverCell = addNewCell(tableHeaderRow);
			headerCoverCell.setCellValue(coverBy.getKey().getName());
			headerCoverCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerCoverCell, tableHeaderProperties);

			for (final var req : source.getRequirements())
			{
				final var refByForReq = req.getReferredByRequirementFor(coverBy.getKey());

				if (!refByForReq.isEmpty())
				{
					final var tableReqRow = addNewRow(sheet);

					final var reqSourceCell = addNewCell(tableReqRow);
					reqSourceCell.setCellValue(req.getText());
					CellUtil.setCellStylePropertiesEnum(reqSourceCell, tableProperties);

					final var covSourceCell = addNewCell(tableReqRow);
					covSourceCell.setCellValue(
							refByForReq.stream().map(RequirementImpl::getText).collect(Collectors.joining("\n")));
					covSourceCell.setCellStyle(wrapped);
					CellUtil.setCellStylePropertiesEnum(covSourceCell, tableProperties);
				}
			}
		}
	}

	/**
	 * Export the "unknown reference" table.
	 * 
	 * @param source
	 *            source to export
	 * @param sheet
	 *            sheet where export
	 */
	private void exportUnknownReferences(final InputRequirementSource source, final XSSFSheet sheet)
	{
		final var unknownReferences = source.getAllUknownReferences();
		if (!unknownReferences.isEmpty())
		{
			final Set<String> refAttributes = new LinkedHashSet<>();
			// ID in first
			refAttributes.add(SourceConfiguration.ATTRIBUTE_ID);
			// Version in second if defined
			if (source.getReferenceAttributes().contains(SourceConfiguration.ATTRIBUTE_VERSION))
			{
				refAttributes.add(SourceConfiguration.ATTRIBUTE_VERSION);
			}
			refAttributes.addAll(source.getReferenceAttributes());
			// Text is a special case
			refAttributes.remove(SourceConfiguration.ATTRIBUTE_TEXT);

			final var counterRow = addNewRow(sheet, 1);
			final var counterCell = addNewCell(counterRow);
			counterCell.setCellValue("Total: " + unknownReferences.size());
			addColspan(sheet, counterCell, refAttributes.size());

			final var tableHeaderRow = addNewRow(sheet);

			final var headerSourceCell = addNewCell(tableHeaderRow);
			headerSourceCell.setCellValue(source.getName());
			headerSourceCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerSourceCell, tableHeaderProperties);

			final var headerUnknownRefCell = addNewCell(tableHeaderRow);
			headerUnknownRefCell.setCellValue("Unknown reference");
			headerUnknownRefCell.setCellStyle(tableHeader);
			CellUtil.setCellStylePropertiesEnum(headerUnknownRefCell, tableProperties);
			CellUtil.setCellStylePropertiesEnum(addNewCell(tableHeaderRow, refAttributes.size() - 2), tableProperties);
			addColspan(sheet, headerUnknownRefCell, refAttributes.size());

			final var tableHeaderRow2 = addNewRow(sheet);
			CellUtil.setCellStylePropertiesEnum(addNewCell(tableHeaderRow2), tableHeaderProperties);

			for (final var attribute : refAttributes)
			{
				final var headerRefAttributeCell = addNewCell(tableHeaderRow2);
				headerRefAttributeCell.setCellValue(attribute);
				headerRefAttributeCell.setCellStyle(tableHeader);
				CellUtil.setCellStylePropertiesEnum(headerRefAttributeCell, tableHeaderProperties);
			}

			addRowspan(sheet, headerSourceCell, 2);

			var tableRow = addNewRow(sheet);
			for (final var req : source.getRequirements())
			{
				final var refRequirements = req.getReferencesFor(null);

				if (!refRequirements.isEmpty())
				{
					final var reqCell = addNewCell(tableRow);
					reqCell.setCellValue(req.getText());
					CellUtil.setCellStylePropertiesEnum(reqCell, tableProperties);

					var skip = 0;
					for (final var ref : refRequirements)
					{
						for (int i = 0; i < skip; i++)
						{
							CellUtil.setCellStylePropertiesEnum(addNewCell(tableRow), tableProperties);
						}
						for (final var attribute : refAttributes)
						{
							final var refAttrituteCell = addNewCell(tableRow);
							refAttrituteCell.setCellValue(ref.getAttribute(attribute));
							CellUtil.setCellStylePropertiesEnum(refAttrituteCell, tableProperties);
						}
						skip = reqCell.getColumnIndex() + 1;
						tableRow = addNewRow(sheet);
					}

					addRowspan(sheet, reqCell, refRequirements.size());
				}
			}
		}
	}
}
