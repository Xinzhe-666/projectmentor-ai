package com.xinzhe.projectmentor.auth.service;

import com.xinzhe.projectmentor.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationRateLimitServiceTests {

    @Test
    void rejectsFourthRegistrationFromSameIpWithinHour() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-06-13T00:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        RegistrationRateLimitService service = new RegistrationRateLimitService(null, clock);

        for (int i = 0; i < RegistrationRateLimitService.HOURLY_LIMIT; i++) {
            service.checkAllowed("203.0.113.10");
            service.recordSuccessfulRegistration("203.0.113.10");
        }

        assertThatThrownBy(() -> service.checkAllowed("203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("注册过于频繁，请稍后再试");
    }

    @Test
    void rejectsEleventhRegistrationFromSameIpWithinDay() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-06-13T00:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        RegistrationRateLimitService service = new RegistrationRateLimitService(null, clock);

        for (int i = 0; i < RegistrationRateLimitService.DAILY_LIMIT; i++) {
            service.checkAllowed("203.0.113.11");
            service.recordSuccessfulRegistration("203.0.113.11");
            clock.advanceSeconds(3600);
        }

        assertThatThrownBy(() -> service.checkAllowed("203.0.113.11"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("注册过于频繁，请稍后再试");
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
