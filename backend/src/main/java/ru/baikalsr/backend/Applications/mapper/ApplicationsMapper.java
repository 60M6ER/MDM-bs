package ru.baikalsr.backend.Applications.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Applications.dto.ApplicationCreateRequest;
import ru.baikalsr.backend.Applications.dto.ApplicationDetailsDto;
import ru.baikalsr.backend.Applications.dto.ApplicationListItemDto;
import ru.baikalsr.backend.Applications.entity.ArtifactApp;

import java.util.List;

/**
 * Ручной маппер DTO для домена приложений.
 */
@Component
public class ApplicationsMapper {

    /**
     * Преобразует сущность приложения в короткое представление для списка.
     */
    public ApplicationListItemDto toListItem(ArtifactApp src) {
        if (src == null) return null;
        return new ApplicationListItemDto(
                src.getId(),
                src.getKey(),
                src.getName()
        );
    }

    /**
     * Преобразует сущность приложения в полное детальное представление.
     */
    public ApplicationDetailsDto toDetails(ArtifactApp src) {
        if (src == null) return null;
        return new ApplicationDetailsDto(
                src.getId(),
                src.getKey(),
                src.getName(),
                src.getPackageName(),
                src.isActive(),
                src.getCreatedAt()
        );
    }

    /**
     * Создает новую сущность приложения из запроса на создание.
     * Новое приложение создается неактивным до явной публикации релиза.
     */
    public ArtifactApp fromCreateRequest(ApplicationCreateRequest src) {
        if (src == null) return null;
        return ArtifactApp.builder()
                .key(src.key())
                .name(src.name())
                .packageName("")
                .active(false)
                .build();
    }

    /**
     * Преобразует список сущностей в список коротких DTO.
     */
    public List<ApplicationListItemDto> toListItems(List<ArtifactApp> list) {
        return list.stream().map(this::toListItem).toList();
    }

    /**
     * Преобразует страницу сущностей в страницу коротких DTO.
     */
    public Page<ApplicationListItemDto> toListItems(Page<ArtifactApp> page) {
        return new PageImpl<>(
                toListItems(page.getContent()),
                page.getPageable(),
                page.getTotalElements()
        );
    }
}
