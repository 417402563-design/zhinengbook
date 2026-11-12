package cn.edu.sdua.zhinengbook.repository;

import cn.edu.sdua.zhinengbook.entity.WorkVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkVersionRepository extends JpaRepository<WorkVersion, Long> {

    List<WorkVersion> findByWorkIdOrderByVersionNoDesc(Long workId);

    Optional<WorkVersion> findFirstByWorkIdOrderByVersionNoDesc(Long workId);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(max(v.versionNo), 0) from WorkVersion v where v.workId = ?1")
    int maxVersionNo(Long workId);
}
