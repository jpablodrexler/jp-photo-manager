package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

public interface MonthlyCountProjection {
    String getMonth();
    Long getCnt();
}
