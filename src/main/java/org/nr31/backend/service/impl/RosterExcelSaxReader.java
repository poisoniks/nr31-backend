package org.nr31.backend.service.impl;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RosterExcelSaxReader {

    public enum CellType {
        STRING, NUMERIC, BOOLEAN, DATE, BLANK
    }

    @Data
    @Builder
    public static class CellValue {
        private CellType type;
        private String stringValue;
        private Double numericValue;
        private Boolean booleanValue;
        private LocalDate dateValue;

        public String getAsString() {
            if (stringValue != null) return stringValue;
            if (numericValue != null) {
                if (numericValue == numericValue.longValue()) {
                    return String.valueOf(numericValue.longValue());
                }
                return String.valueOf(numericValue);
            }
            if (booleanValue != null) return String.valueOf(booleanValue);
            if (dateValue != null) return dateValue.toString();
            return "";
        }
        
        public boolean isBlank() {
            return type == CellType.BLANK || (stringValue != null && stringValue.trim().isEmpty());
        }
    }

    @Data
    public static class ParsedRow {
        private final int rowIndex;
        private final Map<Integer, CellValue> cells = new HashMap<>();

        public CellValue getCell(int colIndex) {
            return cells.getOrDefault(colIndex, CellValue.builder().type(CellType.BLANK).build());
        }

        public boolean isEmpty() {
            for (CellValue val : cells.values()) {
                if (!val.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }

    @Data
    public static class ParsedSheet {
        private final String sheetName;
        private final List<ParsedRow> rows = new ArrayList<>();
        private final Map<Integer, byte[]> embeddedImagesByRow = new HashMap<>();
        
        public ParsedRow getRow(int rowIndex) {
            for (ParsedRow row : rows) {
                if (row.getRowIndex() == rowIndex) {
                    return row;
                }
            }
            return null;
        }
        
        public int getLastRowNum() {
            int max = -1;
            for (ParsedRow row : rows) {
                if (row.getRowIndex() > max) max = row.getRowIndex();
            }
            return max;
        }
    }

    public List<ParsedSheet> read(InputStream is) throws Exception {
        List<ParsedSheet> sheets = new ArrayList<>();

        try (OPCPackage pkg = OPCPackage.open(is)) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (iter.hasNext()) {
                InputStream stream = iter.next();
                String sheetName = iter.getSheetName();
                ParsedSheet parsedSheet = new ParsedSheet(sheetName);

                SAXParserFactory saxFactory = SAXParserFactory.newInstance();
                saxFactory.setNamespaceAware(true);
                SAXParser saxParser = saxFactory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                
                SheetHandler sheetHandler = new SheetHandler(parsedSheet);
                XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(
                        styles, strings, sheetHandler, false);
                xmlReader.setContentHandler(handler);

                xmlReader.parse(new InputSource(stream));
                stream.close();

                // Parse drawings/images if present
                try {
                    PackagePart sheetPart = iter.getSheetPart();
                    PackageRelationshipCollection drawingRelationships =
                            sheetPart.getRelationshipsByType("http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing");
                    if (drawingRelationships.size() > 0) {
                        PackageRelationship rel = drawingRelationships.getRelationship(0);
                        PackagePart drawingPart = sheetPart.getRelatedPart(rel);
                        Map<Integer, byte[]> images = parseDrawingImages(drawingPart);
                        parsedSheet.getEmbeddedImagesByRow().putAll(images);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse sheet drawings/images for sheet: {}", sheetName, e);
                }

                sheets.add(parsedSheet);
            }
        }
        return sheets;
    }

    private Map<Integer, byte[]> parseDrawingImages(PackagePart drawingPart) throws Exception {
        Map<Integer, byte[]> imagesByRow = new HashMap<>();
        try (InputStream drawingStream = drawingPart.getInputStream()) {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(drawingStream);

            org.w3c.dom.NodeList anchors = doc.getElementsByTagNameNS("*", "from");
            for (int i = 0; i < anchors.getLength(); i++) {
                org.w3c.dom.Element fromElement = (org.w3c.dom.Element) anchors.item(i);
                org.w3c.dom.Element anchorElement = (org.w3c.dom.Element) fromElement.getParentNode();
                if (anchorElement == null) continue;

                org.w3c.dom.NodeList cols = fromElement.getElementsByTagNameNS("*", "col");
                org.w3c.dom.NodeList rows = fromElement.getElementsByTagNameNS("*", "row");
                if (cols.getLength() == 0 || rows.getLength() == 0) continue;

                int col = Integer.parseInt(cols.item(0).getTextContent().trim());
                int row = Integer.parseInt(rows.item(0).getTextContent().trim());

                if (col == 3) { // Nationality column is D (index 3)
                    org.w3c.dom.NodeList blips = anchorElement.getElementsByTagNameNS("*", "blip");
                    if (blips.getLength() > 0) {
                        org.w3c.dom.Element blipElement = (org.w3c.dom.Element) blips.item(0);
                        String rId = blipElement.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed");
                        if (rId == null || rId.isEmpty()) {
                            rId = blipElement.getAttribute("r:embed");
                        }
                        if (rId == null || rId.isEmpty()) {
                            rId = blipElement.getAttribute("embed");
                        }
                        if (rId != null && !rId.isEmpty()) {
                            PackageRelationship imageRel = drawingPart.getRelationship(rId);
                            if (imageRel != null) {
                                PackagePart imagePart = drawingPart.getRelatedPart(imageRel);
                                try (InputStream imageStream = imagePart.getInputStream()) {
                                    byte[] imageBytes = imageStream.readAllBytes();
                                    imagesByRow.put(row, imageBytes);
                                }
                            }
                        }
                    }
                }
            }
        }
        return imagesByRow;
    }

    private static class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final ParsedSheet parsedSheet;
        private ParsedRow currentRow;

        public SheetHandler(ParsedSheet parsedSheet) {
            this.parsedSheet = parsedSheet;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ParsedRow(rowNum);
        }

        @Override
        public void endRow(int rowNum) {
            if (currentRow != null && !currentRow.isEmpty()) {
                parsedSheet.getRows().add(currentRow);
            }
            currentRow = null;
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (currentRow == null || cellReference == null) return;
            
            // Extract column index from reference (e.g. "A1" -> 0, "B2" -> 1)
            int colIndex = getColumnIndex(cellReference);
            
            CellValue.CellValueBuilder builder = CellValue.builder();
            
            if (formattedValue == null || formattedValue.trim().isEmpty()) {
                builder.type(CellType.BLANK);
            } else {
                formattedValue = formattedValue.trim();
                builder.stringValue(formattedValue);
                builder.type(CellType.STRING);
                
                // Try parse numeric
                try {
                    // XSSFSheetXMLHandler handles formatting, so numeric formats might be complex.
                    // If it's a raw number it might parse
                    double d = Double.parseDouble(formattedValue.replace(",", "."));
                    builder.numericValue(d);
                    builder.type(CellType.NUMERIC);
                } catch (NumberFormatException e) {
                    // Try parse boolean
                    if ("true".equalsIgnoreCase(formattedValue) || "false".equalsIgnoreCase(formattedValue)) {
                        builder.booleanValue(Boolean.parseBoolean(formattedValue));
                        builder.type(CellType.BOOLEAN);
                    } else {
                        // Try parse date
                        LocalDate d = parseDateStr(formattedValue);
                        if (d != null) {
                            builder.dateValue(d);
                            builder.type(CellType.DATE);
                        }
                    }
                }
            }
            
            currentRow.getCells().put(colIndex, builder.build());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Unused
        }

        private int getColumnIndex(String cellRef) {
            int colIndex = 0;
            for (int i = 0; i < cellRef.length(); i++) {
                char c = cellRef.charAt(i);
                if (c >= 'A' && c <= 'Z') {
                    colIndex = colIndex * 26 + (c - 'A' + 1);
                } else {
                    break;
                }
            }
            return colIndex - 1;
        }
        
        private LocalDate parseDateStr(String val) {
            // Try DD.MM.YYYY or MM/DD/YY etc if needed, often SAX formatter outputs formatted strings
            // such as "14.12.2024" or "2024-12-14"
            if (val.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    return LocalDate.parse(val);
                } catch (DateTimeParseException ignored) {}
            }
            if (val.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                try {
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } catch (DateTimeParseException ignored) {}
            }
            if (val.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) { // e.g., 04.12.24
                try {
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yy"));
                } catch (DateTimeParseException ignored) {}
            }
            return null;
        }
    }
}
