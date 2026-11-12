package cn.edu.sdua.zhinengbook.repository;

import cn.edu.sdua.zhinengbook.entity.WritingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<WritingTemplate, Long> {

    List<WritingTemplate> findByEnabledTrueOrderByUsageCountDesc();

    List<WritingTemplate> findAllByOrderByUsageCountDesc();

    long countByCategory(String category);
}
