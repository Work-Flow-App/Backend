package com.workflow.repository.form;

import com.workflow.entity.form.FormField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FormFieldRepository extends JpaRepository<FormField, Long> {

    List<FormField> findByTemplateIdOrderByOrderIndexAsc(Long templateId);

}