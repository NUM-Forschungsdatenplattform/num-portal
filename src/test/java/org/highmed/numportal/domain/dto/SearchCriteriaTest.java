package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.dto.SearchCriteria;
import org.junit.Assert;
import org.junit.Test;
import org.highmed.numportal.service.exception.BadRequestException;

public class SearchCriteriaTest {

    @Test
    public void isValidTest() {
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sortBy("sortByField")
                .sort("sortField")
                .build();
        Assert.assertTrue(searchCriteria.isValid());
    }

    @Test
    public void sortFieldMissingTest() {
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sortBy("sortByField")
                .build();
        try {
            searchCriteria.isValid();
        } catch (BadRequestException be) {
            Assert.assertEquals("sort field is required when sortBy is provided", be.getMessage());
        }
    }

    @Test
    public void sortByFieldMissingTest() {
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sort("sortField")
                .build();
        try {
            searchCriteria.isValid();
        } catch (BadRequestException be) {
            Assert.assertEquals("sortBy field is required when sort is provided", be.getMessage());
        }
    }

    @Test
    public void sortByAuthorTest() {
        SearchCriteria searchCriteria = SearchCriteria.builder()
                .sortBy("author")
                .sort("desc")
                .build();
        Assert.assertTrue(searchCriteria.isSortByAuthor());
    }
}
