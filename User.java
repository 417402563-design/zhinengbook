package cn.edu.sdua.zhinengbook.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台用户
 */
@Data
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(length = 50)
    private String nickname;

    /** USER / ADMIN */
    @Column(nullable = false, length = 20)
    private String role = "USER";

    /** NORMAL / BANNED */
    @Column(nullable = false, length = 20)
    private String status = "NORMAL";

    /** 累计消耗token数 */
    @Column(nullable = false)
    private Long tokensUsed = 0L;

    /** 累计费用（元） */
    @Column(nullable = false)
    private Double costIncurred = 0.0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
