package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 10)
    private String initials;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(length = 20)
    private String telephone;

    @Column(length = 20)
    private String mobile;

    @Column(length = 100)
    private String email;

    @Column(name = "login_locked", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean loginLocked = false;

    @Column(name = "archived", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean archived = false;
}
