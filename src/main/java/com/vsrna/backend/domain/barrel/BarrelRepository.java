package com.vsrna.backend.domain.barrel;

import java.util.List;

public interface BarrelRepository {
    Barrel create(Barrel barrel);
    List<Barrel> createAll(List<Barrel> barrels);
    List<Barrel> list(BarrelQuery query);
    Barrel update(BarrelQuery query, BarrelPatch patch);
    int updateAll(BarrelQuery query, List<Barrel> barrels);
}
