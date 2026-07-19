package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TimelineGroupResponseDto(LocalDate localDate, String label, List<AssetResponseDto> assets) {}
