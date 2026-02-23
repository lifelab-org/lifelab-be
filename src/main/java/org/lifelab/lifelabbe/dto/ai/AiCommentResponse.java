package org.lifelab.lifelabbe.dto.ai;

public record AiCommentResponse(
        Long experimentId,
        String comment,
        boolean generated
) {}