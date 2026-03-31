package ru.baikalsr.backend.Applications.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ru.baikalsr.backend.Applications.entity.ArtifactRelease;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Набор спецификаций для поиска релизов приложений.
 */
public final class ArtifactReleaseSpecifications {

    private ArtifactReleaseSpecifications() {
    }

    public static Specification<ArtifactRelease> withFilters(Long appId, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (appId != null) {
                predicates.add(cb.equal(root.get("app").get("id"), appId));
            }

            if (StringUtils.hasText(search)) {
                String normalizedSearch = search.trim();
                String loweredSearch = normalizedSearch.toLowerCase();

                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(
                        cb.like(
                                cb.lower(root.get("versionName")),
                                "%" + loweredSearch + "%"
                        )
                );

                try {
                    searchPredicates.add(cb.equal(root.get("versionCode"), Integer.parseInt(normalizedSearch)));
                } catch (NumberFormatException ignored) {
                    // Если поиск не является числом, ищем только по versionName.
                }

                predicates.add(cb.or(searchPredicates.toArray(Predicate[]::new)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
