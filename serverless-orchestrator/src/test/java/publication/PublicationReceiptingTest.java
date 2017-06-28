package publication;

import com.amazonaws.services.stepfunctions.builder.StateMachine;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * PublicationReceipting Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>jun 23, 2017</pre>
 */
public class PublicationReceiptingTest {
    PublicationReceipting publicationReceipting;
    private static final Logger log = LoggerFactory.getLogger(PublicationReceiptingTest.class);
    String sfInput;
    String bundleId;
    String arn;

    @Before
    public void before() throws Exception {
        publicationReceipting = new PublicationReceipting();
        sfInput = "";
        bundleId = "";
        arn = "";
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: startPublicationReceiptingWorkflow()
     */
    @Test
    public void bundle1ShouldGoToSIMS() throws Exception {
        Given:  bundleId="bundle000001";
                sfInput = "{ \"bundleId\": \"" + bundleId + "\"}";
        When:   arn = publicationReceipting.startPublicationReceiptingWorkflow(sfInput);
        Then:   Assert.assertNotNull(arn);
                waitForStepFuntionCompletion(arn);
                Assert.assertEquals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn), "SUCCEEDED");
                Assert.assertTrue(publicationReceipting.returnPublicationReceiptingWorkflowOutput(arn).contains("SIMS"));
    }

    @Test
    public void bundle2ShouldHaveStatusInactive() throws Exception {
        Given:  bundleId="bundle000002";
                sfInput = "{ \"bundleId\": \"" + bundleId + "\"}";
        When:   arn = publicationReceipting.startPublicationReceiptingWorkflow(sfInput);
        Then:   Assert.assertNotNull(arn);
                waitForStepFuntionCompletion(arn);
                Assert.assertEquals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn), "SUCCEEDED");
                Assert.assertTrue(publicationReceipting.returnPublicationReceiptingWorkflowOutput(arn).contains("INACTIVE"));
    }

    @Test
    public void bundle3ShouldHaveBusinessErrors() throws Exception {
        Given:  bundleId="bundle000003";
                sfInput = "{ \"bundleId\": \"" + bundleId + "\"}";
        When:   arn = publicationReceipting.startPublicationReceiptingWorkflow(sfInput);
        Then:   Assert.assertNotNull(arn);
                waitForStepFuntionCompletion(arn);
                Assert.assertEquals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn), "SUCCEEDED");
                Assert.assertTrue(publicationReceipting.returnPublicationReceiptingWorkflowOutput(arn).contains("\"exceptions\":true"));
    }

    @Test
    public void bundle4ShouldHaveNoScholarlyItems() throws Exception {
        Given:  bundleId="bundle000004";
                sfInput = "{ \"bundleId\": \"" + bundleId + "\"}";
        When:   arn = publicationReceipting.startPublicationReceiptingWorkflow(sfInput);
        Then:   Assert.assertNotNull(arn);
                waitForStepFuntionCompletion(arn);
                Assert.assertEquals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn), "SUCCEEDED");
                Assert.assertTrue(publicationReceipting.returnPublicationReceiptingWorkflowOutput(arn).contains("\"scholarlyItems\":0"));
    }

    @Test
    public void bundle5ShouldHaveNoPublicationArtifacts() throws Exception {
        Given:  bundleId="bundle000005";
                sfInput = "{ \"bundleId\": \"" + bundleId + "\"}";
        When:   arn = publicationReceipting.startPublicationReceiptingWorkflow(sfInput);
        Then:   Assert.assertNotNull(arn);
                waitForStepFuntionCompletion(arn);
                Assert.assertEquals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn), "SUCCEEDED");
                Assert.assertTrue(publicationReceipting.returnPublicationReceiptingWorkflowOutput(arn).contains("\"publicationArtifacts\":0"));
    }

    private void waitForStepFuntionCompletion(String arn) {
        while ("RUNNING".equals(publicationReceipting.returnPublicationReceiptingWorkflowStatus(arn)));
    }


    /**
     * Method: buildPublicationReceiptingWorkflow()
     */
    @Test
    public void buildPublicationReceiptingWorkflowShouldReturnStateMachine() throws Exception {
        StateMachine stateMachine = publicationReceipting.buildPublicationReceiptingWorkflow();
        Assert.assertNotNull(stateMachine);
        Assert.assertNotNull(stateMachine.getComment());
    }

    /**
     * Method: listSF(String sfInput)
     */
    @Test @Ignore
    public void listStepFunctionsShouldReturnANonEmptyList() throws Exception {
        List<StateMachineListItem> stateMachineListItems = publicationReceipting.listSF();
        Assert.assertNotNull(stateMachineListItems);
        Assert.assertTrue(stateMachineListItems.size()>0);
    }

    /**
     * Method: createSF()
     */
    @Test @Ignore
    public void createStepFunctionShouldReturnItsARN() throws Exception {
        Assert.assertTrue(publicationReceipting.createSF().contains("arn:aws:states"));
    }
} 
