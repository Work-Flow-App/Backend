package com.workflow;

import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.invoice.InvoicePdfRenderer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractControllerIntegrationTest {

    @MockBean
    protected IStorageService storageService;

    @MockBean
    protected InvoicePdfRenderer pdfRenderer;
}