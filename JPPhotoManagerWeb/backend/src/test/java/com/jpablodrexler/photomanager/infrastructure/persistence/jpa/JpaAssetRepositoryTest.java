package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * {@link JpaAssetRepository} is a Spring Data JPA interface — every method except {@code
 * findWithFilters} is either a derived-query method or an {@code @Query}, both of which have no
 * logic of their own to unit test (their behavior is exercised through {@code
 * AssetRepositoryImpl}/integration tests instead). {@code findWithFilters} is the one exception:
 * a real, hand-written {@code default} method that builds a {@link Specification} out of six
 * independent optional filters plus a fetch-join that must be skipped for count queries. Since
 * this is a plain interface (no Spring context, no real JPA provider), the default method is
 * exercised directly on a Mockito mock configured to call real (default) methods, and the
 * captured {@link Specification} is evaluated against hand-mocked JPA Criteria API objects.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class JpaAssetRepositoryTest {

    private final JpaAssetRepository sut =
            mock(JpaAssetRepository.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

    private Specification<AssetEntity> captureSpecification(FolderEntity folder, String search,
            LocalDateTime dateFrom, LocalDateTime dateTo, Integer minRating, Set<String> tags, Pageable pageable) {
        ArgumentCaptor<Specification<AssetEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        doReturn(new PageImpl<>(List.of())).when(sut).findAll(captor.capture(), eq(pageable));

        sut.findWithFilters(folder, search, dateFrom, dateTo, minRating, tags, pageable);

        return captor.getValue();
    }

    @Test
    void findWithFilters_delegatesToFindAllWithSpecificationAndPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec = captureSpecification(null, null, null, null, null, null, pageable);

        assertThat(spec).isNotNull();
        verify(sut).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void toPredicate_countQuery_doesNotFetchFolder() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec = captureSpecification(null, null, null, null, null, null, pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(Long.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.isNull(any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(root, never()).fetch(anyString(), any(JoinType.class));
    }

    @Test
    void toPredicate_entityQuery_fetchesFolder() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec = captureSpecification(null, null, null, null, null, null, pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(AssetEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.isNull(any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(root).fetch("folder", JoinType.INNER);
    }

    @Test
    void toPredicate_onlyDeletedAtFilterAlwaysApplied_producesSinglePredicate() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec = captureSpecification(null, null, null, null, null, null, pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate deletedAtPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        doReturn(AssetEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.isNull(path)).thenReturn(deletedAtPredicate);
        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        when(cb.and(andCaptor.capture())).thenReturn(finalPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(finalPredicate);
        assertThat(andCaptor.getValue()).containsExactly(deletedAtPredicate);
    }

    @Test
    void toPredicate_allScalarFiltersProvided_addsOnePredicateEach() {
        Pageable pageable = PageRequest.of(0, 10);
        FolderEntity folder = new FolderEntity();
        Specification<AssetEntity> spec = captureSpecification(folder, "%beach%",
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59), 3, null, pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        doReturn(AssetEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.isNull(any())).thenReturn(predicate);
        when(cb.lower(any())).thenReturn(path);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        when(cb.and(andCaptor.capture())).thenReturn(finalPredicate);

        spec.toPredicate(root, query, cb);

        // folder equal, deletedAt isNull, search like, dateFrom>=, dateTo<=, rating>=
        assertThat(andCaptor.getValue()).hasSize(6);
    }

    @Test
    void toPredicate_emptyTags_noSubqueryBuilt() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec = captureSpecification(null, null, null, null, null, Set.of(), pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(AssetEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.isNull(any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(query, never()).subquery(any());
    }

    @Test
    void toPredicate_nonEmptyTags_addsExistsSubqueryPredicatePerTag() {
        Pageable pageable = PageRequest.of(0, 10);
        Specification<AssetEntity> spec =
                captureSpecification(null, null, null, null, null, Set.of("sunset"), pageable);

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate deletedAtPredicate = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        Subquery subquery = mock(Subquery.class);
        Root subAssetRoot = mock(Root.class);
        Join tagJoin = mock(Join.class);

        doReturn(AssetEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(path);
        when(cb.isNull(any())).thenReturn(deletedAtPredicate);
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(AssetEntity.class)).thenReturn(subAssetRoot);
        when(subAssetRoot.join("tags")).thenReturn(tagJoin);
        when(subAssetRoot.get(anyString())).thenReturn(path);
        when(tagJoin.get(anyString())).thenReturn(path);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(any(Predicate.class))).thenReturn(subquery);
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        // The inner "assetId = :assetId AND tagName = :tagName" combine resolves, at the call
        // site, to CriteriaBuilder's fixed-arity and(Expression<Boolean>, Expression<Boolean>)
        // overload rather than the varargs and(Predicate...) one (Java prefers a fixed-arity
        // match over varargs when both apply) — it's left unstubbed (returns null, which is fine
        // since only the outer combine's return value is asserted on below). Only the final
        // predicates.toArray(...) combine actually goes through the varargs overload stubbed here.
        when(cb.and(any(Predicate[].class))).thenReturn(finalPredicate);
        when(cb.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(finalPredicate);
        verify(cb).exists(subquery);
        verify(subAssetRoot).join("tags");
    }
}
