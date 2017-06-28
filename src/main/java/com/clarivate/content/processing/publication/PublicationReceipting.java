package com.clarivate.content.processing.publication;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.builder.ErrorCodes;
import com.amazonaws.services.stepfunctions.builder.StateMachine;
import com.amazonaws.services.stepfunctions.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.*;

public class PublicationReceipting {
    private static final Logger log = LoggerFactory.getLogger(PublicationReceipting.class);

    public PublicationReceipting() {
    }

    protected StateMachine buildPublicationReceiptingWorkflow() {
        StateMachine stateMachine = stateMachine()
                .comment("PublicationReceipting")
                .startAt("CallPIPS")
                .state("CallPIPS", taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-PIPS")
                        .catcher(catcher()
                                .errorEquals(ErrorCodes.TASK_FAILED)
                                .transition(next("SystemError")))
                        .retrier(retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(next("CheckPIPSResponse")))
                .state("CheckPIPSResponse", choiceState()
                        .choice(choice()
                                .transition(next("Inactive"))
                                .condition(eq("$.status", "INACTIVE")))
                        .choice(choice()
                                .transition(next("BusinessErrors"))
                                .condition(eq("$.exceptions", true)))
                        .defaultStateName("CheckScholarlyItems")
                )
                .state("BusinessErrors", passState()
                        .transition(next("Notify")))
                .state("SystemError", passState()
                        .transition(end()))
                .state("Inactive", passState()
                        .transition(end()))
                .state("Notify", taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-Notify")
                        .transition(end()))
                .state("CheckScholarlyItems", choiceState()
                        .choice(choice()
                                .transition(next("CheckPublicationArtifacts"))
                                .condition(gt("$.scholarlyItems", 0)))
                        .defaultStateName("Notify"))
                .state("CheckPublicationArtifacts", choiceState()
                        .choice(choice()
                                .transition(next("CallContentService"))
                                .condition(gt("$.publicationArtifacts", 0)))
                        .defaultStateName("CallSIMS"))
                .state("CallContentService", taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication_ContentService")
                        .retrier(retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(next("CallSIMS")))
                .state("CallSIMS", taskState()
                        .resource("arn:aws:lambda:us-west-2:509786517216:function:publication-SIMS")
                        .retrier(retrier()
                                .errorEquals(ErrorCodes.TIMEOUT)
                                .intervalSeconds(5)
                                .maxAttempts(5)
                                .backoffRate(2.0)
                        )
                        .transition(end()))
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
