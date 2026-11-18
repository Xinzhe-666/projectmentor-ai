package com.xinzhe.projectmentor.retrieval.chunk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectContentHasherTests {
    private final ProjectContentHasher hasher = new ProjectContentHasher();

    @Test
    void normalizesLfCrLfAndCrBeforeHashing() {
        assertThat(hasher.sha256("one\ntwo\nthree"))
                .isEqualTo(hasher.sha256("one\r\ntwo\rthree"));
    }

    @Test
    void usesUtf8Deterministically() {
        assertThat(hasher.sha256("鉴权实现\nJWT"))
                .isEqualTo("3a8b4da6d5c9b5016ca4f91229ef5fc63cf6011da0c7fa53f4fca521c81e5ce9");
    }
}
