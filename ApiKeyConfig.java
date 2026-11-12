package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大模型API密钥配置
 */
@Data
@Entity
@Table(name = "api_key_config")
public class ApiKeyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** dashscope / openai / deepseek / moonshot ... */
    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 300)
    private String apiKey;

    /** 同一时间仅一个激活密钥生效 */
    @Column(nullable = false)
    private Boolean active = false;

    /** UNTESTED / OK / FAILED */
    @Column(nullable = false, length = 20)
    private String status = "UNTESTED";

    @Column(length = 500)
    private String remark;

    private LocalDateTime lastTestedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
