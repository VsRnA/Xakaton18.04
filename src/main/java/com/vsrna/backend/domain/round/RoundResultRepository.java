package com.vsrna.backend.domain.round;

import java.util.Optional;

public interface RoundResultRepository {
    RoundResult create(RoundResult roundResult);
    Optional<RoundResult> find(RoundResultQuery query);
    RoundResult get(RoundResultQuery query);
    RoundResult update(RoundResultQuery query, RoundResultPatch patch);
}
