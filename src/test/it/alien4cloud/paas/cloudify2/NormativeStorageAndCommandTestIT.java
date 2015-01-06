package alien4cloud.paas.cloudify2;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;

import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-testit.xml")
@Slf4j
public class NormativeStorageAndCommandTestIT extends GenericStorageTestCase {

    @Resource(name = "cloudify-paas-provider-bean")
    protected CloudifyPaaSProvider anotherCloudifyPaaSPovider;

    public NormativeStorageAndCommandTestIT() {
    }

    @Test
    public void customCommandTest() throws Exception {
        log.info("\n\n >> Executing Test customCommandTest \n");
        String cloudifyAppId = null;
        this.initElasticSearch(new String[] { "tomcat-test-types" }, new String[] { "1.0-SNAPSHOT" });
        try {
            String[] computesId = new String[] { "comp_custom_cmd" };
            cloudifyAppId = deployTopology("customCmd", computesId);

            this.assertApplicationIsInstalled(cloudifyAppId);
            waitForServiceToStarts(cloudifyAppId, "comp_custom_cmd", 1000L * 120);

            String resultSnipet = "hello <alien>, os_version is <ubuntu>, from <comp_custom_cmd";
            String resultSnipetInst = "hello <alien>, os_version is <ubuntu>, from <comp_custom_cmd.1>";
            Map<String, String> params = Maps.newHashMap();
            params.put("yourName", "alien");
            testCustomCommandSuccess(cloudifyAppId, "tomcat", null, "helloCmd", params, resultSnipet);
            params.put("yourName", null);
            testCustomCommandFail(cloudifyAppId, "tomcat", null, "helloCmd", null);
            params.put("yourName", "alien");
            testCustomCommandSuccess(cloudifyAppId, "tomcat", 1, "helloCmd", params, resultSnipetInst);
            params.put("yourName", "failThis");
            testCustomCommandFail(cloudifyAppId, "tomcat", 1, "helloCmd", params);

        } catch (Exception e) {
            log.error("Test Failed", e);
            throw e;
        }
    }

    @Test
    public void blockStorageVolumeIdProvidedSucessTest() throws Throwable {
        log.info("\n\n >> Executing Test blockStorageVolumeIdProvidedSucessTest \n");
        String cloudifyAppId = null;
        try {
            String[] computesId = new String[] { "comp_storage_volumeid" };
            cloudifyAppId = deployTopology("blockStorageWithVolumeId", computesId);

            this.assertApplicationIsInstalled(cloudifyAppId);
            waitForServiceToStarts(cloudifyAppId, "comp_storage_volumeid", 1000L * 120);
            assertStorageEventFiredWithVolumeId(cloudifyAppId, new String[] { "blockstorage" }, ToscaNodeLifecycleConstants.CREATED);

        } catch (Exception e) {
            log.error("Test Failed", e);
            throw e;
        }
    }

    @Test
    // @Ignore
    public void blockStorageSizeProvidedSucessTest() throws Throwable {
        log.info("\n\n >> Executing Test blockStorageSizeProvidedSucessTest \n");
        String cloudifyAppId = null;
        this.initElasticSearch(new String[] { "deletable-storage-type" }, new String[] { "1.0" });
        try {

            String[] computesId = new String[] { "comp_storage_size" };
            cloudifyAppId = deployTopology("deletableBlockStorageWithSize", computesId);

            this.assertApplicationIsInstalled(cloudifyAppId);
            waitForServiceToStarts(cloudifyAppId, "comp_storage_size", 1000L * 120);
            assertStorageEventFiredWithVolumeId(cloudifyAppId, new String[] { "blockstorage" }, ToscaNodeLifecycleConstants.CREATED);

        } catch (Exception e) {
            log.error("Test Failed", e);
            throw e;
        }
    }

    private void testCustomCommandFail(String applicationId, String nodeName, Integer instanceId, String command, Map<String, String> params) {
        try {
            executeCustomCommand(applicationId, nodeName, instanceId, command, params, new IPaaSCallback<Map<String, String>>() {
                @Override
                public void onSuccess(Map<String, String> data) {
                    Assert.fail();
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
        } catch (OperationExecutionException e) {
        }
    }

    private void testCustomCommandSuccess(String cloudifyAppId, String nodeName, Integer instanceId, String command, Map<String, String> params,
            final String expectedResultSnippet) {
        executeCustomCommand(cloudifyAppId, nodeName, instanceId, command, params, new IPaaSCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> result) {

                if (expectedResultSnippet != null) {
                    for (String opReslt : result.values()) {
                        Assert.assertTrue("Command result is <" + opReslt.toLowerCase() + ">. It should have contain <" + expectedResultSnippet + ">", opReslt
                                .toLowerCase().contains(expectedResultSnippet.toLowerCase()));
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    private void executeCustomCommand(String cloudifyAppId, String nodeName, Integer instanceId, String command, Map<String, String> params,
            IPaaSCallback<Map<String, String>> callback) {
        if (!deployedCloudifyAppIds.contains(cloudifyAppId)) {
            Assert.fail("Topology not found in deployments");
        }
        NodeOperationExecRequest request = new NodeOperationExecRequest();
        request.setInterfaceName("custom");
        request.setOperationName(command);
        request.setNodeTemplateName(nodeName);

        if (instanceId != null) {
            request.setInstanceId(instanceId.toString());
        }
        request.setParameters(params);
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext();
        deploymentContext.setDeploymentId(cloudifyAppId);
        cloudifyPaaSPovider.executeOperation(deploymentContext, request, callback);
    }
}
