package ru.baikalsr.backend.Applications.service;

import jakarta.persistence.EntityManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.baikalsr.backend.Applications.dto.ApplicationAssignCurrentReleaseRequest;
import ru.baikalsr.backend.Applications.dto.ApplicationCreateRequest;
import ru.baikalsr.backend.Applications.dto.ApplicationDetailsDto;
import ru.baikalsr.backend.Applications.dto.ApplicationListItemDto;
import ru.baikalsr.backend.Applications.dto.ApplicationReleaseDownloadLinkDto;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseDetailsDto;
import ru.baikalsr.backend.Applications.entity.ArtifactApp;
import ru.baikalsr.backend.Applications.entity.ArtifactCurrentRelease;
import ru.baikalsr.backend.Applications.entity.ArtifactRelease;
import ru.baikalsr.backend.Applications.enums.ApplicationKey;
import ru.baikalsr.backend.Applications.mapper.ApplicationsMapper;
import ru.baikalsr.backend.Applications.mapper.ArtifactReleaseMapper;
import ru.baikalsr.backend.Applications.repository.ArtifactAppRepository;
import ru.baikalsr.backend.Applications.repository.ArtifactCurrentReleaseRepository;
import ru.baikalsr.backend.Applications.repository.ArtifactReleaseRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApplicationsService {

    private final ArtifactAppRepository artifactAppRepository;
    private final ArtifactReleaseRepository artifactReleaseRepository;
    private final ArtifactCurrentReleaseRepository artifactCurrentReleaseRepository;
    private final ApplicationsMapper applicationsMapper;
    private final ArtifactReleaseMapper artifactReleaseMapper;
    private final EntityManager entityManager;

    /**
     * На старте приложения гарантирует наличие всех системных записей приложений,
     * перечисленных в {@link ApplicationKey}.
     * Существующие записи не изменяет, отсутствующие — создает.
     */
    @PostConstruct
    public void ensureSystemApplications() {
        Arrays.stream(ApplicationKey.values())
                .forEach(this::ensureApplicationExists);
    }

    /**
     * Ищет приложение по системному ключу.
     *
     * @param key ключ приложения из системного перечисления
     * @return найденная запись приложения или пустой результат, если запись отсутствует
     */
    public Optional<ArtifactApp> findByKey(ApplicationKey key) {
        return artifactAppRepository.findByKey(key.name());
    }

    /**
     * Возвращает постраничный список приложений в коротком представлении.
     *
     * @param pageable параметры пагинации и сортировки
     * @return страница приложений для отображения в списке
     */
    public Page<ApplicationListItemDto> findAll(Pageable pageable) {
        return applicationsMapper.toListItems(artifactAppRepository.findAll(pageable));
    }

    /**
     * Возвращает детальное представление приложения по идентификатору.
     *
     * @param id идентификатор приложения
     * @return детальные метаданные приложения
     * @throws ResponseStatusException если приложение не найдено
     */
    public ApplicationDetailsDto getDetails(Long id) {
        ArtifactApp app = artifactAppRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND"));
        return applicationsMapper.toDetails(app);
    }

    /**
     * Гарантирует наличие записи приложения для указанного системного ключа.
     * Если запись уже существует, возвращает ее без изменений.
     * Если записи нет, создает новую с начальными значениями по умолчанию.
     *
     * @param key ключ приложения из системного перечисления
     * @return существующая или только что созданная запись приложения
     */
    @Transactional
    public ArtifactApp ensureApplicationExists(ApplicationKey key) {
        return artifactAppRepository.findByKey(key.name())
                .orElseGet(() -> artifactAppRepository.save(buildSystemApplication(key)));
    }

    /**
     * Создает новое пользовательское приложение в каталоге.
     * Ключ приложения должен быть уникален.
     * packageName заполняется позже, при первой успешной загрузке APK-релиза.
     * Новая запись создается неактивной, пока для нее не будет подготовлен релиз.
     *
     * @param request запрос на создание приложения
     * @return детальное представление созданного приложения
     * @throws ResponseStatusException если ключ уже занят
     */
    @Transactional
    public ApplicationDetailsDto create(ApplicationCreateRequest request) {
        if (artifactAppRepository.existsByKey(request.key())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICATION_KEY_ALREADY_EXISTS");
        }

        ArtifactApp saved = artifactAppRepository.save(applicationsMapper.fromCreateRequest(request));
        return applicationsMapper.toDetails(saved);
    }

    /**
     * Удаляет приложение по идентификатору.
     * Системные приложения, зарегистрированные через {@link ApplicationKey}, удалять запрещено.
     *
     * @param id идентификатор приложения
     * @throws ResponseStatusException если приложение не найдено или удаление запрещено
     */
    @Transactional
    public void delete(Long id) {
        ArtifactApp app = artifactAppRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND"));

        if (isSystemApplication(app.getKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SYSTEM_APPLICATION_DELETE_FORBIDDEN");
        }

        artifactAppRepository.delete(app);
        log.info("Deleted application id={}, key={}", app.getId(), app.getKey());
    }

    /**
     * Назначает конкретный релиз текущим для указанного приложения.
     * Если запись current release уже существует, она обновляется.
     * При успешном назначении приложение переводится в активное состояние.
     *
     * @param appId идентификатор приложения
     * @param request запрос с идентификатором релиза и автором назначения
     * @return детальное представление назначенного текущего релиза
     * @throws ResponseStatusException если приложение или релиз не найдены,
     *                                 либо релиз принадлежит другому приложению
     */
    @Transactional
    public ArtifactReleaseDetailsDto assignCurrentRelease(Long appId, ApplicationAssignCurrentReleaseRequest request) {
        ArtifactApp app = getApplicationOrThrow(appId);
        ArtifactRelease release = getReleaseOrThrow(request.releaseId());

        if (!release.getApp().getId().equals(app.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICATION_RELEASE_BELONGS_TO_ANOTHER_APP");
        }

        ArtifactCurrentRelease currentRelease = artifactCurrentReleaseRepository.findByAppId(app.getId())
                .orElse(null);

        boolean isNewCurrentRelease = currentRelease == null;
        if (isNewCurrentRelease) {
            currentRelease = new ArtifactCurrentRelease();
            currentRelease.setApp(app);
        }

        currentRelease.setRelease(release);
        currentRelease.setAssignedAt(Instant.now());
        currentRelease.setAssignedBy(request.assignedBy());

        if (isNewCurrentRelease) {
            entityManager.persist(currentRelease);
        }

        if (!app.isActive()) {
            app.setActive(true);
            artifactAppRepository.save(app);
        }

        return artifactReleaseMapper.toDetails(release);
    }

    /**
     * Возвращает текущий релиз приложения по идентификатору приложения.
     *
     * @param appId идентификатор приложения
     * @return детальное представление текущего релиза
     * @throws ResponseStatusException если приложение или текущий релиз не найдены
     */
    public ArtifactReleaseDetailsDto getCurrentReleaseByAppId(Long appId) {
        ArtifactApp app = getApplicationOrThrow(appId);
        ArtifactCurrentRelease currentRelease = artifactCurrentReleaseRepository.findByAppId(app.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_CURRENT_RELEASE_NOT_FOUND"));
        return artifactReleaseMapper.toDetails(currentRelease.getRelease());
    }

    /**
     * Возвращает текущий релиз приложения по системному ключу приложения.
     *
     * @param applicationKey ключ приложения из системного перечисления
     * @return детальное представление текущего релиза
     * @throws ResponseStatusException если приложение или текущий релиз не найдены
     */
    public ArtifactReleaseDetailsDto getCurrentReleaseByKey(ApplicationKey applicationKey) {
        ArtifactApp app = artifactAppRepository.findByKey(applicationKey.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND"));
        ArtifactCurrentRelease currentRelease = artifactCurrentReleaseRepository.findByAppId(app.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_CURRENT_RELEASE_NOT_FOUND"));
        return artifactReleaseMapper.toDetails(currentRelease.getRelease());
    }

    /**
     * Возвращает публичную ссылку на скачивание файла текущего релиза по идентификатору приложения.
     *
     * @param appId идентификатор приложения
     * @return DTO со ссылкой на скачивание
     */
    public ApplicationReleaseDownloadLinkDto getCurrentReleaseDownloadLinkByAppId(Long appId) {
        ArtifactReleaseDetailsDto currentRelease = getCurrentReleaseByAppId(appId);
        return new ApplicationReleaseDownloadLinkDto(
                currentRelease.id(),
                buildReleaseDownloadUrl(currentRelease.id())
        );
    }

    /**
     * Возвращает публичную ссылку на скачивание файла текущего релиза по системному ключу приложения.
     *
     * @param applicationKey ключ приложения из системного перечисления
     * @return DTO со ссылкой на скачивание
     */
    public ApplicationReleaseDownloadLinkDto getCurrentReleaseDownloadLinkByKey(ApplicationKey applicationKey) {
        ArtifactReleaseDetailsDto currentRelease = getCurrentReleaseByKey(applicationKey);
        return new ApplicationReleaseDownloadLinkDto(
                currentRelease.id(),
                buildReleaseDownloadUrl(currentRelease.id())
        );
    }

    /**
     * Формирует начальную системную запись приложения для последующего сохранения в БД.
     * Здесь задаются стартовые метаданные, с которыми приложение появляется в каталоге.
     *
     * @param key ключ приложения из системного перечисления
     * @return новая несохраненная сущность приложения
     */
    private ArtifactApp buildSystemApplication(ApplicationKey key) {
        return switch (key) {
            case DEVICE_OWNER_APP -> ArtifactApp.builder()
                    .key(key.name())
                    .name("Device Owner App")
                    .packageName("")
                    .active(false)
                    .build();
        };
    }

    /**
     * Проверяет, относится ли ключ приложения к системным ключам, описанным в enum.
     *
     * @param key строковый ключ приложения
     * @return true, если ключ является системным
     */
    private boolean isSystemApplication(String key) {
        return Arrays.stream(ApplicationKey.values())
                .anyMatch(applicationKey -> applicationKey.name().equals(key));
    }

    private ArtifactApp getApplicationOrThrow(Long appId) {
        return artifactAppRepository.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND"));
    }

    private ArtifactRelease getReleaseOrThrow(Long releaseId) {
        return artifactReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_RELEASE_NOT_FOUND"));
    }

    private String buildReleaseDownloadUrl(Long releaseId) {
        return "/api/v1/applications/releases/" + releaseId + "/file";
    }
}
