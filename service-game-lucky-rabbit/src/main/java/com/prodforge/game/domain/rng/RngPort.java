package com.prodforge.game.domain.rng;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RngPort {
    RngCommitment commit(UUID roomId, int roundNumber);
    List<BigDecimal> reveal(String seedHex, int count);
}
