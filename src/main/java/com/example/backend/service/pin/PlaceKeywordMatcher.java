package com.example.backend.service.pin;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MVP 룰 기반 장소 감지기.
 *
 * - 키워드 리스트 기반 (최장 매칭 우선)
 * - "~역" 형태도 지원
 * - 추후 DB/관리자 페이지로 쉽게 확장 가능
 */
@Component
public class PlaceKeywordMatcher {

    // "~역" 형태 잡기
    private static final Pattern P_STATION = Pattern.compile("([가-힣]{2,10})\s*역");

    // 키워드 기반 (필요 시 확장)
    // ⚠️ "강남역"이 "강남"보다 먼저 매칭되도록 최장 매칭(길이 내림차순) 적용
    private static final List<String> KEYWORDS = List.of(
            "홍대입구", "홍대", "강남역", "강남", "신촌", "합정", "상수", "연남", "이태원", "성수", "건대", "잠실",
            "종로", "광화문", "여의도", "서울역", "고속터미널", "사당", "교대", "선릉", "삼성", "역삼", "판교"
    );

    private final List<String> sortedKeywords;

    public PlaceKeywordMatcher() {
        List<String> list = new ArrayList<>(KEYWORDS);
        list.sort(Comparator.comparingInt(String::length).reversed());
        this.sortedKeywords = List.copyOf(list);
    }

    public String detect(String rawMessage) {
        if (rawMessage == null) return null;
        String msg = rawMessage.trim();
        if (msg.isBlank()) return null;

        // 1) 키워드 최장 매칭
        for (String k : sortedKeywords) {
            if (msg.contains(k)) return k;
        }

        // 2) "~역" 패턴
        Matcher m = P_STATION.matcher(msg);
        if (m.find()) {
            return m.group(1) + "역";
        }

        return null;
    }
}
