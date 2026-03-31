package ru.baikalsr.backend.Applications.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Applications.dto.ApplicationAssignCurrentReleaseRequest;
import ru.baikalsr.backend.Applications.dto.ApplicationCreateRequest;
import ru.baikalsr.backend.Applications.dto.ApplicationDetailsDto;
import ru.baikalsr.backend.Applications.dto.ApplicationListItemDto;
import ru.baikalsr.backend.Applications.dto.ApplicationReleaseDownloadLinkDto;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseDetailsDto;
import ru.baikalsr.backend.Applications.enums.ApplicationKey;
import ru.baikalsr.backend.Applications.service.ApplicationsService;
import ru.baikalsr.backend.common.model.PageResponse;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(
        name = "Applications",
        description = "Операции для работы с каталогом приложений и их базовыми метаданными."
)
public class ApplicationsController {

    private final ApplicationsService applicationsService;

    @GetMapping
    @Operation(
            summary = "Получить список приложений",
            description = "Возвращает постраничный список приложений без дополнительных фильтров."
    )
    public PageResponse<ApplicationListItemDto> getApplications(
            @ParameterObject
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(applicationsService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить детальную информацию о приложении",
            description = "Возвращает полные базовые метаданные приложения по его идентификатору."
    )
    public ApplicationDetailsDto getApplication(@PathVariable Long id) {
        return applicationsService.getDetails(id);
    }

    @PostMapping
    @Operation(
            summary = "Создать новое приложение",
            description = "Создает новую запись приложения в каталоге. Новое приложение создается неактивным."
    )
    public ResponseEntity<ApplicationDetailsDto> createApplication(@Valid @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationsService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить приложение",
            description = "Удаляет приложение по идентификатору. Системные приложения, созданные из ApplicationKey, удалять нельзя."
    )
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/current-release")
    @Operation(
            summary = "Назначить текущий релиз приложения",
            description = "Назначает конкретный релиз текущим для выбранного приложения."
    )
    public ArtifactReleaseDetailsDto assignCurrentRelease(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationAssignCurrentReleaseRequest request
    ) {
        return applicationsService.assignCurrentRelease(id, request);
    }

    @GetMapping("/{id}/current-release")
    @Operation(
            summary = "Получить текущий релиз приложения по id",
            description = "Возвращает текущий релиз для указанного приложения."
    )
    public ArtifactReleaseDetailsDto getCurrentReleaseById(@PathVariable Long id) {
        return applicationsService.getCurrentReleaseByAppId(id);
    }

    @GetMapping("/key/{key}/current-release")
    @Operation(
            summary = "Получить текущий релиз приложения по ключу",
            description = "Возвращает текущий релиз для системного приложения по его ключу."
    )
    public ArtifactReleaseDetailsDto getCurrentReleaseByKey(@PathVariable ApplicationKey key) {
        return applicationsService.getCurrentReleaseByKey(key);
    }

    @GetMapping("/{id}/current-release/download-link")
    @Operation(
            summary = "Получить ссылку на скачивание текущего релиза по id приложения",
            description = "Возвращает публичную ссылку на скачивание файла текущего релиза приложения."
    )
    public ApplicationReleaseDownloadLinkDto getCurrentReleaseDownloadLinkById(@PathVariable Long id) {
        return applicationsService.getCurrentReleaseDownloadLinkByAppId(id);
    }

    @GetMapping("/key/{key}/current-release/download-link")
    @Operation(
            summary = "Получить ссылку на скачивание текущего релиза по ключу приложения",
            description = "Возвращает публичную ссылку на скачивание файла текущего релиза системного приложения."
    )
    public ApplicationReleaseDownloadLinkDto getCurrentReleaseDownloadLinkByKey(@PathVariable ApplicationKey key) {
        return applicationsService.getCurrentReleaseDownloadLinkByKey(key);
    }
}
