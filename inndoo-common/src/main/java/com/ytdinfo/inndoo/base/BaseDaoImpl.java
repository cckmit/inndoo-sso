package com.ytdinfo.inndoo.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.List;

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

/**
 * @author timmy
 * @date 2019/10/11
 */
public class BaseDaoImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements BaseDao<T, ID>{

    /**
     * 父类没有不带参数的构造方法，这里手动构造父类
     * @param entityInformation
     * @param entityManager
     */
    public BaseDaoImpl(Class<T> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.em = entityManager;
        this.entityInformation = entityInformation;
    }

    @PersistenceContext
    private EntityManager em;

    private final Class<T> entityInformation;

    @Override
    public Page<T> findAll(@Nullable Specification<T> spec, Pageable pageable) {

        //如果分页前5000笔，采用默认方式
        if(pageable.getOffset() < 5000){
            return super.findAll(spec,pageable);
        }
        //超过5000，先查询出Id，然后再采用in查询，提高性能，防止扫描全表
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();

        Class<T> domainClass = entityInformation;
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<T> root = query.from(domainClass);
        spec.toPredicate(root, query, criteriaBuilder);
        query.select(root.get("id"));
        if (sort.isSorted()) {
            query.orderBy(toOrders(sort, root, criteriaBuilder));
        }

        TypedQuery<String> typedQuery = em.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<String> resultList = typedQuery.getResultList();

        Path<String> pathId = root.get("id");
        CriteriaBuilder.In<String> idsIn = criteriaBuilder.in(pathId);
        for (String id:resultList){
            idsIn.value(id);
        }
        Predicate predicateIn = criteriaBuilder.and(idsIn);

        CriteriaQuery<T> queryMain = criteriaBuilder.createQuery(domainClass);
        Root<T> rootMain = queryMain.from(domainClass);
        queryMain.select(rootMain);
        queryMain.where(predicateIn);
        if (sort.isSorted()) {
            queryMain.orderBy(toOrders(sort, rootMain, criteriaBuilder));
        }
        TypedQuery<T> typedQueryMain = em.createQuery(queryMain);
        List<T> resultListMain = typedQueryMain.getResultList();

        return PageableExecutionUtils.getPage(resultListMain, pageable,
                () -> executeCountQuery(getCountQuery(spec, domainClass)));
    }

    /**
     * Executes a count query and transparently sums up all values returned.
     *
     * @param query must not be {@literal null}.
     * @return
     */
    private static long executeCountQuery(TypedQuery<Long> query) {

        Assert.notNull(query, "TypedQuery must not be null!");

        List<Long> totals = query.getResultList();
        long total = 0L;

        for (Long element : totals) {
            total += element == null ? 0 : element;
        }

        return total;
    }

    @Override
    public List<T> findOnePage(Specification<T> spec, Pageable pageable) {
        TypedQuery<T> query = getQuery(spec, pageable);
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList();
    }
}