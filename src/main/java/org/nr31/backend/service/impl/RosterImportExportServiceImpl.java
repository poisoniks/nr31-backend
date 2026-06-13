package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.model.*;
import org.nr31.backend.repository.*;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.FileStorageService;
import org.nr31.backend.service.RosterImportExportService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.time.YearMonth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RosterImportExportServiceImpl implements RosterImportExportService {

    private final SpecialtyRepository specialtyRepository;
    private final RankRepository rankRepository;
    private final AwardRepository awardRepository;
    private final TrainingDisciplineRepository trainingDisciplineRepository;
    private final RosterMemberRepository rosterMemberRepository;
    private final MemberAwardRepository memberAwardRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final MonthlyEventCountRepository monthlyEventCountRepository;
    private final MemberTrainingScoreRepository memberTrainingScoreRepository;
    private final FileStorageService fileStorageService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final UnitTypeRepository unitTypeRepository;
    private final AppConfigService appConfigService;

    private final ObjectProvider<FileSystem> fileSystemProvider;

    @Value("${app.uploads.dir:/app/uploads}")
    private String uploadDirStr;

    private Path getUploadDir() {
        FileSystem fs = fileSystemProvider.getIfAvailable(FileSystems::getDefault);
        return fs.getPath(uploadDirStr).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void importFromExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Roster Excel file is empty");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Maps to hold parsed reference entities to be saved or resolved
            Map<String, Specialty> parsedSpecialties = new HashMap<>();
            Map<String, Rank> parsedRanks = new HashMap<>();
            Map<String, Award> parsedAwards = new HashMap<>();
            Map<String, TrainingDiscipline> parsedDisciplines = new HashMap<>();

            // Main maps to link data by mb_nickname
            Map<String, RosterMember> membersByNickname = new LinkedHashMap<>();
            Map<String, String> memberUnits = new HashMap<>();
            List<ParsedAttendance> parsedAttendanceList = new ArrayList<>();
            List<ParsedTraining> parsedTrainingList = new ArrayList<>();
            List<ParsedMonthlyEventCount> parsedMonthlyEvents = new ArrayList<>();

            // 1. PARSE SHEET 1: Реєстр
            Sheet registrySheet = workbook.getSheetAt(0);
            parseRegistrySheet(registrySheet, parsedSpecialties, parsedRanks, parsedAwards, membersByNickname);

            // 2. PARSE SHEET 2: Відвідування
            if (workbook.getNumberOfSheets() > 1) {
                Sheet attendanceSheet = workbook.getSheetAt(1);
                parseAttendanceSheet(attendanceSheet, membersByNickname, memberUnits, parsedAttendanceList, parsedMonthlyEvents);
            }

            // 3. PARSE SHEET 3: Підготовка
            if (workbook.getNumberOfSheets() > 2) {
                Sheet trainingSheet = workbook.getSheetAt(2);
                parseTrainingSheet(trainingSheet, membersByNickname, parsedDisciplines, parsedTrainingList);
            }

            // --- SAVE DATABASE TRANSACTIONS ---
            // Wipe existing roster data in correct order to prevent FK violations
            memberTrainingScoreRepository.deleteAllInBatch();
            attendanceRecordRepository.deleteAllInBatch();
            monthlyEventCountRepository.deleteAllInBatch();
            memberAwardRepository.deleteAllInBatch();
            rosterMemberRepository.deleteAllInBatch();
            rankRepository.deleteAllInBatch();
            specialtyRepository.deleteAllInBatch();
            awardRepository.deleteAllInBatch();
            trainingDisciplineRepository.deleteAllInBatch();

            // Save new Reference tables
            Map<String, Specialty> savedSpecialties = new HashMap<>();
            for (Specialty spec : parsedSpecialties.values()) {
                savedSpecialties.put(spec.getName(), specialtyRepository.save(spec));
            }

            Map<String, Rank> savedRanks = new HashMap<>();
            for (Rank r : parsedRanks.values()) {
                if (r.getSpecialty() != null) {
                    r.setSpecialty(savedSpecialties.get(r.getSpecialty().getName()));
                }
                savedRanks.put(r.getAbbreviation(), rankRepository.save(r));
            }

            Map<String, Award> savedAwards = new HashMap<>();
            for (Award a : parsedAwards.values()) {
                savedAwards.put(a.getAbbreviation(), awardRepository.save(a));
            }

            Map<String, TrainingDiscipline> savedDisciplines = new HashMap<>();
            for (TrainingDiscipline d : parsedDisciplines.values()) {
                savedDisciplines.put(d.getName(), trainingDisciplineRepository.save(d));
            }

            // Resolve unit types from DB
            List<UnitType> allUnitTypes = unitTypeRepository.findAll();

            // Save core RosterMembers
            Map<String, RosterMember> savedMembers = new HashMap<>();
            for (RosterMember member : membersByNickname.values()) {
                // Link Specialty
                if (member.getSpecialty() != null) {
                    member.setSpecialty(savedSpecialties.get(member.getSpecialty().getName()));
                }
                // Link Rank
                if (member.getRank() != null) {
                    member.setRank(savedRanks.get(member.getRank().getAbbreviation()));
                }
                // Link UnitType (Division) from Sheet 2
                String unitName = memberUnits.get(member.getMbNickname());
                if (unitName != null) {
                    member.setUnitType(findMatchingUnitType(unitName, allUnitTypes));
                }
                // Link User if username matches discord nickname or mb nickname
                userRepository.findByUsername(member.getDiscordNickname())
                        .or(() -> userRepository.findByUsername(member.getMbNickname()))
                        .ifPresent(member::setUser);

                RosterMember savedMember = rosterMemberRepository.save(member);
                savedMembers.put(savedMember.getMbNickname(), savedMember);
            }

            // Save MemberAwards
            for (RosterMember member : membersByNickname.values()) {
                RosterMember savedMember = savedMembers.get(member.getMbNickname());
                for (MemberAward ma : member.getAwards()) {
                    ma.setMember(savedMember);
                    ma.setAward(savedAwards.get(ma.getAward().getAbbreviation()));
                    memberAwardRepository.save(ma);
                }
            }

            // Save MonthlyEventCounts
            Map<String, MonthlyEventCount> savedMonthlyEvents = new HashMap<>();
            for (ParsedMonthlyEventCount pme : parsedMonthlyEvents) {
                MonthlyEventCount mec = MonthlyEventCount.builder()
                        .year(pme.year)
                        .month(pme.month)
                        .eventCount(pme.eventCount)
                        .build();
                savedMonthlyEvents.put(pme.year + "-" + pme.month, monthlyEventCountRepository.save(mec));
            }

            // Save AttendanceRecords
            for (ParsedAttendance pa : parsedAttendanceList) {
                RosterMember savedMember = savedMembers.get(pa.mbNickname);
                if (savedMember != null) {
                    AttendanceRecord ar = AttendanceRecord.builder()
                            .member(savedMember)
                            .year(pa.year)
                            .month(pa.month)
                            .attendanceCount(pa.attendanceCount)
                            .status(pa.status)
                            .build();
                    attendanceRecordRepository.save(ar);
                }
            }

            // Save MemberTrainingScores
            for (ParsedTraining pt : parsedTrainingList) {
                RosterMember savedMember = savedMembers.get(pt.mbNickname);
                if (savedMember != null) {
                    MemberTrainingScore mts = MemberTrainingScore.builder()
                            .member(savedMember)
                            .discipline(savedDisciplines.get(pt.disciplineName))
                            .score(pt.score)
                            .build();
                    memberTrainingScoreRepository.save(mts);
                }
            }

            log.info("Successfully imported roster from Excel. Wiped and created {} members.", savedMembers.size());

        } catch (IOException e) {
            throw new FileStorageException("Failed to read roster Excel file", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        AppConfigDto config = appConfigService.getConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isNull()) {
            throw new ElementNotFoundException("Roster export template file is not configured", ErrorCode.ELEMENT_NOT_FOUND);
        }

        UUID fileId;
        try {
            fileId = UUID.fromString(config.getConfigValue().asText());
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Roster export template file ID is not a valid UUID", e);
        }

        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("Roster template file metadata not found", ErrorCode.FILE_NOT_FOUND));

        Path filePath = getUploadDir().resolve(metadata.getStoredName());
        if (!Files.exists(filePath)) {
            throw new FileStorageException("Roster template physical file not found on disk");
        }

        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            List<RosterMember> members = rosterMemberRepository.findAll();
            // Sort active members first (not archived) then by sequence number or mbNickname
            members.sort(Comparator.comparing(RosterMember::isArchived)
                    .thenComparing(m -> m.getSequenceNumber() != null ? m.getSequenceNumber() : Integer.MAX_VALUE)
                    .thenComparing(RosterMember::getMbNickname));

            // 1. POPULATE SHEET 1: Реєстр
            Sheet registrySheet = workbook.getSheetAt(0);
            populateRegistrySheet(registrySheet, members);

            // 2. POPULATE SHEET 2: Відвідування
            if (workbook.getNumberOfSheets() > 1) {
                Sheet attendanceSheet = workbook.getSheetAt(1);
                populateAttendanceSheet(attendanceSheet, members);
            }

            // 3. POPULATE SHEET 3: Підготовка
            if (workbook.getNumberOfSheets() > 2) {
                Sheet trainingSheet = workbook.getSheetAt(2);
                populateTrainingSheet(trainingSheet, members);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new FileStorageException("Failed to generate export Excel file", e);
        }
    }

    @Override
    @Transactional
    public void uploadTemplate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Template file cannot be empty");
        }

        // Store file with ATTACHMENT scope so it is kept in system uploads
        FileUploadResponse response = fileStorageService.storeFile(file, "admin", FileScope.ATTACHMENT);
        UUID fileId = response.getId();

        AppConfigDto configDto = AppConfigDto.builder()
                .name(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey())
                .configValue(new ObjectMapper().valueToTree(fileId.toString()))
                .build();

        appConfigService.updateConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey(), configDto);
        log.info("Roster export template uploaded successfully with file metadata ID: {}", fileId);
    }

    // --- PARSING HELPERS ---

    private void parseRegistrySheet(Sheet sheet, Map<String, Specialty> parsedSpecialties,
                                    Map<String, Rank> parsedRanks, Map<String, Award> parsedAwards,
                                    Map<String, RosterMember> membersByNickname) {
        Row headerRow = sheet.getRow(2); // Row 3 in Excel (0-indexed 2)
        if (headerRow == null) {
            throw new FileStorageException("Invalid roster registry sheet structure: missing header row at row 3");
        }

        // Map award columns
        Map<Integer, String> awardCols = new HashMap<>();
        for (int col = 12; col <= 20; col++) { // Columns M-U (index 12-20)
            Cell cell = headerRow.getCell(col);
            String val = getCellValueAsString(cell).trim();
            if (!val.isEmpty()) {
                awardCols.put(col, val);
                final int finalCol = col;
                parsedAwards.computeIfAbsent(val, abbr -> Award.builder()
                        .abbreviation(abbr)
                        .name(abbr)
                        .sortOrder(finalCol)
                        .build());
            }
        }

        int rankSortCounter = 0;
        int specialtySortCounter = 0;

        for (int rowIdx = 3; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            String mbNickname = getCellValueAsString(row.getCell(2)).trim(); // Col C
            if (mbNickname.isEmpty()) continue;

            Integer seqNum = null;
            String seqStr = getCellValueAsString(row.getCell(1)).trim(); // Col B
            if (!seqStr.isEmpty()) {
                try {
                    seqNum = Integer.parseInt(seqStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            String nationality = getCellValueAsString(row.getCell(3)).trim(); // Col D
            String discordNickname = getCellValueAsString(row.getCell(4)).trim(); // Col E
            String discordId = getCellValueAsString(row.getCell(5)).trim(); // Col F

            Specialty specialty = null;
            String specName = getCellValueAsString(row.getCell(6)).trim(); // Col G
            if (!specName.isEmpty()) {
                specialty = parsedSpecialties.get(specName);
                if (specialty == null) {
                    specialty = Specialty.builder()
                            .name(specName)
                            .sortOrder(specialtySortCounter++)
                            .build();
                    parsedSpecialties.put(specName, specialty);
                }
            }

            Rank rank = null;
            String rankStr = getCellValueAsString(row.getCell(7)).trim(); // Col H
            if (!rankStr.isEmpty()) {
                rank = parseRankString(rankStr, parsedRanks, specialty, rankSortCounter++);
            }

            LocalDate joinDate = null;
            Cell joinCell = row.getCell(9); // Col J
            if (joinCell != null) {
                joinDate = parseLocalDate(joinCell);
            }

            BigDecimal penalties = BigDecimal.ZERO;
            Cell penaltyCell = row.getCell(11); // Col L
            if (penaltyCell != null) {
                try {
                    if (penaltyCell.getCellType() == CellType.NUMERIC) {
                        penalties = BigDecimal.valueOf(penaltyCell.getNumericCellValue());
                    } else if (penaltyCell.getCellType() == CellType.STRING) {
                        String s = penaltyCell.getStringCellValue().trim();
                        if (!s.isEmpty()) penalties = new BigDecimal(s);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            RosterMember member = RosterMember.builder()
                    .sequenceNumber(seqNum)
                    .mbNickname(mbNickname)
                    .nationality(nationality)
                    .discordNickname(discordNickname)
                    .discordId(discordId)
                    .specialty(specialty)
                    .rank(rank)
                    .joinDate(joinDate)
                    .penalties(penalties)
                    .isArchived(false) // Default active, Sheet 3 can mark archived
                    .awards(new ArrayList<>())
                    .trainingScores(new ArrayList<>())
                    .attendanceRecords(new ArrayList<>())
                    .build();

            // Read awards
            for (Map.Entry<Integer, String> awardEntry : awardCols.entrySet()) {
                Cell awardCell = row.getCell(awardEntry.getKey());
                if (awardCell != null && awardCell.getCellType() != CellType.BLANK) {
                    LocalDate awardedDate = parseLocalDate(awardCell);
                    if (awardedDate == null) {
                        awardedDate = joinDate != null ? joinDate : LocalDate.now();
                    }
                    Award aw = parsedAwards.get(awardEntry.getValue());
                    MemberAward ma = MemberAward.builder()
                            .award(aw)
                            .awardedDate(awardedDate)
                            .build();
                    member.getAwards().add(ma);
                }
            }

            membersByNickname.put(mbNickname, member);
        }
    }

    private void parseAttendanceSheet(Sheet sheet, Map<String, RosterMember> membersByNickname,
                                      Map<String, String> memberUnits, List<ParsedAttendance> parsedAttendanceList,
                                      List<ParsedMonthlyEventCount> parsedMonthlyEvents) {
        Row monthHeaderRow = sheet.getRow(5); // Row 6 in Excel (0-indexed 5)
        Row eventCountRow = sheet.getRow(2);  // Row 3 in Excel (0-indexed 2)
        if (monthHeaderRow == null) return;

        Map<Integer, YearMonth> monthCols = new HashMap<>();

        // Parse month columns starting at Col D (index 3)
        for (int col = 3; col < monthHeaderRow.getLastCellNum(); col++) {
            Cell monthCell = monthHeaderRow.getCell(col);
            YearMonth ym = parseYearMonth(monthCell);
            if (ym != null) {
                monthCols.put(col, ym);

                // Read monthly event count if exists
                int count = 0;
                if (eventCountRow != null) {
                    Cell ecCell = eventCountRow.getCell(col);
                    if (ecCell != null && ecCell.getCellType() == CellType.NUMERIC) {
                        count = (int) ecCell.getNumericCellValue();
                    } else if (ecCell != null && ecCell.getCellType() == CellType.STRING) {
                        try {
                            count = Integer.parseInt(ecCell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                parsedMonthlyEvents.add(new ParsedMonthlyEventCount(ym.getYear(), ym.getMonthValue(), count));
            }
        }

        // Parse members starting from Row 6 (0-indexed)
        for (int rowIdx = 6; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            String unitName = getCellValueAsString(row.getCell(0)).trim(); // Col A
            String mbNickname = getCellValueAsString(row.getCell(1)).trim(); // Col B
            if (mbNickname.isEmpty()) continue;

            if (!unitName.isEmpty()) {
                memberUnits.put(mbNickname, unitName);
            }

            for (Map.Entry<Integer, YearMonth> entry : monthCols.entrySet()) {
                Cell cell = row.getCell(entry.getKey());
                if (cell == null || cell.getCellType() == CellType.BLANK) continue;

                Integer attCount = null;
                AttendanceStatus status = null;

                if (cell.getCellType() == CellType.NUMERIC) {
                    attCount = (int) cell.getNumericCellValue();
                } else if (cell.getCellType() == CellType.STRING) {
                    String val = cell.getStringCellValue().trim();
                    if (!val.isEmpty()) {
                        String upper = val.toUpperCase();
                        if (upper.startsWith("V")) {
                            status = AttendanceStatus.VACATION;
                        } else if (upper.startsWith("M")) {
                            status = AttendanceStatus.MILITARY_SERVICE;
                        } else if (upper.startsWith("P")) {
                            status = AttendanceStatus.EXCUSED;
                        } else if (upper.equals("/")) {
                            status = AttendanceStatus.NOT_IN_REGIMENT;
                        } else {
                            try {
                                attCount = Integer.parseInt(val);
                            } catch (NumberFormatException e) {
                                // Ignore or treat as error
                            }
                        }
                    }
                }
                if (attCount != null || status != null) {
                    parsedAttendanceList.add(new ParsedAttendance(mbNickname, entry.getValue().getYear(), entry.getValue().getMonthValue(), attCount, status));
                }
            }
        }
    }

    private void parseTrainingSheet(Sheet sheet, Map<String, RosterMember> membersByNickname,
                                    Map<String, TrainingDiscipline> parsedDisciplines, List<ParsedTraining> parsedTrainingList) {
        Row headerRow = sheet.getRow(3); // Row 4 in Excel (index 3)
        if (headerRow == null) return;

        Map<Integer, String> disciplineCols = new HashMap<>();
        // Disciplines are in Col B-G (indices 1-6)
        for (int col = 1; col <= 6; col++) {
            Cell cell = headerRow.getCell(col);
            String name = getCellValueAsString(cell).trim();
            if (!name.isEmpty()) {
                disciplineCols.put(col, name);
                boolean isMandatory = (col <= 3); // Cols B, C, D (indices 1, 2, 3) are mandatory
                final int finalCol = col;
                parsedDisciplines.computeIfAbsent(name, n -> TrainingDiscipline.builder()
                        .name(n)
                        .isMandatory(isMandatory)
                        .sortOrder(finalCol)
                        .build());
            }
        }

        // Data starts at Row 4 (index 4)
        for (int rowIdx = 4; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            String mbNickname = getCellValueAsString(row.getCell(0)).trim(); // Col A
            if (mbNickname.isEmpty()) continue;

            boolean isArchived = false;
            Cell archCell = row.getCell(7); // Col H
            if (archCell != null) {
                if (archCell.getCellType() == CellType.BOOLEAN) {
                    isArchived = archCell.getBooleanCellValue();
                } else if (archCell.getCellType() == CellType.STRING) {
                    String val = archCell.getStringCellValue().trim();
                    isArchived = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("так");
                }
            }

            String notes = getCellValueAsString(row.getCell(8)).trim(); // Col I

            RosterMember member = membersByNickname.get(mbNickname);
            if (member == null) {
                // Member not in Sheet 1 (Registry). Create an archived stub!
                member = RosterMember.builder()
                        .mbNickname(mbNickname)
                        .isArchived(isArchived)
                        .trainingNotes(notes)
                        .awards(new ArrayList<>())
                        .trainingScores(new ArrayList<>())
                        .attendanceRecords(new ArrayList<>())
                        .build();
                membersByNickname.put(mbNickname, member);
            } else {
                // Update archived and notes status
                member.setArchived(isArchived);
                if (!notes.isEmpty()) {
                    member.setTrainingNotes(notes);
                }
            }

            for (Map.Entry<Integer, String> entry : disciplineCols.entrySet()) {
                Cell cell = row.getCell(entry.getKey());
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    int score = (int) cell.getNumericCellValue();
                    if (score >= 1 && score <= 5) {
                        parsedTrainingList.add(new ParsedTraining(mbNickname, entry.getValue(), score));
                    }
                } else if (cell != null && cell.getCellType() == CellType.STRING) {
                    try {
                        int score = Integer.parseInt(cell.getStringCellValue().trim());
                        if (score >= 1 && score <= 5) {
                            parsedTrainingList.add(new ParsedTraining(mbNickname, entry.getValue(), score));
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private Rank parseRankString(String val, Map<String, Rank> parsedRanks, Specialty specialty, int sortOrder) {
        // Parse "(Abbr) Name" e.g., "(Aa) Оберст"
        Pattern p = Pattern.compile("^\\(([^)]+)\\)\\s*(.*)$");
        Matcher m = p.matcher(val);
        String abbr;
        String name;
        if (m.matches()) {
            abbr = m.group(1).trim();
            name = m.group(2).trim();
        } else {
            abbr = val.length() > 3 ? val.substring(0, 3) : val;
            name = val;
        }

        RankCategory category = determineRankCategory(abbr, name);

        return parsedRanks.computeIfAbsent(abbr, a -> Rank.builder()
                .abbreviation(a)
                .name(name)
                .category(category)
                .specialty(specialty)
                .sortOrder(sortOrder)
                .build());
    }

    private RankCategory determineRankCategory(String abbr, String name) {
        String lowerName = name.toLowerCase();
        String lowerAbbr = abbr.toLowerCase();
        if (lowerName.contains("оберст") || lowerName.contains("лейтенант") || lowerName.contains("майор")
                || lowerName.contains("капітан") || lowerName.contains("шеф") || lowerName.contains("генерал")
                || lowerAbbr.equals("aa") || lowerAbbr.equals("bb") || lowerAbbr.equals("cc")) {
            return RankCategory.OFFICER;
        } else if (lowerName.contains("сержант") || lowerName.contains("капрал") || lowerName.contains("фельдфебель")
                || lowerName.contains("унтер") || lowerName.contains("вахмістр") || lowerAbbr.contains("nco")) {
            return RankCategory.NCO;
        } else {
            return RankCategory.ENLISTED;
        }
    }

    private LocalDate parseLocalDate(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (!val.isEmpty()) {
                // Try format YYYY-MM-DD
                try {
                    return LocalDate.parse(val);
                } catch (DateTimeParseException e) {
                    // Try DD.MM.YYYY
                    try {
                        return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    } catch (DateTimeParseException ex) {
                        return null;
                    }
                }
            }
        }
        return null;
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

    private UnitType findMatchingUnitType(String excelUnitName, List<UnitType> allUnitTypes) {
        return allUnitTypes.stream()
                .filter(ut -> ut.getName() != null && ut.getName().values().stream()
                        .anyMatch(name -> name.toLowerCase().contains(excelUnitName.toLowerCase())
                                || excelUnitName.toLowerCase().contains(name.toLowerCase())))
                .findFirst()
                .orElse(null);
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

    // --- EXPORT HELPERS ---

    private void populateRegistrySheet(Sheet sheet, List<RosterMember> members) {
        Row prototypeRow = sheet.getRow(3); // Row 4 in Excel (first data row)
        if (prototypeRow == null) {
            log.warn("Registry template does not have a prototype row at index 3");
            return;
        }

        // Collect award columns map from headers
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

        // Workbook and Styles
        Workbook wb = sheet.getWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle dateStyle = wb.createCellStyle();
        // Try to locate prototype cell style for dates, else format
        Cell prototypeDateCell = prototypeRow.getCell(9); // Col J
        if (prototypeDateCell != null) {
            dateStyle.cloneStyleFrom(prototypeDateCell.getCellStyle());
        } else {
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        }

        // Fill members
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
            writeCellValue(row, 3, member.getNationality()); // Col D
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
                    ecCell.setCellValue(mecOpt.get().getEventCount());
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

                Optional<AttendanceRecord> arOpt = attendanceRecordRepository.findByMemberIdAndYearAndMonth(
                        member.getId(), entry.getKey().getYear(), entry.getKey().getMonthValue());

                if (arOpt.isPresent()) {
                    AttendanceRecord ar = arOpt.get();
                    if (ar.getAttendanceCount() != null) {
                        attCell.setCellValue(ar.getAttendanceCount());
                    } else if (ar.getStatus() != null) {
                        switch (ar.getStatus()) {
                            case VACATION -> attCell.setCellValue("V");
                            case MILITARY_SERVICE -> attCell.setCellValue("M");
                            case EXCUSED -> attCell.setCellValue("P");
                            case NOT_IN_REGIMENT -> attCell.setCellValue("/");
                        }
                    } else {
                        attCell.setCellValue("");
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

    // --- PARSED STRUCTURES ---

    private static class ParsedAttendance {
        final String mbNickname;
        final int year;
        final int month;
        final Integer attendanceCount;
        final AttendanceStatus status;

        ParsedAttendance(String mbNickname, int year, int month, Integer attendanceCount, AttendanceStatus status) {
            this.mbNickname = mbNickname;
            this.year = year;
            this.month = month;
            this.attendanceCount = attendanceCount;
            this.status = status;
        }
    }

    private static class ParsedTraining {
        final String mbNickname;
        final String disciplineName;
        final int score;

        ParsedTraining(String mbNickname, String disciplineName, int score) {
            this.mbNickname = mbNickname;
            this.disciplineName = disciplineName;
            this.score = score;
        }
    }

    private static class ParsedMonthlyEventCount {
        final int year;
        final int month;
        final int eventCount;

        ParsedMonthlyEventCount(int year, int month, int eventCount) {
            this.year = year;
            this.month = month;
            this.eventCount = eventCount;
        }
    }
}
