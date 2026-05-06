package com.aulix.aulix_backend.domain.course.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void createCourseRejectsInvalidPriceCurrencyAndThumbnail() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Valid title");
        request.setPrice(BigDecimal.valueOf(-1));
        request.setCurrency("usd");
        request.setThumbnailUrl("ftp://example.com/image.png");

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("price", "currency", "thumbnailUrl");
    }

    @Test
    void createLessonRejectsInvalidVideoUrlAndNegativeDuration() {
        CreateLessonRequest request = new CreateLessonRequest();
        request.setTitle("Valid title");
        request.setVideoUrl("file://video.mp4");
        request.setDurationSecs(-1);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("videoUrl", "durationSecs");
    }

    @Test
    void addModuleRejectsBlankTitle() {
        AddModuleRequest request = new AddModuleRequest();
        request.setTitle("   ");

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title");
    }
}
