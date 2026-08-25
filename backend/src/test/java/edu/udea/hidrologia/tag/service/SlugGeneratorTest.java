package edu.udea.hidrologia.tag.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    private final SlugGenerator slugGenerator = new SlugGenerator();

    @Test
    void generatesDeterministicUrlSafeSlugs() {
        assertThat(slugGenerator.generate(" Morfometría ")).isEqualTo("morfometria");
        assertThat(slugGenerator.generate("Curva de duración")).isEqualTo("curva-de-duracion");
        assertThat(slugGenerator.generate("Balance hídrico / caudales")).isEqualTo("balance-hidrico-caudales");
    }
}
