package com.knightdevelopers.kheerabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.WorkItemWriteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class WorkItemWriteRequestTest {
    ObjectMapper mapper=new ObjectMapper();
    @ParameterizedTest
    @ValueSource(strings={"{\"title\":null}","{\"efforts\":null}","{\"efforts\":2147483648}","{\"efforts\":\"3\"}",
            "{\"typeId\":null}","{\"stageId\":null}","{\"parentId\":\"1-1-1-1-1\"}","{\"plannedStartDate\":\"2026-10-02\"}","{\"projectId\":null}"})
    void rejectsInvalidPatch(String json) {
        assertThatThrownBy(()->mapper.readValue(json,WorkItemWriteRequest.class).validate(false)).isInstanceOf(Exception.class);
    }
    @Test void distinguishesOmissionFromClearAndAcceptsZeroEffort() throws Exception {
        var patch=mapper.readValue("{\"description\":null,\"efforts\":0}",WorkItemWriteRequest.class);
        patch.validate(false);
        assertThat(patch.has("description")).isTrue(); assertThat(patch.text("description")).isNull();
        assertThat(patch.has("parentId")).isFalse(); assertThat(patch.efforts()).isZero();
    }
    @Test void enforcesDatabaseTextLengths() {
        for(String field:new String[]{"title","description"}) {
            String json="{\""+field+"\":\""+"x".repeat(field.equals("title")?256:501)+"\"}";
            assertThatThrownBy(()->mapper.readValue(json,WorkItemWriteRequest.class).validate(false)).isInstanceOf(Exception.class);
        }
    }
}
