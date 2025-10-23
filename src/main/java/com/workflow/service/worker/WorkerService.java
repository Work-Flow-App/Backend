package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.customException.UserAlreadyExistsException;
import com.workflow.common.exception.customException.WorkerAlreadyExistsException;
import com.workflow.common.exception.customException.WorkerNotFoundException;
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

        // Check if email is being changed and already exists
        if (request.email() != null && !request.email().isBlank() &&
                !request.email().equalsIgnoreCase(worker.getEmail()) &&
                workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(request.email())) {
            throw new WorkerAlreadyExistsException("Worker with email '" + request.email() + "' already exists");
        }

        // Update worker fields
        worker.setName(request.name());
        worker.setInitials(request.initials());
        worker.setTelephone(request.telephone());
        worker.setMobile(request.mobile());
        worker.setEmail(request.email());

        Worker updatedWorker = workerRepository.save(worker);
        log.info("Updated worker with ID: {}", workerId);

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

    @Override
    @Transactional(readOnly = true)
    public WorkerInviteResponse sendInvitation(Long workerId, Long companyUserId) {
        Company company = companyService.findCompanyByUserId(companyUserId);

        Worker worker = workerRepository.findByIdAndCompanyIdAndNotArchived(workerId, company.getId())
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with ID: " + workerId));

        // TODO: Implement actual email sending logic here
        // For now, just return a response indicating invitation would be sent
        log.info("Invitation email would be sent to worker: {} at {}", worker.getName(), worker.getEmail());

        return new WorkerInviteResponse(
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                "Invitation email will be sent to " + worker.getEmail()
        );
    }
}