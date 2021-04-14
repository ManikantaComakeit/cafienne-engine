package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.FilterStepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

public class FilterExpressionDefinition extends ConditionDefinition {
    private final String inputName;
    private final String elementName;

    public FilterExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        FilterStepDefinition parent = getParentElement();
        inputName = parent.assertOneInput();
        elementName = parseAttribute("element", false, inputName);
    }

    @Override
    public Result getResult(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        return new ResultCreator(calculation, step, sourceMap).result;
    }

    @Override
    public String getType() {
        return "Filter";
    }

    class ResultCreator {
        private final Calculation calculation;
        private final CalculationStep step;
        private final Value input;
        private final Result result;

        ResultCreator(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
            this.calculation = calculation;
            this.step = step;
            this.input = sourceMap.get(inputName);
            this.result = new Result(calculation, step, getFilteredValue());
        }

        private Value getFilteredValue() {
            if (input.isList()) {
                // Filter the list and return a list with the filtered items only.
                Object[] items = input.asList().stream().filter(this::isFilteredItem).toArray();
                // Note: "items" is always an array of type Value
                return new ValueList(items);
            } else {
                // Instead of the list, we will only check if the given input object matches the filter.
                // If so, then we return that object, otherwise we return a null value (is that the best choice?)
                if (isFilteredItem(input)) {
                    return input;
                } else {
                    return Value.NULL;
                }
            }
        }

        private boolean isFilteredItem(Value item) {
            // In the expression, the input element can only be accessed through the element name
            ValueMap sourceMap = new ValueMap(elementName, item);
            return getBooleanResult(calculation, step, sourceMap);
        }
    }
}
