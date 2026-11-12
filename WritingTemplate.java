package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 写作模板（多场景）
 */
@Data
@Entity
@Table(name = "writing_template")
public class WritingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 场景分类：公文/营销/学术/文学/办公/新媒体 等 */
    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    /** 大模型系统提示词（角色设定） */
    @Column(length = 2000)
    private String systemPrompt;

    /** 用户提示词模板，支持 {{变量名}} 占位符 */
    @Column(length = 4000)
    private String promptTemplate;

    /** 变量定义 JSON，如 [{"key":"topic","label":"主题","required":true}] */
    @Column(length = 2000)
    private String variables;

    /** 是否系统内置模板 */
    @Column(nullable = false)
    private Boolean builtin = false;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
