package org.lifelab.lifelabbe.dto.experiment;

public class PreStateResponse {
    private final Long experimentId;
    private final boolean saved;

    public PreStateResponse(Long experimentId, boolean saved) {
        this.experimentId = experimentId;
        this.saved = saved;
    }

    public Long getExperimentId() { return experimentId; }
    public boolean isSaved() { return saved; }
}
