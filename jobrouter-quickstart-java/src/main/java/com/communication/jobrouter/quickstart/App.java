package com.communication.jobrouter.quickstart;

import com.azure.communication.jobrouter.JobRouterAdministrationClient;
import com.azure.communication.jobrouter.JobRouterAdministrationClientBuilder;
import com.azure.communication.jobrouter.JobRouterClient;
import com.azure.communication.jobrouter.JobRouterClientBuilder;
import com.azure.communication.jobrouter.models.AcceptJobOfferResult;
import com.azure.communication.jobrouter.models.ChannelConfiguration;
import com.azure.communication.jobrouter.models.CloseJobOptions;
import com.azure.communication.jobrouter.models.CompleteJobOptions;
import com.azure.communication.jobrouter.models.CreateDistributionPolicyOptions;
import com.azure.communication.jobrouter.models.CreateJobOptions;
import com.azure.communication.jobrouter.models.CreateQueueOptions;
import com.azure.communication.jobrouter.models.CreateWorkerOptions;
import com.azure.communication.jobrouter.models.DistributionPolicy;
import com.azure.communication.jobrouter.models.LabelOperator;
import com.azure.communication.jobrouter.models.LabelValue;
import com.azure.communication.jobrouter.models.LongestIdleMode;
import com.azure.communication.jobrouter.models.RouterJob;
import com.azure.communication.jobrouter.models.RouterJobMatchingMode;
import com.azure.communication.jobrouter.models.RouterJobOffer;
import com.azure.communication.jobrouter.models.RouterQueue;
import com.azure.communication.jobrouter.models.RouterQueueAssignment;
import com.azure.communication.jobrouter.models.RouterWorker;
import com.azure.communication.jobrouter.models.RouterWorkerSelector;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Azure Communication Services - Job Router Quickstart
 */
public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        System.out.println("Azure Communication Services - Job Router Quickstart");

        // Get a connection string to our Azure Communication Services resource.
        String connectionString = "your_connection_string";
        JobRouterAdministrationClient routerAdminClient = new JobRouterAdministrationClientBuilder()
                .connectionString(connectionString).buildClient();
        JobRouterClient routerClient = new JobRouterClientBuilder().connectionString(connectionString).buildClient();

        DistributionPolicy distributionPolicy = routerAdminClient.createDistributionPolicy(
            new CreateDistributionPolicyOptions("distribution-policy-1", Duration.ofMinutes(1), new LongestIdleMode())
                .setName("My distribution policy"));

        RouterQueue queue = routerAdminClient.createQueue(
            new CreateQueueOptions("queue-1",distributionPolicy.getId()).setName("My queue"));

        RouterJob job = routerClient.createJob(new CreateJobOptions("job-1", "voice", queue.getId())
            .setPriority(1)
            .setRequestedWorkerSelectors(List.of(new RouterWorkerSelector()
                .setKey("Some-Skill")
                .setLabelOperator(LabelOperator.GREATER_THAN)
                .setValue(new LabelValue(10)))));

        RouterWorker worker = routerClient.createWorker(
            new CreateWorkerOptions("worker-1", 1)
                .setQueueAssignments(Map.of(queue.getId(), new RouterQueueAssignment()))
                .setLabels(Map.of("Some-Skill", new LabelValue(11)))
                .setChannelConfigurations(Map.of("voice", new ChannelConfiguration().setCapacityCostPerJob(1)))
                .setAvailableForOffers(true));

        Thread.sleep(5000);
        worker = routerClient.getWorker(worker.getId());
        for (RouterJobOffer offer : worker.getOffers()) {
            System.out.printf("Worker %s has an active offer for job %s\n", worker.getId(), offer.getJobId());
        }

        AcceptJobOfferResult accept = routerClient.acceptJobOffer(worker.getId(), worker.getOffers().get(0).getOfferId());
        System.out.printf("Worker %s is assigned job %s\n", worker.getId(), accept.getJobId());

        routerClient.completeJob(new CompleteJobOptions(accept.getJobId(), accept.getAssignmentId()));
        System.out.printf("Worker %s has completed job %s\n", worker.getId(), accept.getJobId());

        routerClient.closeJob(new CloseJobOptions(accept.getJobId(), accept.getAssignmentId()).setDispositionCode("Resolved"));
        System.out.printf("Worker %s has closed job %s\n", worker.getId(), accept.getJobId());

        routerClient.deleteJob(accept.getJobId());
        System.out.printf("Deleted job %s\n", accept.getJobId());
    }
}
