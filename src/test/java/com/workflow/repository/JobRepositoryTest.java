package com.workflow.repository;

import com.workflow.common.constant.Role;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.repository.job.JobRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("JobRepository Integration Tests")
class JobRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JobRepository jobRepository;

    private Company testCompany;
    private JobTemplate template;

    @BeforeEach
    void setUp() {
        User companyUser = User.builder()
                .uuid("jobrepo-company-uuid")
                .username("jobrepocompany")
                .email("jobrepo@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        entityManager.persist(companyUser);

        testCompany = Company.builder()
                .name("Job Repo Test Company")
                .user(companyUser)
                .archived(false)
                .build();
        entityManager.persist(testCompany);

        template = JobTemplate.builder()
                .company(testCompany)
                .name("Test Template")
                .build();
        entityManager.persist(template);

        entityManager.flush();
    }

    private Job persistJobWithCreatedAt(Company company, boolean archived, LocalDateTime createdAt) {
        Job job = Job.builder()
                .template(template)
                .company(company)
                .status(JobStatus.NEW)
                .archived(archived)
                .build();
        entityManager.persist(job);
        entityManager.flush();

        // @CreationTimestamp overwrites whatever createdAt is set on the builder at insert time,
        // so backdating requires a direct update after the row exists, then clearing the
        // persistence context so subsequent reads see the DB's real value, not the stale cached entity.
        entityManager.createNativeQuery("UPDATE jobs SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, job.getId())
                .executeUpdate();
        entityManager.clear();

        return job;
    }

    @Test
    @DisplayName("Should count jobs created within the given range")
    void countByCompanyIdAndCreatedAtBetween_CountsJobsInRange() {
        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        persistJobWithCreatedAt(testCompany, false, startOfMonth.plusDays(1));
        persistJobWithCreatedAt(testCompany, false, startOfMonth.plusDays(2));

        long count = jobRepository.countByCompanyIdAndCreatedAtBetween(testCompany.getId(), startOfMonth, startOfNextMonth);

        assertEquals(2L, count);
    }

    @Test
    @DisplayName("Should not count jobs created before the range")
    void countByCompanyIdAndCreatedAtBetween_ExcludesJobsBeforeRange() {
        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        persistJobWithCreatedAt(testCompany, false, startOfMonth.minusDays(1)); // last month — excluded
        persistJobWithCreatedAt(testCompany, false, startOfMonth.plusDays(1)); // this month — included

        long count = jobRepository.countByCompanyIdAndCreatedAtBetween(testCompany.getId(), startOfMonth, startOfNextMonth);

        assertEquals(1L, count);
    }

    @Test
    @DisplayName("Should not count jobs created at or after the exclusive end boundary")
    void countByCompanyIdAndCreatedAtBetween_ExcludesJobsAtEndBoundary() {
        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        persistJobWithCreatedAt(testCompany, false, startOfNextMonth); // next month starts here — excluded

        long count = jobRepository.countByCompanyIdAndCreatedAtBetween(testCompany.getId(), startOfMonth, startOfNextMonth);

        assertEquals(0L, count);
    }

    @Test
    @DisplayName("Should still count archived jobs — the cap is about creation load this month, not current active count")
    void countByCompanyIdAndCreatedAtBetween_CountsArchivedJobsToo() {
        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        persistJobWithCreatedAt(testCompany, true, startOfMonth.plusDays(1)); // archived

        long count = jobRepository.countByCompanyIdAndCreatedAtBetween(testCompany.getId(), startOfMonth, startOfNextMonth);

        assertEquals(1L, count);
    }

    @Test
    @DisplayName("Should scope the count to the given company only")
    void countByCompanyIdAndCreatedAtBetween_ScopesByCompany() {
        User anotherCompanyUser = User.builder()
                .uuid("jobrepo-company-uuid-2")
                .username("jobrepocompany2")
                .email("jobrepo2@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        entityManager.persist(anotherCompanyUser);

        Company anotherCompany = Company.builder()
                .name("Another Job Repo Company")
                .user(anotherCompanyUser)
                .archived(false)
                .build();
        entityManager.persist(anotherCompany);

        JobTemplate anotherTemplate = JobTemplate.builder()
                .company(anotherCompany)
                .name("Another Template")
                .build();
        entityManager.persist(anotherTemplate);
        entityManager.flush();

        LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        persistJobWithCreatedAt(testCompany, false, startOfMonth.plusDays(1));

        Job otherCompanyJob = Job.builder()
                .template(anotherTemplate)
                .company(anotherCompany)
                .status(JobStatus.NEW)
                .archived(false)
                .build();
        entityManager.persist(otherCompanyJob);
        entityManager.flush();

        long count = jobRepository.countByCompanyIdAndCreatedAtBetween(testCompany.getId(), startOfMonth, startOfNextMonth);

        assertEquals(1L, count);
    }
}
