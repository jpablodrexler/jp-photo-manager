package com.jpablodrexler.photomanager.infrastructure.repository;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.repository.AssetRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AssetRepositoryCustomImpl implements AssetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Asset> findByFolderWithFilters(Folder folder, String search, LocalDateTime dateFrom,
                                               LocalDateTime dateTo, Integer minRating, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Asset> query = cb.createQuery(Asset.class);
        Root<Asset> root = query.from(Asset.class);
        root.fetch("folder", JoinType.INNER);
        query.where(buildPredicates(cb, root, folder, search, dateFrom, dateTo, minRating));
        applySort(cb, query, root, pageable.getSort());

        List<Asset> assets = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Asset> countRoot = countQuery.from(Asset.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, folder, search, dateFrom, dateTo, minRating));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(assets, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Asset> root, Folder folder,
                                        String search, LocalDateTime dateFrom, LocalDateTime dateTo,
                                        Integer minRating) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("folder"), folder));
        predicates.add(cb.isNull(root.get("deletedAt")));
        if (search != null) {
            predicates.add(cb.like(cb.lower(root.get("fileName")), search));
        }
        if (dateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fileCreationDateTime"), dateFrom));
        }
        if (dateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fileCreationDateTime"), dateTo));
        }
        if (minRating != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private void applySort(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Asset> root, Sort sort) {
        if (sort.isUnsorted()) {
            query.orderBy(cb.asc(root.get("fileName")));
            return;
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Path<?> path = root.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        query.orderBy(orders);
    }
}
