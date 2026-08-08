package com.workflow.service.worker;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.UserAlreadyExistsException;
import com.workflow.common.exception.business.WorkerAlreadyExistsException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerPasswordResetRequest;
import com.workflow.dto.worker.WorkerProfileResponse;
import com.workflow.dto.worker.WorkerRateUpdateRequest;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.dto.worker.WorkerWeeklyHoursResponse;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.JobWorkflowStepVisitLog;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.job.JobWorkflowStepVisitLogRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.storage.IStorageService;
import com.workflow.service.subscription.IStorageQuotaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService implements IWorkerService {

    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5 MB

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final ICompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final CompanyCounterService companyCounterService;
    private final JobWorkflowStepVisitLogRepository visitLogRepository;
    private final Tika tika;
    private final IStorageService s3Service;
    private final IStorageQuotaService storageQuotaService;

    @Value("${workflow.security.file.blocked-types}")
    private List<String> blockedTypes;

    @Override
    @Transactional
    public WorkerResponse createWorker(WorkerCreateRequest request, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        // Check if username already exists
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }

        // Check if email already exists for another worker
        if (request.email() != null && !request.email().isBlank() &&
                workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(request.email())) {
            throw new WorkerAlreadyExistsException("Worker with email '" + request.email() + "' already exists");
        }

        // Create User account for worker
        User workerUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(Role.WORKER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(workerUser);

        // Create Worker entity
        Worker worker = Worker.builder()
                .name(request.name())
                .initials(request.initials())
                .telephone(request.telephone())
                .mobile(request.mobile())
                .email(request.email())
                .company(company)
                .user(savedUser)
                .loginLocked(false)
                .archived(false)
                .workerRef(companyCounterService.nextWorkerId(company.getId()))
                .build();

        Worker savedWorker = workerRepository.save(worker);
        log.info("Created worker with ID: {} for company: {}", savedWorker.getId(), company.getName());

        return map(savedWorker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getAllWorkers(Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        List<Worker> workers = workerRepository.findByCompanyIdAndNotArchived(company.getId());

        return workers.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerById(Long workerId, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        return map(worker);
    }

    @Override
    @Transactional
    public WorkerResponse updateWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        // Check if email is being changed and already exists in workers table
        if (request.email() != null && !request.email().isBlank() &&
                !request.email().equalsIgnoreCase(worker.getEmail())) {
            if (workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(request.email())) {
                throw new WorkerAlreadyExistsException("Worker with email '" + request.email() + "' already exists");
            }
            // Check if email exists in users table (excluding current worker's user)
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), worker.getUser().getId())) {
                throw new UserAlreadyExistsException("Email '" + request.email() + "' is already in use");
            }
        }

        // Update worker fields
        worker.setName(request.name());
        worker.setInitials(request.initials());
        worker.setTelephone(request.telephone());
        worker.setMobile(request.mobile());
        worker.setEmail(request.email());
        // Sync email with User entity and persist
        worker.getUser().setEmail(request.email());
        userRepository.save(worker.getUser());

        Worker updatedWorker = workerRepository.save(worker);
        log.info("Updated worker with ID: {}", workerId);

        return map(updatedWorker);
    }

    @Override
    @Transactional
    public WorkerResponse patchWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        // Check if email is being changed and already exists in workers table
        if (request.email() != null && !request.email().isBlank() &&
                !request.email().equalsIgnoreCase(worker.getEmail())) {
            if (workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(request.email())) {
                throw new WorkerAlreadyExistsException("Worker with email '" + request.email() + "' already exists");
            }
            // Check if email exists in users table (excluding current worker's user)
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), worker.getUser().getId())) {
                throw new UserAlreadyExistsException("Email '" + request.email() + "' is already in use");
            }
        }

        // Patch only non-null fields
        if (request.name() != null) {
            worker.setName(request.name());
        }
        if (request.initials() != null) {
            worker.setInitials(request.initials());
        }
        if (request.telephone() != null) {
            worker.setTelephone(request.telephone());
        }
        if (request.mobile() != null) {
            worker.setMobile(request.mobile());
        }
        if (request.email() != null) {
            worker.setEmail(request.email());
            // Sync email with User entity and persist
            worker.getUser().setEmail(request.email());
            userRepository.save(worker.getUser());
        }

        Worker updatedWorker = workerRepository.save(worker);
        log.info("Patched worker with ID: {}", workerId);

        return map(updatedWorker);
    }

    @Override
    @Transactional
    public void deleteWorker(Long workerId, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        // Soft delete - set archived to true
        worker.setArchived(true);
        workerRepository.save(worker);
        log.info("Archived worker with ID: {}", workerId);
    }

    @Override
    @Transactional
    public void resetWorkerUsernamePassword(Long workerId, WorkerPasswordResetRequest request, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        worker.getUser().setPassword(passwordEncoder.encode(request.newPassword()));

        if (request.newUsername() != null && !request.newUsername().isBlank()) {
            userRepository.findByUsername(request.newUsername()).ifPresent(existing -> {
                if (!existing.getId().equals(worker.getUser().getId())) {
                    throw new UserAlreadyExistsException("Username '" + request.newUsername() + "' is already taken");
                }
            });
            worker.getUser().setUsername(request.newUsername());
        }

        userRepository.save(worker.getUser());
        log.info("Reset credentials for worker ID: {} in company: {}", workerId, company.getId());
    }

    /*
     * ===========================
     * SELF-SERVICE PROFILE
     * ===========================
     */

    private Worker getWorker(Long userId) {
        return workerRepository.findByUserIdAndArchivedFalse(userId)
                .orElseThrow(() -> new WorkerNotFoundException("Current user is not a registered worker"));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerProfileResponse getOwnProfile(Long workerUserId) {
        Worker worker = getWorker(workerUserId);
        return WorkerProfileResponse.fromEntity(worker, s3Service.resolveFileUrl(worker.getPhotoUrl()));
    }

    @Override
    @Transactional
    public WorkerProfileResponse uploadOwnPhoto(Long workerUserId, MultipartFile file) throws IOException {
        Worker worker = getWorker(workerUserId);
        uploadPhoto(worker, file);
        return WorkerProfileResponse.fromEntity(worker, s3Service.resolveFileUrl(worker.getPhotoUrl()));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerWeeklyHoursResponse getOwnWeeklyHours(Long workerUserId, LocalDate anyDateInWeek) {
        Worker worker = getWorker(workerUserId);
        return calculateWeeklyHours(worker, anyDateInWeek);
    }

    /*
     * ===========================
     * ADMIN-FACING
     * ===========================
     */

    @Override
    @Transactional
    public WorkerResponse uploadPhotoForWorker(Long workerId, Long companyUserId, MultipartFile file) throws IOException {
        Company company = companyService.findCompanyByUserId(companyUserId);
        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        uploadPhoto(worker, file);
        return map(worker);
    }

    @Override
    @Transactional
    public WorkerResponse updateHourlyRate(Long workerId, WorkerRateUpdateRequest request, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);
        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        worker.setHourlyRate(request.hourlyRate());
        Worker updatedWorker = workerRepository.save(worker);
        log.info("Updated hourly rate for worker ID: {}", workerId);

        return map(updatedWorker);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerWeeklyHoursResponse getWeeklyHoursForWorker(Long workerId, Long companyUserId, LocalDate anyDateInWeek) {
        Company company = companyService.findCompanyByUserId(companyUserId);
        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        return calculateWeeklyHours(worker, anyDateInWeek);
    }

    /*
     * ===========================
     * INTERNAL HELPERS
     * ===========================
     */

    private void uploadPhoto(Worker worker, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyFileException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new FileSizeLimitExceededException("Photo must not exceed 5 MB");
        }

        String detectedType = tika.detect(file.getInputStream());

        if (blockedTypes.contains(detectedType)) {
            throw new ForbiddenActionException("Upload is blocked for security reasons.");
        }

        // Lock the worker row for the rest of this transaction so a concurrent
        // upload can't read the same stale photoUrl and both delete/overwrite it.
        workerRepository.findByIdForUpdate(worker.getId());

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String safeUniqueFilename = UUID.randomUUID() + extension;

        String key = String.format(
                "companies/%d/workers/%d/photo/%s",
                worker.getCompany().getId(),
                worker.getId(),
                safeUniqueFilename);

        Long companyId = worker.getCompany().getId();
        storageQuotaService.assertCapacity(companyId, file.getSize());
        s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);

        String previousKey = worker.getPhotoUrl();
        Long previousSizeBytes = worker.getPhotoSizeBytes();
        worker.setPhotoUrl(key);
        worker.setPhotoSizeBytes(file.getSize());
        workerRepository.save(worker);
        storageQuotaService.recordUpload(companyId, file.getSize());

        // Delete the old photo only after the transaction commits - if the commit
        // fails, the DB rolls back to previousKey and the old S3 object must still exist.
        if (previousKey != null) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        s3Service.delete(previousKey);
                        // Legacy rows uploaded before photoSizeBytes existed have nothing to decrement
                        if (previousSizeBytes != null) {
                            storageQuotaService.recordDelete(companyId, previousSizeBytes);
                        }
                    }
                });
            } else {
                s3Service.delete(previousKey);
                if (previousSizeBytes != null) {
                    storageQuotaService.recordDelete(companyId, previousSizeBytes);
                }
            }
        }
    }

    private WorkerWeeklyHoursResponse calculateWeeklyHours(Worker worker, LocalDate anyDateInWeek) {
        LocalDate reference = anyDateInWeek != null ? anyDateInWeek : LocalDate.now();
        LocalDate weekStart = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = reference.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<JobWorkflowStepVisitLog> logs = visitLogRepository.findByLoggedByIdAndVisitDateBetween(
                worker.getUser().getId(), weekStart, weekEnd);

        Duration totalDuration = Duration.ZERO;
        boolean hasOpenVisit = false;

        for (JobWorkflowStepVisitLog visitLog : logs) {
            if (visitLog.getTimeOut() == null) {
                hasOpenVisit = true;
                continue;
            }
            totalDuration = totalDuration.plus(Duration.between(visitLog.getTimeIn(), visitLog.getTimeOut()));
        }

        BigDecimal totalHours = BigDecimal.valueOf(totalDuration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return new WorkerWeeklyHoursResponse(worker.getId(), weekStart, weekEnd, totalHours, hasOpenVisit);
    }

    private WorkerResponse map(Worker worker) {
        return WorkerResponse.fromEntity(worker, s3Service.resolveFileUrl(worker.getPhotoUrl()));
    }
}
