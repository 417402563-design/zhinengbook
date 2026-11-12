package cn.edu.sdua.zhinengbook.repository;

import cn.edu.sdua.zhinengbook.entity.ApiKeyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKeyConfig, Long> {

    Optional<ApiKeyConfig> findFirstByActiveTrue();

    List<ApiKeyConfig> findAllByOrderByIdAsc();
}
