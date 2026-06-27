package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.dto.attendance.MemberMonthlyAttendanceDTO;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.model.*;
import org.nr31.backend.repository.*;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.EventAttendanceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RosterExcelExporter {

    private final AppConfigService appConfigService;
    private final FileMetadataRepository fileMetadataRepository;
    private final RosterMemberRepository rosterMemberRepository;
    private final MonthlyEventCountRepository monthlyEventCountRepository;
    private final ObjectProvider<FileSystem> fileSystemProvider;
    private final EventAttendanceService eventAttendanceService;

    @Value("${app.uploads.dir:/app/uploads}")
    private String uploadDirStr;

    private Path getUploadDir() {
        FileSystem fs = fileSystemProvider.getIfAvailable(FileSystems::getDefault);
        return fs.getPath(uploadDirStr).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public void exportToExcel(OutputStream out) {
        AppConfigDto config = appConfigService.getConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isNull()) {
            throw new ElementNotFoundException("Roster export template file is not configured", ErrorCode.ELEMENT_NOT_FOUND);
        }

        UUID fileId;
        try {
            fileId = UUID.fromString(config.getConfigValue().asString());
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Roster export template file ID is not a valid UUID", e);
        }

        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("Roster template file metadata not found", ErrorCode.FILE_NOT_FOUND));

        Path filePath = getUploadDir().resolve(metadata.getStoredName());
        if (!Files.exists(filePath)) {
            throw new FileStorageException("Roster template physical file not found on disk");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("roster_template", ".xlsx");
            Files.copy(filePath, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            try (Workbook workbook = new XSSFWorkbook(tempFile.toFile())) {

                List<RosterMember> members = rosterMemberRepository.findAll();
                members.sort(Comparator.comparing(RosterMember::isArchived)
                        .thenComparing(m -> m.getSequenceNumber() != null ? m.getSequenceNumber() : Integer.MAX_VALUE)
                        .thenComparing(RosterMember::getMbNickname));

                Sheet registrySheet = workbook.getSheetAt(0);
                populateRegistrySheet(registrySheet, members);

                if (workbook.getNumberOfSheets() > 1) {
                    Sheet attendanceSheet = workbook.getSheetAt(1);
                    populateAttendanceSheet(attendanceSheet, members);
                }

                if (workbook.getNumberOfSheets() > 2) {
                    Sheet trainingSheet = workbook.getSheetAt(2);
                    populateTrainingSheet(trainingSheet, members);
                }

                workbook.write(out);

            }
        } catch (Exception e) {
            throw new FileStorageException("Failed to generate export Excel file", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.error("Failed to delete temp roster template file: {}", tempFile, e);
                }
            }
        }
    }

    private void populateRegistrySheet(Sheet sheet, List<RosterMember> members) {
        Row prototypeRow = sheet.getRow(3); // Row 4 in Excel (first data row)
        if (prototypeRow == null) {
            log.warn("Registry template does not have a prototype row at index 3");
            return;
        }

        Row headerRow = sheet.getRow(2);
        Map<String, Integer> awardAbbrToCol = new HashMap<>();
        if (headerRow != null) {
            for (int col = 12; col <= 20; col++) {
                Cell cell = headerRow.getCell(col);
                String val = getCellValueAsString(cell).trim();
                if (!val.isEmpty()) {
                    awardAbbrToCol.put(val, col);
                }
            }
        }

        int writeRowIdx = 3;
        int rowCounter = 1;

        Workbook wb = sheet.getWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        Cell prototypeDateCell = prototypeRow.getCell(9); // Col J
        if (prototypeDateCell != null) {
            dateStyle.cloneStyleFrom(prototypeDateCell.getCellStyle());
        } else {
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        }

        for (RosterMember member : members) {
            if (member.isArchived()) {
                continue; // Sheets 1 and 2 only contain active members
            }

            Row row = sheet.getRow(writeRowIdx);
            if (row == null) {
                row = sheet.createRow(writeRowIdx);
            }

            // Copy formatting from prototype row for all columns
            for (int col = 0; col < prototypeRow.getLastCellNum(); col++) {
                Cell prototypeCell = prototypeRow.getCell(col);
                if (prototypeCell != null) {
                    Cell targetCell = row.getCell(col);
                    if (targetCell == null) {
                        targetCell = row.createCell(col);
                    }
                    targetCell.setCellStyle(prototypeCell.getCellStyle());

                    // If it is a formula cell, copy the formula and adjust it
                    if (prototypeCell.getCellType() == CellType.FORMULA) {
                        String formula = prototypeCell.getCellFormula();
                        String adjusted = adjustFormula(formula, 4, writeRowIdx + 1);
                        targetCell.setCellFormula(adjusted);
                    }
                }
            }

            // Write values
            writeCellValue(row, 0, rowCounter++); // Col A
            writeCellValue(row, 1, member.getSequenceNumber()); // Col B
            writeCellValue(row, 2, member.getMbNickname()); // Col C
            writeCellValue(row, 3, member.getNationalityFlag() != null && member.getNationalityFlag().getCountryCode() != null
                    ? member.getNationalityFlag().getCountryCode() : ""); // Col D
            writeCellValue(row, 4, member.getDiscordNickname()); // Col E
            writeCellValue(row, 5, member.getDiscordId()); // Col F
            writeCellValue(row, 6, member.getSpecialty() != null ? member.getSpecialty().getName() : ""); // Col G

            // Rank Col H
            if (member.getRank() != null) {
                String rankStr = String.format("(%s) %s", member.getRank().getAbbreviation(), member.getRank().getName());
                writeCellValue(row, 7, rankStr);
            } else {
                writeCellValue(row, 7, "");
            }

            // Col J Join Date
            Cell joinCell = row.getCell(9);
            if (joinCell == null) joinCell = row.createCell(9);
            if (member.getJoinDate() != null) {
                joinCell.setCellValue(Date.from(member.getJoinDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                joinCell.setCellStyle(dateStyle);
            } else {
                joinCell.setCellValue((String) null);
            }

            // Col L Penalties
            writeCellValue(row, 11, member.getPenalties() != null ? member.getPenalties().doubleValue() : 0.0);

            // Col M-U Awards
            for (Map.Entry<String, Integer> entry : awardAbbrToCol.entrySet()) {
                int col = entry.getValue();
                Cell awCell = row.getCell(col);
                if (awCell == null) awCell = row.createCell(col);

                Optional<MemberAward> maOpt = member.getAwards().stream()
                        .filter(ma -> ma.getAward() != null && entry.getKey().equals(ma.getAward().getAbbreviation()))
                        .findFirst();

                if (maOpt.isPresent() && maOpt.get().getAwardedDate() != null) {
                    awCell.setCellValue(Date.from(maOpt.get().getAwardedDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    awCell.setCellStyle(dateStyle);
                } else {
                    awCell.setCellValue((String) null);
                }
            }

            writeRowIdx++;
        }

        // Delete any remaining rows in the template if the new list is smaller than the template's dummy rows
        for (int r = writeRowIdx; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private void populateAttendanceSheet(Sheet sheet, List<RosterMember> members) {
        Row prototypeRow = sheet.getRow(6); // Row 7 in Excel (first data row)
        Row monthHeaderRow = sheet.getRow(5); // Month header
        if (prototypeRow == null || monthHeaderRow == null) return;

        Map<YearMonth, Integer> monthToCol = new HashMap<>();
        for (int col = 3; col < monthHeaderRow.getLastCellNum(); col++) {
            YearMonth ym = parseYearMonth(monthHeaderRow.getCell(col));
            if (ym != null) {
                monthToCol.put(ym, col);
            }
        }

        // Fill monthly event counts in Row 2 (0-indexed)
        Row eventCountRow = sheet.getRow(2);
        if (eventCountRow != null) {
            for (Map.Entry<YearMonth, Integer> entry : monthToCol.entrySet()) {
                Cell ecCell = eventCountRow.getCell(entry.getValue());
                if (ecCell == null) ecCell = eventCountRow.createCell(entry.getValue());

                Optional<MonthlyEventCount> mecOpt = monthlyEventCountRepository.findByYearAndMonth(
                        entry.getKey().getYear(), entry.getKey().getMonthValue());

                if (mecOpt.isPresent()) {
                    ecCell.setCellValue(mecOpt.get().getManualEventCount());
                }
            }
        }

        int writeRowIdx = 6;

        for (RosterMember member : members) {
            if (member.isArchived()) continue;

            Row row = sheet.getRow(writeRowIdx);
            if (row == null) {
                row = sheet.createRow(writeRowIdx);
            }

            // Copy prototype cells
            for (int col = 0; col < monthHeaderRow.getLastCellNum(); col++) {
                Cell prototypeCell = prototypeRow.getCell(col);
                if (prototypeCell != null) {
                    Cell targetCell = row.getCell(col);
                    if (targetCell == null) {
                        targetCell = row.createCell(col);
                    }
                    targetCell.setCellStyle(prototypeCell.getCellStyle());

                    if (prototypeCell.getCellType() == CellType.FORMULA) {
                        String formula = prototypeCell.getCellFormula();
                        String adjusted = adjustFormula(formula, 7, writeRowIdx + 1);
                        targetCell.setCellFormula(adjusted);
                    }
                }
            }

            // Write details
            writeCellValue(row, 0, member.getUnitType() != null && member.getUnitType().getName() != null
                    ? member.getUnitType().getName().getOrDefault("en", "") : ""); // Col A
            writeCellValue(row, 1, member.getMbNickname()); // Col B

            // Col D+ Attendance values
            for (Map.Entry<YearMonth, Integer> entry : monthToCol.entrySet()) {
                int col = entry.getValue();
                Cell attCell = row.getCell(col);
                if (attCell == null) attCell = row.createCell(col);

                MemberMonthlyAttendanceDTO monthlyDto = eventAttendanceService.getMemberMonthlyAttendance(
                        member.getId(), entry.getKey().getYear(), entry.getKey().getMonthValue());

                if (monthlyDto.getTotalScore() > 0 || monthlyDto.getManualAttendanceCount() > 0) {
                    attCell.setCellValue(monthlyDto.getTotalScore());
                } else if (monthlyDto.getStatus() != null) {
                    switch (monthlyDto.getStatus()) {
                        case VACATION -> attCell.setCellValue("V");
                        case MILITARY_SERVICE -> attCell.setCellValue("M");
                        case EXCUSED -> attCell.setCellValue("P");
                        case NOT_IN_REGIMENT -> attCell.setCellValue("/");
                    }
                } else {
                    attCell.setCellValue("");
                }
            }

            writeRowIdx++;
        }

        // Clear trailing rows
        for (int r = writeRowIdx; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) sheet.removeRow(row);
        }
    }

    private void populateTrainingSheet(Sheet sheet, List<RosterMember> members) {
        Row prototypeRow = sheet.getRow(4); // Row 5 in Excel (index 4)
        Row headerRow = sheet.getRow(3);
        if (prototypeRow == null || headerRow == null) return;

        Map<String, Integer> disciplineToCol = new HashMap<>();
        for (int col = 1; col <= 6; col++) {
            String name = getCellValueAsString(headerRow.getCell(col)).trim();
            if (!name.isEmpty()) {
                disciplineToCol.put(name, col);
            }
        }

        int writeRowIdx = 4;

        for (RosterMember member : members) {
            Row row = sheet.getRow(writeRowIdx);
            if (row == null) {
                row = sheet.createRow(writeRowIdx);
            }

            // Copy styling
            for (int col = 0; col < prototypeRow.getLastCellNum(); col++) {
                Cell prototypeCell = prototypeRow.getCell(col);
                if (prototypeCell != null) {
                    Cell targetCell = row.getCell(col);
                    if (targetCell == null) {
                        targetCell = row.createCell(col);
                    }
                    targetCell.setCellStyle(prototypeCell.getCellStyle());
                }
            }

            // Write values
            writeCellValue(row, 0, member.getMbNickname()); // Col A

            // Col B-G scores
            for (Map.Entry<String, Integer> entry : disciplineToCol.entrySet()) {
                int col = entry.getValue();
                Cell scCell = row.getCell(col);
                if (scCell == null) scCell = row.createCell(col);

                Optional<MemberTrainingScore> scoreOpt = member.getTrainingScores().stream()
                        .filter(ts -> ts.getDiscipline() != null && entry.getKey().equals(ts.getDiscipline().getName()))
                        .findFirst();

                if (scoreOpt.isPresent()) {
                    scCell.setCellValue(scoreOpt.get().getScore());
                } else {
                    scCell.setCellValue((String) null);
                }
            }

            // Col H Archived
            Cell archCell = row.getCell(7);
            if (archCell == null) archCell = row.createCell(7);
            archCell.setCellValue(member.isArchived());

            // Col I Notes
            writeCellValue(row, 8, member.getTrainingNotes());

            writeRowIdx++;
        }

        // Clear trailing rows
        for (int r = writeRowIdx; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) sheet.removeRow(row);
        }
    }

    private void writeCellValue(Row row, int col, Object value) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            cell = row.createCell(col);
        }
        if (value == null) {
            cell.setCellValue((String) null);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        }
    }

    private String adjustFormula(String formula, int srcRow, int destRow) {
        String regexSrc = "(?<!\\d)" + srcRow + "(?!\\d)";
        String result = formula.replaceAll(regexSrc, String.valueOf(destRow));

        int srcRowSheet2 = srcRow + 3;
        int destRowSheet2 = destRow + 3;
        String regexSrc2 = "(?<!\\d)" + srcRowSheet2 + "(?!\\d)";
        result = result.replaceAll(regexSrc2, String.valueOf(destRowSheet2));

        return result;
    }

    private YearMonth parseYearMonth(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
            return YearMonth.of(date.getYear(), date.getMonthValue());
        } else {
            String val = getCellValueAsString(cell).trim();
            if (!val.isEmpty()) {
                // Try format MM.YYYY or MM/YYYY
                Pattern p = Pattern.compile("^(\\d{2})[./-](\\d{4})$");
                Matcher m = p.matcher(val);
                if (m.matches()) {
                    return YearMonth.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
                }

                // Try Ukrainian month names e.g. "Січень 2026"
                Pattern pUk = Pattern.compile("^([А-Яа-яІіЇїЄє]+)\\s+(\\d{4})$");
                Matcher mUk = pUk.matcher(val);
                if (mUk.matches()) {
                    int month = mapUkrainianMonth(mUk.group(1));
                    if (month > 0) {
                        return YearMonth.of(Integer.parseInt(mUk.group(2)), month);
                    }
                }
            }
        }
        return null;
    }

    private int mapUkrainianMonth(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("січ")) return 1;
        if (lower.startsWith("лют")) return 2;
        if (lower.startsWith("берез")) return 3;
        if (lower.startsWith("квіт")) return 4;
        if (lower.startsWith("трав")) return 5;
        if (lower.startsWith("черв")) return 6;
        if (lower.startsWith("лип")) return 7;
        if (lower.startsWith("серп")) return 8;
        if (lower.startsWith("верес")) return 9;
        if (lower.startsWith("жовт")) return 10;
        if (lower.startsWith("листоп")) return 11;
        if (lower.startsWith("груд")) return 12;
        return 0;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        double valForm = cell.getNumericCellValue();
                        if (valForm == (long) valForm) {
                            return String.valueOf((long) valForm);
                        }
                        return String.valueOf(valForm);
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
}
