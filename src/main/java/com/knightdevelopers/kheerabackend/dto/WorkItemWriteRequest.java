package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

@Schema(description="Task create/PATCH fields: title, description, efforts, typeId, stageId, parentId, assigneeMemberId, plannedStartDate, plannedEndDate, actualStartDate, actualEndDate. Omission preserves values. Null clears optional fields only. UUID relationships are scoped; dates are ISO-8601 instants. Unknown fields including sprintId and assigneeId are rejected.",
        example="{\"title\":\"Implement checkout\",\"efforts\":3,\"description\":null}", implementation=WorkItemWriteRequest.Fields.class)
public class WorkItemWriteRequest {
    @Schema(name="WorkItemInput",description="Create requires title. PATCH omission preserves; null clears only nullable fields. All writes require space.update.")
    public record Fields(
            @Schema(minLength=1,maxLength=255) String title,
            @Schema(maxLength=500,nullable=true) String description,
            @Schema(minimum="0",defaultValue="1") Integer efforts,
            UUID typeId, UUID stageId,
            @Schema(nullable=true) UUID parentId,
            @Schema(nullable=true,description="Active same-space membership UUID, never a user UUID") UUID assigneeMemberId,
            @Schema(nullable=true) Instant plannedStartDate, @Schema(nullable=true) Instant plannedEndDate,
            @Schema(nullable=true) Instant actualStartDate, @Schema(nullable=true) Instant actualEndDate) {}
    private static final Set<String> TEXT=Set.of("title","description");
    private static final Set<String> IDS=Set.of("typeId","stageId","parentId","assigneeMemberId");
    private static final Set<String> DATES=Set.of("plannedStartDate","plannedEndDate","actualStartDate","actualEndDate");
    private final Map<String,Object> fields=new HashMap<>();

    @JsonAnySetter public void field(String name,JsonNode value) {
        if(!TEXT.contains(name) && !IDS.contains(name) && !DATES.contains(name) && !name.equals("efforts"))
            throw SpaceApiException.invalid("body","Unknown fields are not allowed.");
        if(value==null || value.isNull()) { fields.put(name,null); return; }
        if(name.equals("efforts")) {
            if(!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<0)
                throw SpaceApiException.invalid(name,"Must be a non-negative integer.");
            fields.put(name,value.intValue()); return;
        }
        if(!value.isTextual()) throw SpaceApiException.invalid(name,"Must be a string or null.");
        String text=value.textValue();
        try {
            if(IDS.contains(name)) {
                UUID id=UUID.fromString(text);
                if(!id.toString().equalsIgnoreCase(text)) throw new IllegalArgumentException();
                fields.put(name,id);
            } else if(DATES.contains(name)) {
                Instant date=Instant.parse(text);
                if(date.isBefore(Instant.parse("0001-01-01T00:00:00Z")) || date.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z")))
                    throw new IllegalArgumentException();
                fields.put(name,date);
            } else fields.put(name,name.equals("title")?text.strip():text);
        } catch(IllegalArgumentException | DateTimeParseException ex) {
            throw SpaceApiException.invalid(name,IDS.contains(name)?"Must be a UUID.":"Must be a supported ISO-8601 instant.");
        }
    }
    public void validate(boolean create) {
        if(create && !has("title")) throw SpaceApiException.invalid("title","Title is required.");
        for(String field:List.of("title","efforts","typeId","stageId"))
            if(has(field) && fields.get(field)==null) throw SpaceApiException.invalid(field,"Cannot be null.");
        for(String field:TEXT) {
            String text=text(field);
            if(text!=null && (field.equals("title") && text.isBlank() || text.codePointCount(0,text.length())>(field.equals("title")?255:500)))
                throw SpaceApiException.invalid(field,"Text is blank or exceeds its length limit.");
        }
    }
    public boolean has(String name) { return fields.containsKey(name); }
    public boolean empty() { return fields.isEmpty(); }
    public String text(String name) { return (String)fields.get(name); }
    public UUID id(String name) { return (UUID)fields.get(name); }
    public Instant date(String name) { return (Instant)fields.get(name); }
    public Integer efforts() { return (Integer)fields.get("efforts"); }
}
