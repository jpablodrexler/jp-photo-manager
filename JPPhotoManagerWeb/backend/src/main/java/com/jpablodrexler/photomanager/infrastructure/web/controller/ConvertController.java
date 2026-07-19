package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.port.in.convert.ConvertAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.GetConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.in.convert.SaveConvertConfigUseCase;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.ConvertDirectoryPairDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.ConvertWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Convert", description = "PNG to JPEG conversion configuration and execution")
@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ConvertController {

    private final GetConvertConfigUseCase getConvertConfigUseCase;
    private final SaveConvertConfigUseCase saveConvertConfigUseCase;
    private final ConvertAssetsUseCase convertAssetsUseCase;
    private final KafkaProgressRegistry kafkaProgressRegistry;
    private final ConvertWebMapper convertWebMapper;

    @Operation(summary = "Get convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convert configuration"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/configuration")
    public ResponseEntity<List<ConvertDirectoryPairDto>> getConfiguration() {
        return ResponseEntity.ok(convertWebMapper.toDtoList(getConvertConfigUseCase.execute()));
    }

    @Operation(summary = "Save convert directory pair configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Configuration saved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/configuration")
    public ResponseEntity<Void> setConfiguration(@Valid @RequestBody List<ConvertDirectoryPairDto> definitions) {
        saveConvertConfigUseCase.execute(convertWebMapper.toDomainList(definitions));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Run PNG→JPEG conversion and stream progress via SSE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SSE stream of convert status and results"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        long runId = System.currentTimeMillis();
        Runnable cleanup = () -> kafkaProgressRegistry.remove(runId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());
        kafkaProgressRegistry.registerEmitter(runId, emitter);
        convertAssetsUseCase.execute(runId);
        return emitter;
    }
}
