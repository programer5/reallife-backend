package com.example.backend.service.pin;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PinDetectionServiceTest {

    private final PinDetectionService sut = new PinDetectionService();
    private final ZoneId zoneId = ZoneId.of("Asia/Seoul");
    private final LocalDate today = LocalDate.of(2026, 2, 26);

    @Test
    void 시간만_있고_맥락도_장소도_없으면_핀_생성_안함() {
        var r = sut.detect("7시까지 연락줘", zoneId, today);
        assertThat(r).isEmpty();
    }

    @Test
    void 내일_7시_홍대_핀_생성() {
        var r = sut.detect("내일 7시 홍대", zoneId, today);
        assertThat(r).isPresent();
        assertThat(r.get().placeText()).contains("홍대");
        assertThat(r.get().startAt().toLocalDate()).isEqualTo(today.plusDays(1));
        assertThat(r.get().startAt().getHour()).isEqualTo(7);
    }

    @Test
    void 오후_7시_30분_회사앞에서_핀_생성() {
        var r = sut.detect("내일 오후 7시 30분 회사 앞에서 만나", zoneId, today);
        assertThat(r).isPresent();
        assertThat(r.get().placeText()).contains("회사 앞");
        assertThat(r.get().startAt().getHour()).isEqualTo(19);
        assertThat(r.get().startAt().getMinute()).isEqualTo(30);
    }

    @Test
    void 날짜_대시_형식_인식() {
        var r = sut.detect("2026-02-28 19:00 강남역에서 보자", zoneId, today);
        assertThat(r).isPresent();
        assertThat(r.get().startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(r.get().placeText()).contains("강남역");
    }

    @Test
    void 날짜_슬래시_형식_인식() {
        var r = sut.detect("2/28 19:00 성수", zoneId, today);
        assertThat(r).isPresent();
        assertThat(r.get().startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(r.get().placeText()).contains("성수");
    }
}