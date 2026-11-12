package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大模型调用用量记录（用于统计与计费）
 */
@Data
@Entity
@Table(name = "usage_record")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** GENERATE / POLISH / RECOMMEND / TEST */
    @Column(nullable = false, length = 20)
    private String operation;

    private Long templateId;

    private Long workId;

    @Column(nullable = false)
    private Integer promptTokens = 0;

    @Column(nullable = false)
    private Integer completionTokens = 0;

    @Column(nullable = false)
    private Integer totalTokens = 0;

    /** 本次费用（元），免费额度内为0 */
    @Column(nullable = false)
    private Double cost = 0.0;

    /** 是否真实调用大模型（false表示本地模拟） */
    @Column(nullable = false)
    private Boolean realCall = false;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(length = 500)
    private String errorMsg;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
