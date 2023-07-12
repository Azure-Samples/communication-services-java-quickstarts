package com.communication.jobrouter.quickstart;

import com.azure.communication.jobrouter.models.AcceptJobOfferResult;
import com.azure.communication.jobrouter.models.ChannelConfiguration;
import com.azure.communication.jobrouter.models.DistributionPolicy;
import com.azure.communication.jobrouter.models.LabelOperator;
import com.azure.communication.jobrouter.models.LabelValue;
import com.azure.communication.jobrouter.models.LongestIdleMode;
import com.azure.communication.jobrouter.models.QueueAssignment;
import com.azure.communication.jobrouter.models.RouterJob;
import com.azure.communication.jobrouter.models.RouterWorker;
import com.azure.communication.jobrouter.models.options.CloseJobOptions;
import com.azure.communication.jobrouter.models.options.CreateDistributionPolicyOptions;
import com.azure.communication.jobrouter.models.options.CreateJobOptions;
import com.azure.communication.jobrouter.models.options.CreateQueueOptions;
import com.azure.communication.jobrouter.models.options.CreateWorkerOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        System.out.println("Azure Communication Services - Job Router Quickstart");

        // Get a connection string to our Azure Communication Services resource.
        String connectionString = "your_connection_string";
        JobRouterAdministrationClient routerAdminClient = new JobRouterAdministrationClientBuilder().connectionString(connectionString).buildClient();
        JobRouterClient routerClient = new JobRouterClientBuilder().connectionString(connectionString).buildClient();

        DistributionPolicy distributionPolicy = routerAdminClient.createDistributionPolicy(
            new CreateDistributionPolicyOptions("distribution-policy-1", Duration.ofMinutes(1), new LongestIdleMode())
                .setName("My distribution policy"));

        RouterQueue queue = routerAdminClient.createQueue(
            new CreateQueueOptions("queue-1",distributionPolicy.getId())
                .setName("My queue")
        );

        RouterJob job = routerClient.createJob(
            new CreateJobOptions("job-1", "voice", queue.getId())
                .setPriority(1)
                .setRequestedWorkerSelectors(List.of(
                        new RouterWorkerSelector("Some-Skill", LabelOperator.GREATER_THAN, new LabelValue(10)))));

        RouterWorker worker = routerClient.createWorker(
            new CreateWorkerOptions("worker-1", 1)
                .setQueueIds(new HashMap<String, QueueAssignment>() {{
                    put(queue.getId(), new RouterQueueAssignment());
                }})
                .setLabels(new HashMap<String, LabelValue>() {{
                    put("Some-Skill", new LabelValue(11));
                }})
                .setChannelConfigurations(new HashMap<String, ChannelConfiguration>() {{
                    put("voice", new ChannelConfiguration(1));
                }}));

        Thread.sleep(3000);
        worker = routerClient.getWorker(worker.getId());
        for (RouterJobOffer offer : worker.getOffers()) {
            System.out.printf("Worker %s has an active offer for job %s", worker.getId(), offer.getJobId());
        }

        AcceptJobOfferResult accept = routerClient.acceptJobOffer(worker.getId(), worker.getOffers().get(0).getOfferId());
        System.out.printf("Worker %s is assigned job %s", worker.getId(), accept.getJobId());

        routerClient.completeJob(new CompleteJobOptions("job-1", accept.getAssignmentId()));
        System.out.printf("Worker %s has completed job %s", worker.getId(), accept.getJobId());

        routerClient.closeJob(new CloseJobOptions("job-1", accept.getAssignmentId())
                .setDispositionCode("Resolved"));
        System.out.printf("Worker %s has closed job %s", worker.getId(), accept.getJobId());
    }
}
