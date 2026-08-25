package edu.udea.hidrologia.post.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

class PostMappingTest {

    @Test
    void mapsSourceQuestionAsOptionalOneToOneWithoutDestructiveCascade() throws NoSuchFieldException {
        Field sourceQuestionField = Post.class.getDeclaredField("sourceQuestion");
        OneToOne oneToOne = sourceQuestionField.getAnnotation(OneToOne.class);
        JoinColumn joinColumn = sourceQuestionField.getAnnotation(JoinColumn.class);

        assertThat(oneToOne).isNotNull();
        assertThat(oneToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(Arrays.asList(oneToOne.cascade())).doesNotContain(CascadeType.REMOVE, CascadeType.ALL);
        assertThat(joinColumn.name()).isEqualTo("source_question_id");
        assertThat(joinColumn.unique()).isTrue();
        assertThat(joinColumn.nullable()).isTrue();
    }
}
