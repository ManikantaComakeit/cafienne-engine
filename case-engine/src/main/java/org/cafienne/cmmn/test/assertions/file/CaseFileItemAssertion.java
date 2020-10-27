package org.cafienne.cmmn.test.assertions.file;

import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.ModelTestCommand;
import org.cafienne.cmmn.test.assertions.ModelTestCommandAssertion;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;

import java.util.ArrayList;
import java.util.List;

public class CaseFileItemAssertion extends ModelTestCommandAssertion {
    private final Path path;
    private final PublishedEventsAssertion<CaseFileEvent> events = new PublishedEventsAssertion(new ArrayList());
    private final CaseFileAssertion caseFileAssertion;
    private final boolean isArrayElement;
    private int indexInArray = -1;

    CaseFileItemAssertion(CaseFileAssertion caseFileAssertion, ModelTestCommand command, Path path) {
        super(command);
        this.caseFileAssertion = caseFileAssertion;
        this.path = path;
        this.isArrayElement = path.index >= 0;
    }

    void addEvent(CaseFileEvent event) {
        // Reverse order of events; most recent one always at front
        this.events.getEvents().add(0, event);
        indexInArray = event.getIndex();
    }

    int getIndexInArray() {
        return indexInArray;
    }

    /**
     * Asserts that this case file item is an array.
     *
     * @return
     */
    private CaseFileItemAssertion assertIterable() {
        Object something = this.events.getEvents().stream().filter(e -> e.getPath().index >= 0).findFirst();
        if (something == null) {
            // There should be at least one 'array' like event, right?
            throw new AssertionError("Case file item " + getName() + " is expected to be an array, but it is not");
        }
        return this;
    }

    /**
     * Asserts that this is an array and contains the expected number of elements.
     *
     * @param expectedSize
     * @return
     */
    public CaseFileItemAssertion assertSize(int expectedSize) {
        assertIterable();
        List<CaseFileItemAssertion> elements = caseFileAssertion.getArrayElements(path);
        if (elements.size() != expectedSize) {
            throw new AssertionError("Case file item array " + getName() + " is expected to contain " + expectedSize + " elements, but it has " + elements.size());
        }
        return this;
    }

    /**
     * Returns a CaseFileItemAssertion for a child that corresponds with the relative path
     *
     * @param relativePath item path, starting from this case file item.
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertCaseFileItem(Path relativePath) {
        Path absolutePath = new Path(this.path.toString() + "/" + relativePath.toString());
        return caseFileAssertion.assertCaseFileItem(absolutePath);
    }

    public CaseFileItemAssertion assertArrayElement(int index) {
        return caseFileAssertion.assertCaseFileItem(new Path(this.path.toString() + "[" + index + "]"));
    }

    /**
     * Assert the item expected value with the actual value
     *
     * @param expectedValue expected value
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertValue(Value<?> expectedValue) {
        if (!getValue().equals(expectedValue)) {
            throw new AssertionError("The value of case file item " + getName() + " does not match the expected value.\nFound:\n" + getValue() + "\nExpected:\n" + expectedValue);
        }
        return this;
    }

    /**
     * Asserts the CaseFileItem value has the specified json type class
     *
     * @param vClass
     * @return
     */
    public CaseFileItemAssertion assertValueType(Class<? extends Value<?>> vClass) {
        if (!getValue().getClass().equals(vClass)) {
            throw new AssertionError("The value of case file item " + getName() + " does not match the expected type '" + vClass.getSimpleName() + "', found '" + getValue().getClass().getSimpleName() + "' instead");
        }
        return this;
    }

    /**
     * Asserts that the case file item is in the expected state.
     *
     * @param expectedState expected state of the item
     */
    public CaseFileItemAssertion assertState(State expectedState) {
        State actualState = getState();
        if (!actualState.equals(expectedState)) {
            throw new AssertionError("CaseFileItem " + getName() + " is not in state " + expectedState + " but " + actualState);
        }
        return this;
    }

    /**
     * Returns current value of case file item (i.e., value as passed in last event, or Value.NULL if there are no events)
     *
     * @return
     */
    public Value getValue() {
        Value value = getEventValue(e -> e.getValue(), Value.NULL);
        if (!isArrayElement && value == Value.NULL) {

            final List<CaseFileItemAssertion> arrayElements = caseFileAssertion.getArrayElements(this.path);
            if (arrayElements.isEmpty()) {
                return value;
            }
//            System.out.println("Composing value ");
            ValueList list = new ValueList();
            arrayElements.forEach(a -> list.add(a.getValue()));
            value = list;
        }
        return value;
    }

    private State getState() {
        return getEventValue(e -> e.getState(), State.Null);
    }

    private String getName() {
        return getEventValue(e -> e.getCaseFileItemName(), path.toString());
    }

    private <T> T getEventValue(EventValuePicker<T> picker, T defaultValue) {
        if (events.getEvents().isEmpty()) return defaultValue;
        return picker.pick(events.getEvents().get(0));
    }

    interface EventValuePicker<T> {
        T pick(CaseFileEvent e);
    }
}
