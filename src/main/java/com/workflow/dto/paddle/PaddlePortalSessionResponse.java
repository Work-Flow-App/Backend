package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaddlePortalSessionResponse(PortalData data) {

    public record PortalData(Urls urls) {}

    public record Urls(General general) {}

    public record General(String overview) {}
}