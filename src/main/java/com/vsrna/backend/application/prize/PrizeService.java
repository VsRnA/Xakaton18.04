package com.vsrna.backend.application.prize;

import java.util.UUID;

public interface PrizeService {
    void distributePrize(UUID roomId);
}
