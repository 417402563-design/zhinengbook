package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户作品
 */
@Data
@Entity
@Table(name = "work")
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    /** 当前正文内容 */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    /** 创作时使用的模板 */
    private Long templateId;

    @Column(length = 50)
    private String category;

    /** DRAFT / PUBLISHED */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    /** 当前版本号 */
    @Column(nullable = false)
    private Integer currentVersion = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
