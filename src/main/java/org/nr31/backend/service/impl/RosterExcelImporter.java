package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.model.*;
import org.nr31.backend.repository.*;
import org.nr31.backend.service.FileStorageService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RosterExcelImporter {

    private final SpecialtyRepository specialtyRepository;
    private final RankRepository rankRepository;
    private final AwardRepository awardRepository;
    private final TrainingDisciplineRepository trainingDisciplineRepository;
    private final RosterMemberRepository rosterMemberRepository;
    private final MemberAwardRepository memberAwardRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final MonthlyEventCountRepository monthlyEventCountRepository;
    private final MemberTrainingScoreRepository memberTrainingScoreRepository;
    private final UserRepository userRepository;
    private final UnitTypeRepository unitTypeRepository;
    private final NationalityFlagRepository nationalityFlagRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void importFromExcel(MultipartFile file, String uploaderUsername) {
        if (file.isEmpty()) {
            throw new FileStorageException("Roster Excel file is empty");
        }

        try (InputStream is = file.getInputStream()) {
            RosterExcelSaxReader reader = new RosterExcelSaxReader();
            List<RosterExcelSaxReader.ParsedSheet> sheets = reader.read(is);

            if (sheets.isEmpty()) {
                throw new FileStorageException("Roster Excel file has no sheets");
            }

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
            Map<String, byte[]> memberNationalityFlags = new HashMap<>();

            // 1. PARSE SHEET 1: Реєстр
            RosterExcelSaxReader.ParsedSheet registrySheet = sheets.get(0);
            parseRegistrySheet(registrySheet, parsedSpecialties, parsedRanks, parsedAwards, membersByNickname, memberNationalityFlags);

            // 2. PARSE SHEET 2: Відвідування
            if (sheets.size() > 1) {
                RosterExcelSaxReader.ParsedSheet attendanceSheet = sheets.get(1);
                parseAttendanceSheet(attendanceSheet, membersByNickname, memberUnits, parsedAttendanceList, parsedMonthlyEvents);
            }

            // 3. PARSE SHEET 3: Підготовка
            if (sheets.size() > 2) {
                RosterExcelSaxReader.ParsedSheet trainingSheet = sheets.get(2);
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
                // Link NationalityFlag
                byte[] flagBytes = memberNationalityFlags.get(member.getMbNickname());
                if (flagBytes != null) {
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hashBytes = digest.digest(flagBytes);
                        String sha256Hash = HexFormat.of().formatHex(hashBytes);

                        Optional<NationalityFlag> existingFlag = nationalityFlagRepository.findByFlagFileStoredName(sha256Hash);
                        NationalityFlag flag;
                        if (existingFlag.isPresent()) {
                            flag = existingFlag.get();
                        } else {
                            String originalName = "flag_" + sha256Hash.substring(0, 8) + ".png";
                            FileUploadResponse response = fileStorageService.storeFile(flagBytes, originalName, "image/png", uploaderUsername, FileScope.LIBRARY);
                            FileMetadata fileMeta = FileMetadata.builder().id(response.getId()).build(); // Stub for FK
                            flag = NationalityFlag.builder()
                                    .flagFile(fileMeta)
                                    .countryCode(null)
                                    .build();
                            flag = nationalityFlagRepository.save(flag);
                        }
                        member.setNationalityFlag(flag);
                    } catch (Exception e) {
                        log.error("Failed to process flag for member: {}", member.getMbNickname(), e);
                    }
                }

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

        } catch (Exception e) {
            log.error("Error parsing roster excel file", e);
            throw new FileStorageException("Failed to read roster Excel file", e);
        }
    }

    private void parseRegistrySheet(RosterExcelSaxReader.ParsedSheet sheet, Map<String, Specialty> parsedSpecialties,
                                    Map<String, Rank> parsedRanks, Map<String, Award> parsedAwards,
                                    Map<String, RosterMember> membersByNickname, Map<String, byte[]> memberNationalityFlags) {
        RosterExcelSaxReader.ParsedRow headerRow = sheet.getRow(2); // Row 3 in Excel (0-indexed 2)
        if (headerRow == null) {
            throw new FileStorageException("Invalid roster registry sheet structure: missing header row at row 3");
        }

        // Map award columns
        Map<Integer, String> awardCols = new HashMap<>();
        for (int col = 12; col <= 20; col++) { // Columns M-U (index 12-20)
            RosterExcelSaxReader.CellValue cell = headerRow.getCell(col);
            String val = cell.getAsString().trim();
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
            RosterExcelSaxReader.ParsedRow row = sheet.getRow(rowIdx);
            if (row == null || row.isEmpty()) continue;

            String mbNickname = row.getCell(2).getAsString().trim(); // Col C
            if (mbNickname.isEmpty()) continue;

            Integer seqNum = null;
            String seqStr = row.getCell(1).getAsString().trim(); // Col B
            if (!seqStr.isEmpty()) {
                try {
                    seqNum = Integer.parseInt(seqStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            byte[] flagBytes = sheet.getEmbeddedImagesByRow().get(rowIdx);
            if (flagBytes != null) {
                memberNationalityFlags.put(mbNickname, flagBytes);
            }
            String discordNickname = row.getCell(4).getAsString().trim(); // Col E
            String discordId = row.getCell(5).getAsString().trim(); // Col F

            Specialty specialty = null;
            String specName = row.getCell(6).getAsString().trim(); // Col G
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
            String rankStr = row.getCell(7).getAsString().trim(); // Col H
            if (!rankStr.isEmpty()) {
                rank = parseRankString(rankStr, parsedRanks, specialty, rankSortCounter++);
            }

            LocalDate joinDate = row.getCell(9).getDateValue(); // Col J

            BigDecimal penalties = BigDecimal.ZERO;
            RosterExcelSaxReader.CellValue penaltyCell = row.getCell(11); // Col L
            if (!penaltyCell.isBlank()) {
                try {
                    if (penaltyCell.getType() == RosterExcelSaxReader.CellType.NUMERIC) {
                        penalties = BigDecimal.valueOf(penaltyCell.getNumericValue());
                    } else if (penaltyCell.getType() == RosterExcelSaxReader.CellType.STRING) {
                        String s = penaltyCell.getStringValue().trim();
                        if (!s.isEmpty()) penalties = new BigDecimal(s);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            RosterMember member = RosterMember.builder()
                    .sequenceNumber(seqNum)
                    .mbNickname(mbNickname)
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
                RosterExcelSaxReader.CellValue awardCell = row.getCell(awardEntry.getKey());
                if (!awardCell.isBlank()) {
                    LocalDate awardedDate = awardCell.getDateValue();
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

    private void parseAttendanceSheet(RosterExcelSaxReader.ParsedSheet sheet, Map<String, RosterMember> membersByNickname,
                                      Map<String, String> memberUnits, List<ParsedAttendance> parsedAttendanceList,
                                      List<ParsedMonthlyEventCount> parsedMonthlyEvents) {
        RosterExcelSaxReader.ParsedRow monthHeaderRow = sheet.getRow(5); // Row 6 in Excel (0-indexed 5)
        RosterExcelSaxReader.ParsedRow eventCountRow = sheet.getRow(2);  // Row 3 in Excel (0-indexed 2)
        if (monthHeaderRow == null) return;

        Map<Integer, YearMonth> monthCols = new HashMap<>();

        // Parse month columns starting at Col D (index 3)
        for (Map.Entry<Integer, RosterExcelSaxReader.CellValue> entry : monthHeaderRow.getCells().entrySet()) {
            int col = entry.getKey();
            if (col < 3) continue;
            RosterExcelSaxReader.CellValue monthCell = entry.getValue();
            YearMonth ym = parseYearMonth(monthCell);
            if (ym != null) {
                monthCols.put(col, ym);

                // Read monthly event count if exists
                int count = 0;
                if (eventCountRow != null) {
                    RosterExcelSaxReader.CellValue ecCell = eventCountRow.getCell(col);
                    if (!ecCell.isBlank() && ecCell.getType() == RosterExcelSaxReader.CellType.NUMERIC) {
                        count = ecCell.getNumericValue().intValue();
                    } else if (!ecCell.isBlank() && ecCell.getType() == RosterExcelSaxReader.CellType.STRING) {
                        try {
                            count = Integer.parseInt(ecCell.getStringValue().trim());
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
            RosterExcelSaxReader.ParsedRow row = sheet.getRow(rowIdx);
            if (row == null || row.isEmpty()) continue;

            String unitName = row.getCell(0).getAsString().trim(); // Col A
            String mbNickname = row.getCell(1).getAsString().trim(); // Col B
            if (mbNickname.isEmpty()) continue;

            if (!unitName.isEmpty()) {
                memberUnits.put(mbNickname, unitName);
            }

            for (Map.Entry<Integer, YearMonth> entry : monthCols.entrySet()) {
                RosterExcelSaxReader.CellValue cell = row.getCell(entry.getKey());
                if (cell.isBlank()) continue;

                Integer attCount = null;
                AttendanceStatus status = null;

                if (cell.getType() == RosterExcelSaxReader.CellType.NUMERIC) {
                    attCount = cell.getNumericValue().intValue();
                } else if (cell.getType() == RosterExcelSaxReader.CellType.STRING) {
                    String val = cell.getStringValue().trim();
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

    private void parseTrainingSheet(RosterExcelSaxReader.ParsedSheet sheet, Map<String, RosterMember> membersByNickname,
                                    Map<String, TrainingDiscipline> parsedDisciplines, List<ParsedTraining> parsedTrainingList) {
        RosterExcelSaxReader.ParsedRow headerRow = sheet.getRow(3); // Row 4 in Excel (index 3)
        if (headerRow == null) return;

        Map<Integer, String> disciplineCols = new HashMap<>();
        // Disciplines are in Col B-G (indices 1-6)
        for (int col = 1; col <= 6; col++) {
            RosterExcelSaxReader.CellValue cell = headerRow.getCell(col);
            String name = cell.getAsString().trim();
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
            RosterExcelSaxReader.ParsedRow row = sheet.getRow(rowIdx);
            if (row == null || row.isEmpty()) continue;

            String mbNickname = row.getCell(0).getAsString().trim(); // Col A
            if (mbNickname.isEmpty()) continue;

            boolean isArchived = false;
            RosterExcelSaxReader.CellValue archCell = row.getCell(7); // Col H
            if (!archCell.isBlank()) {
                if (archCell.getType() == RosterExcelSaxReader.CellType.BOOLEAN) {
                    isArchived = archCell.getBooleanValue();
                } else if (archCell.getType() == RosterExcelSaxReader.CellType.STRING) {
                    String val = archCell.getStringValue().trim();
                    isArchived = val.equalsIgnoreCase("true") || val.equalsIgnoreCase("так");
                }
            }

            String notes = row.getCell(8).getAsString().trim(); // Col I

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
                RosterExcelSaxReader.CellValue cell = row.getCell(entry.getKey());
                if (!cell.isBlank() && cell.getType() == RosterExcelSaxReader.CellType.NUMERIC) {
                    int score = cell.getNumericValue().intValue();
                    if (score >= 1 && score <= 5) {
                        parsedTrainingList.add(new ParsedTraining(mbNickname, entry.getValue(), score));
                    }
                } else if (!cell.isBlank() && cell.getType() == RosterExcelSaxReader.CellType.STRING) {
                    try {
                        int score = Integer.parseInt(cell.getStringValue().trim());
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

    private YearMonth parseYearMonth(RosterExcelSaxReader.CellValue cell) {
        if (cell == null || cell.isBlank()) return null;
        if (cell.getType() == RosterExcelSaxReader.CellType.DATE) {
            LocalDate date = cell.getDateValue();
            return YearMonth.of(date.getYear(), date.getMonthValue());
        } else {
            String val = cell.getAsString().trim();
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
