package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.UserAlreadyExistsException;
import com.workflow.common.exception.business.WorkerAlreadyExistsException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerInviteResponse;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.entity.Worker;
import com.workflow.repository.UserRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.service.company.CompanyService;
import com.workflow.service.sequence.CompanyCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService implements IWorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final CompanyCounterService companyCounterService;

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

        return WorkerResponse.fromEntity(savedWorker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerResponse> getAllWorkers(Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        List<Worker> workers = workerRepository.findByCompanyIdAndNotArchived(company.getId());

        return workers.stream()
                .map(WorkerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerResponse getWorkerById(Long workerId, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        return WorkerResponse.fromEntity(worker);
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

        return WorkerResponse.fromEntity(updatedWorker);
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

        return WorkerResponse.fromEntity(updatedWorker);
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
}