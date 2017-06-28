package publication;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.builder.ErrorCodes;
import com.amazonaws.services.stepfunctions.builder.StateMachine;
import com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder;
import com.amazonaws.services.stepfunctions.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PublicationReceipting {
    private static final Logger log = LoggerFactory.getLogger(PublicationReceipting.class);

    public PublicationReceipting() {
    }

    protected StateMachine buildPublicationReceiptingWorkflow() {
        StateMachine stateMachine = StepFunctionBuilder.stateMachine()
                .comment("PublicationReceipting")
                .startAt("CallPIPS")
                .state("CallPIPS", StepFunctionBuilder.taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-PIPS")
                        .catcher(StepFunctionBuilder.catcher()
                                .errorEquals(ErrorCodes.TASK_FAILED)
                                .transition(StepFunctionBuilder.next("SystemError")))
                        .retrier(StepFunctionBuilder.retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(StepFunctionBuilder.next("CheckPIPSResponse")))
                .state("CheckPIPSResponse", StepFunctionBuilder.choiceState()
                        .choice(StepFunctionBuilder.choice()
                                .transition(StepFunctionBuilder.next("Inactive"))
                                .condition(StepFunctionBuilder.eq("$.status", "INACTIVE")))
                        .choice(StepFunctionBuilder.choice()
                                .transition(StepFunctionBuilder.next("BusinessErrors"))
                                .condition(StepFunctionBuilder.eq("$.exceptions", true)))
                        .defaultStateName("CheckScholarlyItems")
                )
                .state("BusinessErrors", StepFunctionBuilder.passState()
                        .transition(StepFunctionBuilder.next("Notify")))
                .state("SystemError", StepFunctionBuilder.passState()
                        .transition(StepFunctionBuilder.end()))
                .state("Inactive", StepFunctionBuilder.passState()
                        .transition(StepFunctionBuilder.end()))
                .state("Notify", StepFunctionBuilder.taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-Notify")
                        .transition(StepFunctionBuilder.end()))
                .state("CheckScholarlyItems", StepFunctionBuilder.choiceState()
                        .choice(StepFunctionBuilder.choice()
                                .transition(StepFunctionBuilder.next("CheckPublicationArtifacts"))
                                .condition(StepFunctionBuilder.gt("$.scholarlyItems", 0)))
                        .defaultStateName("Notify"))
                .state("CheckPublicationArtifacts", StepFunctionBuilder.choiceState()
                        .choice(StepFunctionBuilder.choice()
                                .transition(StepFunctionBuilder.next("CallContentService"))
                                .condition(StepFunctionBuilder.gt("$.publicationArtifacts", 0)))
                        .defaultStateName("CallSIMS"))
                .state("CallContentService", StepFunctionBuilder.taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication_ContentService")
                        .retrier(StepFunctionBuilder.retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(StepFunctionBuilder.next("CallSIMS")))
                .state("CallSIMS", StepFunctionBuilder.taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-SIMS")
                        .retrier(StepFunctionBuilder.retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(StepFunctionBuilder.end()))
                .build();
        try {
            Files.write(Paths.get("src/main/resources/PublicationReceiptingGenerated.json"), stateMachine.toPrettyJson().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stateMachine;
    }

    public String createSF() {
        AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();
        CreateStateMachineResult createStateMachineResult = client.createStateMachine(new CreateStateMachineRequest()
                .withName("PublicationReceipting")
                .withRoleArn("arn:aws:iam::509786517216:role/service-role/StatesExecutionRole-us-west-2")
                .withDefinition(buildPublicationReceiptingWorkflow()));
        return createStateMachineResult.getStateMachineArn();
    }

    public String startPublicationReceiptingWorkflow(String sfInput) {
        final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();
        StartExecutionResult startExecutionResult = client.startExecution(new StartExecutionRequest()
                .withStateMachineArn("arn:aws:states:us-west-2:509786517216:stateMachine:PublicationReceipting.0.5")
                .withInput(sfInput));
        log.info("Request processed with id: " + startExecutionResult.getSdkResponseMetadata().getRequestId());
        return startExecutionResult.getExecutionArn();
    }

    public String returnPublicationReceiptingWorkflowStatus(String sfARN) {
        final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();
        DescribeExecutionResult describeExecutionResult = client.describeExecution(new DescribeExecutionRequest().withExecutionArn(sfARN));
        log.info("Workflow status is : " + describeExecutionResult.getStatus());
        return describeExecutionResult.getStatus();
    }

    public String returnPublicationReceiptingWorkflowOutput(String sfARN) {
        final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();
        DescribeExecutionResult describeExecutionResult = client.describeExecution(new DescribeExecutionRequest().withExecutionArn(sfARN));
        log.info("Workflow output is : " + describeExecutionResult.getOutput());
        return describeExecutionResult.getOutput();
    }

    public List<StateMachineListItem> listSF() {
        final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();
        ListStateMachinesResult sfs = client.listStateMachines(new ListStateMachinesRequest());
        sfs.getStateMachines().stream()
            .forEach(sm -> System.out.println(sm.getStateMachineArn()));
        return sfs.getStateMachines();
    }

}
