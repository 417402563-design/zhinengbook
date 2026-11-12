package cn.edu.sdua.zhinengbook.repository;

import cn.edu.sdua.zhinengbook.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UsageRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(u.totalTokens), 0) from UsageRecord u where u.userId = ?1")
    long sumTokensByUser(Long userId);

    @Query("select coalesce(sum(u.cost), 0.0) from UsageRecord u where u.userId = ?1")
    double sumCostByUser(Long userId);

    long countByCreatedAtAfter(LocalDateTime time);

    @Query("select u.operation, count(u), coalesce(sum(u.totalTokens), 0), coalesce(sum(u.cost), 0.0) " +
           "from UsageRecord u group by u.operation")
    List<Object[]> groupByOperation();

    @Query("select u.userId, count(u), coalesce(sum(u.totalTokens), 0), coalesce(sum(u.cost), 0.0) " +
           "from UsageRecord u group by u.userId order by sum(u.totalTokens) desc")
    List<Object[]> groupByUser();
}
