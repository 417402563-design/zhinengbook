package cn.edu.sdua.zhinengbook.repository;

import cn.edu.sdua.zhinengbook.entity.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkRepository extends JpaRepository<Work, Long> {

    List<Work> findByUserIdOrderByUpdatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
