package com.workflow.templates.pdf.estimate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class EstimatePdfRenderer {

    private static final String TEMPLATE = "pdf/estimate/estimate";

    private final SpringTemplateEngine templateEngine;

    public byte[] render(EstimateTemplateData data) {
        Context context = new Context();
        context.setVariable("data", data);
        String html = templateEngine.process(TEMPLATE, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render estimate PDF", e);
        }
    }
}
