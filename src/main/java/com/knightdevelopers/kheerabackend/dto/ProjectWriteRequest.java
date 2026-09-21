package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project metadata. PATCH omission preserves values; null clears description only. Unknown fields are rejected.")
public class ProjectWriteRequest {
    @Schema(type="string",maxLength=255,description="Trimmed nonblank name; required on create.")
    private String name;
    @Schema(type="string",maxLength=500,nullable=true)
    private String description;
    @Schema(type="integer",minimum="1",maximum="2147483647",defaultValue="7")
    private Integer sprintCycleDays;
    @Schema(hidden=true) private boolean namePresent;
    @Schema(hidden=true) private boolean descriptionPresent;
    @Schema(hidden=true) private boolean sprintCycleDaysPresent;
    @JsonSetter("name") public void setName(JsonNode value) { namePresent=true; name=text("name",value); }
    @JsonSetter("description") public void setDescription(JsonNode value) { descriptionPresent=true; description=text("description",value); }
    @JsonSetter("sprintCycleDays") public void setSprintCycleDays(JsonNode value) {
        sprintCycleDaysPresent=true;
        if (value==null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<1)
            throw SpaceApiException.invalid("sprintCycleDays","Must be a positive integer.");
        sprintCycleDays=value.intValue();
    }
    @JsonAnySetter public void unknown(String field,JsonNode value) {
        throw SpaceApiException.invalid("body","Unknown fields are not allowed.");
    }
    private static String text(String field,JsonNode value) {
        if(value==null || value.isNull()) return null;
        if(!value.isTextual()) throw SpaceApiException.invalid(field,"Must be a string or null.");
        return value.textValue();
    }
    public void validate(boolean creation) {
        if(creation || namePresent) {
            if(name==null || name.isBlank()) throw SpaceApiException.invalid("name","Name is required.");
            name=name.strip();
            limit("name",name,255);
        }
        limit("description",description,500);
    }
    private static void limit(String field,String value,int max) {
        if(value!=null && value.codePointCount(0,value.length())>max)
            throw SpaceApiException.invalid(field,"Must contain at most "+max+" characters.");
    }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getSprintCycleDays() { return sprintCycleDays; }
    public boolean hasName() { return namePresent; }
    public boolean hasDescription() { return descriptionPresent; }
    public boolean hasSprintCycleDays() { return sprintCycleDaysPresent; }
}
