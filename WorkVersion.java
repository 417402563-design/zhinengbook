package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品历史版本
 */
@Data
@Entity
@Table(name = "work_version", uniqueConstraints = @UniqueConstraint(columnNames = {"workId", "versionNo"}))
public class WorkVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workId;

    @Column(nullable = false)
    private Integer versionNo;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    /** GENERATE 生成 / POLISH 润色 / EDIT 手动编辑 / RESTORE 版本回滚 */
    @Column(nullable = false, length = 20)
    private String changeType;

    @Column(length = 200)
    private String changeNote;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
