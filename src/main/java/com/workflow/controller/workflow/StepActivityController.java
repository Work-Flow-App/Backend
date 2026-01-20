package com.workflow.controller.workflow;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IStepActivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/job-workflow-steps")
@RequiredArgsConstructor
public class StepActivityController {

    private final IStepActivityService stepActivityService;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @GetMapping("/{stepId}/timeline")
    public ResponseEntity<List<StepActivityResponse>> getTimeline(
            @PathVariable Long stepId,
            Authentication auth) {

        return ResponseEntity.ok(
                stepActivityService.getTimeline(
                        stepId,
                        getCompanyId(auth)));
    }
}
