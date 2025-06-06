// ================================
// File: AssemblyGenerator.java
// ================================
package assembly;

import machinery.State;
import machinery.StateInterface;
import machinery.StateMachine;
import machinery.TransitionInterface;
import pws.editor.semantics.Configuration;
import pws.editor.semantics.Semantics;
import smalgebra.BasicStateProposition;
import smalgebra.SMProposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that generates all possible Assembly instances
 * by assigning to each state machine in the assembly one of its possible states
 * as its current state. Each generated assembly instance will have a unique
 * configuration of current states across all its state machines.
 */
public class AssemblyGenerator {

    /**
     * Generates all possible Assembly instances given the provided Assembly template.
     * Each state machine in the template may have any of its states selected as the current state.
     *
     * @param template the original Assembly template
     * @return a List of Assembly instances, each with a unique combination of current states
     */
    public static List<Assembly> generateAllAssemblies(Assembly template) {
        // Get the list of machine identifiers in the template.
        List<String> machineIds = new ArrayList<>(template.getStateMachines().keySet());
        List<Assembly> result = new ArrayList<>();

        // Start recursive generation.
        generateAssembliesRecursive(template, machineIds, 0, new HashMap<>(), result);
        return result;
    }

    /**
     * Recursive helper method that builds a mapping from machine identifier to a chosen state,
     * then creates a new Assembly with state machines cloned accordingly.
     *
     * @param template   the original Assembly template.
     * @param machineIds list of machine identifiers.
     * @param index      the current index in machineIds that we are assigning.
     * @param assignment a mapping from machine id to a chosen State.
     * @param result     the list of generated Assembly instances.
     */
    private static void generateAssembliesRecursive(Assembly template, List<String> machineIds, int index,
                                                    Map<String, State> assignment, List<Assembly> result) {
        if (index == machineIds.size()) {
            // All machines have been assigned a current state.
            Assembly cloned = cloneAssemblyWithAssignment(template, assignment);
            result.add(cloned);
            return;
        }

        String machineId = machineIds.get(index);
        StateMachine machine = template.getStateMachines().get(machineId);
        // For each possible state in the state machine, assign it and recurse.
        for (StateInterface state : machine.getStates()) {
            assignment.put(machineId, (State) state);
            generateAssembliesRecursive(template, machineIds, index + 1, assignment, result);
            // Remove the assignment before the next iteration.
            assignment.remove(machineId);
        }
    }

    /**
     * Clones the given Assembly and for each state machine in it sets the current state
     * as indicated by the assignment map.
     *
     * @param template   the original Assembly.
     * @param assignment a mapping from machine id to a chosen State.
     * @return a new Assembly instance with cloned state machines and current state set accordingly.
     */
    private static Assembly cloneAssemblyWithAssignment(Assembly template, Map<String, State> assignment) {
        Assembly newAssembly = new Assembly(template.getAssemblyId());

        // Iterate over each machine in the template.
        for (Map.Entry<String, StateMachine> entry : template.getStateMachines().entrySet()) {
            String machineId = entry.getKey();
            StateMachine origMachine = entry.getValue();
            // Clone the machine, setting its current state from the assignment.
            StateMachine clonedMachine = cloneStateMachine(origMachine, assignment.get(machineId));
            newAssembly.addStateMachine(machineId, clonedMachine);
        }
        return newAssembly;
    }

    /**
     * Creates a clone of the given state machine. The cloned state machine will have
     * the same states and transitions, but its current state is set to the provided value.
     *
     * This method assumes that states (and transitions) are immutable or shared safely.
     *
     * @param origMachine the original state machine.
     * @param current     the state to set as current in the cloned machine.
     * @return a new StateMachine instance with the same structure as origMachine and with current state set.
     */
    private static StateMachine cloneStateMachine(StateMachine origMachine, State current) {
        StateMachine clone = new StateMachine(origMachine.getName());

        // Add all states.
        for (StateInterface s : origMachine.getStates()) {
            // Assuming StateImpl is immutable, we can add the same instance.
            clone.addState(s);
        }
        // Add all transitions.
        for (TransitionInterface t : origMachine.getTransitions()) {
            // We assume that transitions refer to the same states; if necessary, you could clone them as well.
            clone.addTransition(t);
        }
        // Set the current state.
        clone.setCurrentState(current);

        return clone;
    }

    /**
     * Evaluates the given SMProposition formula over all feasible assemblies generated
     * Only assemblies for which the formula evaluates to true are converted to configurations and added to the final result.
     *
     * @param template the Assembly template used to generate all feasible assemblies.
     * @param formula the SMProposition formula to evaluate.
     * @return a Semantics object containing the configurations corresponding to the assemblies that satisfy the formula.
     */
    public static Semantics evaluateSMPropositionOverAllFeasibleAssemblies(Assembly template, SMProposition formula) {
        List<Assembly> assemblies = generateAllAssemblies(template);
        Semantics result = new Semantics(template.getAssemblyId());
        for (Assembly assembly : assemblies) {
            if (formula.evaluate(assembly)) {
                // Convert assembly to configuration.
                Configuration config = extractConfiguration(assembly);
                result.addConfiguration(config);
            }
        }
        return result;
    }

    /**
     * Converts an Assembly into a Configuration.
     * For each state machine in the assembly, the current state's name is used to create a BasicStateProposition.
     *
     * @param assembly the Assembly to convert.
     * @return a Configuration representing the current states of the assembly.
     */
    public static Configuration extractConfiguration(Assembly assembly) {
        List<BasicStateProposition> propositions = new ArrayList<>();
        for (Map.Entry<String, StateMachine> entry : assembly.getStateMachines().entrySet()) {
            String machineId = entry.getKey();
            StateMachine machine = entry.getValue();
            StateInterface currentState = machine.getCurrentState();
            if (currentState != null) {
                propositions.add(new BasicStateProposition(machineId, currentState.getName()));
            }
        }
        return Configuration.fromBasicStatePropositions(assembly.getAssemblyId(), propositions);
    }
}