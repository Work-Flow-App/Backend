    package com.workflow.entity;

    import jakarta.persistence.*;
    import lombok.*;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Entity
    @Table(name = "clients")
    public class Client {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 150)
        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "company_id", nullable = false)
        private Company company;

        @Column(length = 100)
        private String email;

        @Column(length = 20)
        private String telephone;

        @Column(length = 20)
        private String mobile;

        @Column(length = 255)
        private String address;

        @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
        private boolean archived = false;
    }
