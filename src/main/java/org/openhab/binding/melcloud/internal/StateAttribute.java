package org.openhab.binding.melcloud.internal;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.Nullable;

public class StateAttribute {

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal step;
    private String pattern;
    private boolean readOnly;

    public StateAttribute(BigDecimal min, BigDecimal max, @Nullable BigDecimal step, @Nullable String pattern,
            boolean readOnly) {
        super();
        this.min = min;
        this.max = max;
        this.step = step;
        this.pattern = pattern;
        this.readOnly = readOnly;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public BigDecimal getStep() {
        return step;
    }

    public void setStep(BigDecimal step) {
        this.step = step;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

}
