package com.workflow.service.jobtemplate;

import com.workflow.common.constant.job.JobFieldType;
import com.workflow.entity.Company;
import com.workflow.entity.JobTemplate;
import com.workflow.entity.JobTemplateField;
import com.workflow.entity.Workflow;
import com.workflow.entity.WorkflowStep;
import com.workflow.repository.JobTemplateFieldRepository;
import com.workflow.repository.JobTemplateRepository;
import com.workflow.repository.WorkflowRepository;
import com.workflow.repository.WorkflowStepRepository;
import com.workflow.service.sequence.CompanyCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultTemplateSeederService {

    private static final String SAMPLE_DRYING_JOBS       = "Sample Drying Jobs";
    private static final String SAMPLE_ABC_MECHANICS     = "Sample ABC Mechanics (Demo)";
    private static final String DISASTER_ASSIST_NW       = "Disaster Assist (North West) (Demo)";
    private static final String OFFICE_TASKS             = "Office Tasks (Demo)";

    private final JobTemplateRepository templateRepository;
    private final JobTemplateFieldRepository fieldRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final CompanyCounterService companyCounterService;

    @Transactional
    public void seedDefaultTemplates(Company company) {
        seedSampleDryingJobs(company);
        seedSampleAbcMechanics(company);
        seedDisasterAssistNorthWest(company);
        seedOfficeTasks(company);
    }

    // -------------------------------------------------------------------------
    // Sample Drying Jobs — job template
    // -------------------------------------------------------------------------

    private void seedSampleDryingJobs(Company company) {
        if (templateRepository.existsByCompanyIdAndName(company.getId(), SAMPLE_DRYING_JOBS)) {
            return;
        }

        boolean isFirstTemplate = templateRepository.findByCompanyId(company.getId()).isEmpty();

        JobTemplate template = JobTemplate.builder()
                .company(company)
                .name(SAMPLE_DRYING_JOBS)
                .isDefault(isFirstTemplate)
                .templateRef(companyCounterService.nextTemplateId(company.getId()))
                .build();

        templateRepository.save(template);

        fieldRepository.saveAll(List.of(
                field(template, "customer_name",         "Customer Name",         JobFieldType.TEXT,   true),
                field(template, "address",               "Address",               JobFieldType.TEXT,   false),
                field(template, "postcode",              "Postcode",              JobFieldType.TEXT,   false),
                field(template, "mobile_phone_number",   "Mobile Phone Number",   JobFieldType.NUMBER, false),
                field(template, "landline",              "Landline",              JobFieldType.NUMBER, false),
                field(template, "email_address",         "Email Address",         JobFieldType.TEXT,   false),
                field(template, "description_of_damage", "Description of Damage", JobFieldType.TEXT,   false)
        ));
    }

    // -------------------------------------------------------------------------
    // Sample ABC Mechanics — workflow
    // -------------------------------------------------------------------------

    private void seedSampleAbcMechanics(Company company) {
        if (workflowRepository.existsByCompanyIdAndName(company.getId(), SAMPLE_ABC_MECHANICS)) {
            return;
        }

        Workflow workflow = Workflow.builder()
                .company(company)
                .name(SAMPLE_ABC_MECHANICS)
                .workflowRef(companyCounterService.nextWorkflowId(company.getId()))
                .build();

        workflowRepository.save(workflow);

        workflowStepRepository.saveAll(List.of(
                step(workflow, "Cars booked in",
                        "diaried vehicles coming in.\nto be called 1 day before", 0),
                step(workflow, "Cars in the garage",
                        "customer arrived. \ntag key\ncoffee\ncourt car?", 1),
                step(workflow, "Mechanic report",
                        "Failed or passed info/report written", 2),
                step(workflow, "invoiced and paid by customer",
                        "correct keys\nsatisfaction note ", 3)
        ));
    }

    // -------------------------------------------------------------------------
    // Disaster Assist (North West) — workflow
    // -------------------------------------------------------------------------

    private void seedDisasterAssistNorthWest(Company company) {
        if (workflowRepository.existsByCompanyIdAndName(company.getId(), DISASTER_ASSIST_NW)) {
            return;
        }

        Workflow workflow = Workflow.builder()
                .company(company)
                .name(DISASTER_ASSIST_NW)
                .workflowRef(companyCounterService.nextWorkflowId(company.getId()))
                .build();

        workflowRepository.save(workflow);

        workflowStepRepository.saveAll(List.of(
                step(workflow, "New Instruction received",
                        "Job has been sent via email, text, verbal by Client and has been logged on our system\n\nPlease move along once customer has been contacted and a initial visit has been added to diary.", 0),
                step(workflow, "Booked Awaiting Report",
                        "The site has been visited and the Tech is to submit his report/findings", 1),
                step(workflow, "Awaiting",
                        "We need approval or a 3rd party to carry out a procedure such as ACM removal before moving this on to works to book", 2),
                step(workflow, "Approved works to book",
                        "We have the approval and now to make arrangements to book those works in with the customer. \n\nMove along when in the diary.", 3),
                step(workflow, "Work in progress ",
                        "the job is in progress, it has been diaried, workers have been assigned and client CRM is updated  with dates.\n\nWorks now will be logged by site labour in form of site logs, information to be taken from there for CRMs", 4),
                step(workflow, "Revisits",
                        "Site work is complete, this could be job is still drying and requires revisits.", 5),
                step(workflow, "Job Complete - to invoice",
                        "Provide all required handover documents and invoice", 6),
                step(workflow, "Rejected invoices",
                        "Needs amending and re submission", 7),
                step(workflow, "Storage",
                        "Any job complete and in our store or 3rd party store", 8)
        ));
    }

    // -------------------------------------------------------------------------
    // Office Tasks — workflow
    // -------------------------------------------------------------------------

    private void seedOfficeTasks(Company company) {
        if (workflowRepository.existsByCompanyIdAndName(company.getId(), OFFICE_TASKS)) {
            return;
        }

        Workflow workflow = Workflow.builder()
                .company(company)
                .name(OFFICE_TASKS)
                .workflowRef(companyCounterService.nextWorkflowId(company.getId()))
                .build();

        workflowRepository.save(workflow);

        workflowStepRepository.saveAll(List.of(
                step(workflow, "New Tasks ",
                        "Add important tasks any admin or assigned admin can complete / cut down communications ie whatsapp & emails", 0),
                step(workflow, "in progress",
                        "Please input initials and current status, upload any supporting documents etc", 1),
                step(workflow, "Cannot complete need assistance",
                        "Details needed ", 2),
                step(workflow, "Task complete",
                        "archive if happy", 3)
        ));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JobTemplateField field(JobTemplate template, String name, String label,
                                   JobFieldType type, boolean required) {
        return JobTemplateField.builder()
                .template(template)
                .name(name)
                .label(label)
                .jobFieldType(type)
                .required(required)
                .options("")
                .build();
    }

    private WorkflowStep step(Workflow workflow, String name, String description, int orderIndex) {
        return WorkflowStep.builder()
                .workflow(workflow)
                .name(name)
                .description(description)
                .orderIndex(orderIndex)
                .optional(false)
                .build();
    }
}
