package com.example.backend.service.pin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PinDetectionService {

    // 시간 패턴들 (MVP: 규칙 기반)
    private static final Pattern P_TIME_HOUR = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})\\s*시(\\s*(\\d{1,2})\\s*분)?");
    private static final Pattern P_TIME_COLON = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})");

    // 날짜 패턴들
    private static final Pattern P_DATE_SLASH = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})");          // 2/28
    private static final Pattern P_DATE_DASH = Pattern.compile("(\\d{4})\\s*-\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})"); // 2026-02-28

    // 장소 후보(키워드 기반 MVP). 필요하면 쉽게 확장 가능.
    private static final List<String> PLACE_KEYWORDS = List.of(
            "홍대", "강남", "강남역", "신촌", "합정", "상수", "연남", "이태원", "성수", "건대", "잠실",
            "종로", "광화문", "여의도", "서울역", "고속터미널", "사당", "교대", "선릉", "삼성", "역삼"
    );

    // "~역" 형태를 잡기 위한 간단 패턴
    private static final Pattern P_STATION = Pattern.compile("([가-힣]{2,6})역");

    public record DetectionResult(
            String title,
            String placeText,
            LocalDateTime startAt
    ) {}

    public Optional<DetectionResult> detect(String rawMessage) {
        if (rawMessage == null) return Optional.empty();
        String msg = rawMessage.trim();
        if (msg.isBlank()) return Optional.empty();

        String place = detectPlace(msg);
        LocalDateTime startAt = detectDateTime(msg);

        // MVP: 장소/시간 둘 중 하나라도 있으면 핀 생성
        if ((place == null || place.isBlank()) && startAt == null) {
            return Optional.empty();
        }

        return Optional.of(new DetectionResult("약속", place, startAt));
    }

    private String detectPlace(String msg) {
        for (String k : PLACE_KEYWORDS) {
            if (msg.contains(k)) return k;
        }

        Matcher m = P_STATION.matcher(msg);
        if (m.find()) {
            return m.group(1) + "역";
        }
        return null;
    }

    private LocalDateTime detectDateTime(String msg) {
        LocalDate baseDate = detectBaseDate(msg);
        LocalTime time = detectTime(msg);

        if (baseDate == null && time == null) return null;

        // 시간이 없으면 startAt null (장소만 핀) 허용
        if (time == null) return null;

        LocalDate date = (baseDate == null) ? LocalDate.now(ZoneId.of("Asia/Seoul")) : baseDate;
        return LocalDateTime.of(date, time);
    }

    private LocalDate detectBaseDate(String msg) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);

        if (msg.contains("오늘")) return today;
        if (msg.contains("내일")) return today.plusDays(1);
        if (msg.contains("모레")) return today.plusDays(2);

        // 2026-02-28
        Matcher dash = P_DATE_DASH.matcher(msg);
        if (dash.find()) {
            int y = Integer.parseInt(dash.group(1));
            int mo = Integer.parseInt(dash.group(2));
            int d = Integer.parseInt(dash.group(3));
            try {
                return LocalDate.of(y, mo, d);
            } catch (Exception ignore) {
                return null;
            }
        }

        // 2/28  (연도는 올해로 가정)
        Matcher slash = P_DATE_SLASH.matcher(msg);
        if (slash.find()) {
            int mo = Integer.parseInt(slash.group(1));
            int d = Integer.parseInt(slash.group(2));
            int y = today.getYear();
            try {
                LocalDate parsed = LocalDate.of(y, mo, d);
                // 과거로 떨어지면 내년으로 보정(예: 12월에 "1/2")
                if (parsed.isBefore(today.minusDays(1))) parsed = parsed.plusYears(1);
                return parsed;
            } catch (Exception ignore) {
                return null;
            }
        }

        return null;
    }

    private LocalTime detectTime(String msg) {
        // 19:00
        Matcher colon = P_TIME_COLON.matcher(msg);
        if (colon.find()) {
            int h = Integer.parseInt(colon.group(1));
            int m = Integer.parseInt(colon.group(2));
            if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                return LocalTime.of(h, m);
            }
        }

        // "오후 7시 30분", "7시"
        Matcher hour = P_TIME_HOUR.matcher(msg);
        if (hour.find()) {
            String ampm = hour.group(1);      // 오전/오후/null
            int h = Integer.parseInt(hour.group(2));
            String minGroup = hour.group(4);
            int m = (minGroup == null) ? 0 : Integer.parseInt(minGroup);

            if (m < 0 || m > 59) return null;

            // 오전/오후 처리
            if ("오후".equals(ampm) && h < 12) h += 12;
            if ("오전".equals(ampm) && h == 12) h = 0;

            // ampm이 없으면 0~23 그대로 해석 (MVP)
            if (h < 0 || h > 23) return null;

            return LocalTime.of(h, m);
        }

        return null;
    }
}