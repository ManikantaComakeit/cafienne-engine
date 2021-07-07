package org.cafienne.processtask.implementation.calculation.definition.source;

import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.InputSource;
import org.cafienne.processtask.implementation.calculation.operation.Source;

public class InputParameterSourceDefinition implements SourceDefinition {
    private final InputParameterDefinition parameter;

    public InputParameterSourceDefinition(InputParameterDefinition parameter) {
        this.parameter = parameter;
    }

    @Override
    public boolean hasDependency(StepDefinition stepDefinition) {
        return false;
    }

    @Override
    public Source<?> createInstance(Calculation calculation) {
        return new InputSource(this, calculation, parameter);
    }

    @Override
    public String getIdentifier() {
        return parameter.getName();
    }

    @Override
    public String getType() {
        return "Input parameter";
    }
}
