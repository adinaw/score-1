package org.score.samples.openstack;

import com.hp.score.api.TriggeringProperties;
import org.score.samples.openstack.actions.ExecutionPlanBuilder;
import org.score.samples.openstack.actions.InputBinding;
import org.score.samples.openstack.actions.MatchType;
import org.score.samples.openstack.actions.NavigationMatcher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.score.samples.openstack.OpenstackCommons.*;
import static org.score.samples.openstack.actions.InputBinding.*;
import static org.score.samples.openstack.actions.FinalStepActions.RESPONSE_KEY;
import static org.score.samples.openstack.actions.FinalStepActions.SUCCESS_KEY;
import static org.score.samples.openstack.actions.StringOccurrenceCounter.RETURN_RESULT;

/**
 * Date: 8/29/2014
 *
 * @author Bonczidai Levente
 */
public class ValidateServerExistsFlow {
	private List<InputBinding> inputBindings;

	public ValidateServerExistsFlow() {
		inputBindings = generateInitialInputBindings();
	}

	public TriggeringProperties validateServerExistsFlow() {
		ExecutionPlanBuilder builder = new ExecutionPlanBuilder("validate");
		Long splitId = 0L;
		Long joinId = 1L;
		Long successId = 2L;
		Long failureId = 3L;
		Long prepareStringOccurrencesId = 4L;
		Long stringOccurencesId = 5L;
		Long resultFormatterId = 6L;

		createGetServersSubflow(builder, splitId, joinId, failureId, prepareStringOccurrencesId);

		createPrepareStringOccurencesStep(builder, prepareStringOccurrencesId, stringOccurencesId);

		createStringOccurencesStep(builder, stringOccurencesId, successId, resultFormatterId);

		createSuccessStep(builder, successId);

		createResultFormatterStepForFailure(builder, failureId, resultFormatterId);

		createFailureStep(builder, failureId);

		Map<String, Serializable> context = new HashMap<>();
		context.put(FLOW_DESCRIPTION, "Validate servers");
		builder.setInitialExecutionContext(context);

		builder.setBeginStep(0L);

		return builder.createTriggeringProperties();
	}

	public List<InputBinding> getInputBindings() {
		return inputBindings;
	}

	private void createPrepareStringOccurencesStep(ExecutionPlanBuilder builder, Long prepareStringOccurrencesId, Long stringOccurencesId) {
		//prepare string occurrences
		List<NavigationMatcher<Serializable>> navigationMatchers;
		navigationMatchers = new ArrayList<>();
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.DEFAULT, stringOccurencesId));
		builder.addOOActionStep(prepareStringOccurrencesId, CONTEXT_MERGER_CLASS, "prepareStringOccurrences", null, navigationMatchers);
	}

	private void createResultFormatterStepForFailure(ExecutionPlanBuilder builder, Long failureId, Long resultFormatterId) {
		//result formatter step
		builder.addStep(resultFormatterId, CONTEXT_MERGER_CLASS, "validateServerResult", failureId);
	}

	private void createStringOccurencesStep(ExecutionPlanBuilder builder, Long stringOccurencesId, Long successId, Long resultFormatterId) {
		List<NavigationMatcher<Serializable>> navigationMatchers;//string occurrence
		navigationMatchers = new ArrayList<>();
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.COMPARE_GREATER, RETURN_RESULT, "0", successId));
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.DEFAULT, resultFormatterId));
		builder.addOOActionStep(stringOccurencesId,
				"org.score.samples.openstack.actions.StringOccurrenceCounter",
				"execute",
				null,
				navigationMatchers);
	}

	private void createGetServersSubflow(ExecutionPlanBuilder builder, Long splitId, Long joinId, Long failureId, Long prepareStringOccurrencesId) {
		//get servers
		List<NavigationMatcher<Serializable>> navigationMatchers = new ArrayList<>();
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.EQUAL, RESPONSE_KEY, SUCCESS_KEY, prepareStringOccurrencesId));
		navigationMatchers.add(new NavigationMatcher<Serializable>(MatchType.DEFAULT, failureId));
		ListServersFlow listServersFlow = new ListServersFlow();
		TriggeringProperties triggeringProperties = listServersFlow.listServersFlow();
		List<String> inputKeys = new ArrayList<>();
		for (InputBinding inputBinding : listServersFlow.getInputBindings()) {
			inputKeys.add(inputBinding.getInputKey());
		}

		builder.addSubflow(splitId, joinId, triggeringProperties, inputKeys, navigationMatchers);
	}

	private List<InputBinding> generateInitialInputBindings() {
		List<InputBinding> bindings = new ArrayList<>();

		bindings.addAll(new ListServersFlow().getInputBindings());
		bindings.add(createInputBinding(SERVER_NAME_MESSAGE, SERVER_NAME_KEY, true));

		return bindings;
	}
}