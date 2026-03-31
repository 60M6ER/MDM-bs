package ru.baikalsr.backend.Applications.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseCreateRequest;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseDetailsDto;
import ru.baikalsr.backend.Applications.service.ArtifactReleaseService;
import ru.baikalsr.backend.common.model.PageResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/applications/releases")
@RequiredArgsConstructor
@Tag(
        name = "Application Releases",
        description = "Операции для загрузки, получения и удаления файлов релизов приложений."
)
public class ArtifactReleaseController {

    private final ArtifactReleaseService artifactReleaseService;

    @GetMapping
    @Operation(
            summary = "Получить список релизов приложений",
            description = "Возвращает постраничный список релизов. Можно фильтровать по приложению и искать по versionName или versionCode."
    )
    public PageResponse<ArtifactReleaseDetailsDto> getReleases(
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) String search,
            @ParameterObject
            @PageableDefault(size = 30, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(artifactReleaseService.findAll(appId, search, pageable));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузить новый релиз приложения",
            description = "Создает запись релиза и сохраняет бинарный файл в локальное storage. Ожидает multipart/form-data с частями 'request' и 'file'."
    )
    public ResponseEntity<ArtifactReleaseDetailsDto> createRelease(
            @Valid @RequestPart("request") ArtifactReleaseCreateRequest request,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(artifactReleaseService.create(request, file));
    }

    @GetMapping("/{releaseId}")
    @Operation(
            summary = "Получить метаданные релиза",
            description = "Возвращает детальные метаданные релиза без бинарного содержимого файла."
    )
    public ArtifactReleaseDetailsDto getRelease(@PathVariable Long releaseId) {
        return artifactReleaseService.getDetails(releaseId);
    }

    @GetMapping("/{releaseId}/file")
    @Operation(
            summary = "Скачать файл релиза",
            description = "Возвращает бинарный файл релиза по идентификатору. Endpoint доступен без авторизации."
    )
    public ResponseEntity<Resource> downloadReleaseFile(@PathVariable Long releaseId) {
        ArtifactReleaseDetailsDto details = artifactReleaseService.getDetails(releaseId);
        Resource resource = artifactReleaseService.loadArtifact(releaseId);

        String filename = artifactReleaseService.getDownloadFilename(releaseId);
        MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @DeleteMapping("/{releaseId}")
    @Operation(
            summary = "Удалить релиз приложения",
            description = "Удаляет запись релиза и связанный файл. Удаление запрещено для релиза, назначенного текущим."
    )
    public ResponseEntity<Void> deleteRelease(@PathVariable Long releaseId) {
        artifactReleaseService.delete(releaseId);
        return ResponseEntity.noContent().build();
    }
}
