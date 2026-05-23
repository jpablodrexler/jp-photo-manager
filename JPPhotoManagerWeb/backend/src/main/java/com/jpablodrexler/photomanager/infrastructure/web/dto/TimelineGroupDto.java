package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

public record TimelineGroupDto(LocalDate localDate, String label, List<AssetDto> assets) {}
