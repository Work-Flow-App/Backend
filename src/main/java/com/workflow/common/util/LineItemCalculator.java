package com.workflow.common.util;

import com.workflow.entity.financial.EstimateLineItem;
import com.workflow.entity.financial.LineItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class LineItemCalculator {

    private LineItemCalculator() {}

    public static void recalculate(LineItem item) {
        BigDecimal net = item.getUnitPrice()
                .multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = net
                .multiply(item.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setNetAmount(net);
        item.setVatAmount(vat);
        item.setTotalAmount(net.add(vat));
    }

    public static void recalculate(EstimateLineItem item) {
        BigDecimal net = item.getUnitPrice()
                .multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = net
                .multiply(item.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setNetAmount(net);
        item.setVatAmount(vat);
        item.setTotalAmount(net.add(vat));
    }
}
