package com.maya.fashionassistant;

import com.maya.fashionassistant.service.IntentExtractor;
import com.maya.fashionassistant.service.StylingIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntentExtractorTest {

    private final IntentExtractor extractor = new IntentExtractor();

    @Test
    void extract_OccasionAndStyle() {
        StylingIntent intent = extractor.extract("Pick something cute for brunch with my girlies tomorrow.");
        assertEquals("brunch", intent.getOccasion());
        assertEquals("cute", intent.getStyle());
        assertEquals("casual", intent.getFormality());
    }

    @Test
    void extract_NegativeConstraint_Sneakers() {
        StylingIntent intent = extractor.extract("I don't want sneakers.");
        assertTrue(intent.getExclusions().contains("sneakers"));
    }

    @Test
    void extract_Refinement_MoreFeminine() {
        StylingIntent intent = extractor.extract("Make it more feminine.");
        assertTrue(intent.isMoreFeminine());
    }

    @Test
    void extract_Refinement_AnotherOption() {
        StylingIntent intent = extractor.extract("Give me another option.");
        assertTrue(intent.isAnotherOption());
    }

    @Test
    void extract_FormalOccasion() {
        StylingIntent intent = extractor.extract("What should I wear for a wedding?");
        assertEquals("wedding", intent.getOccasion());
        assertEquals("formal", intent.getFormality());
    }
}
