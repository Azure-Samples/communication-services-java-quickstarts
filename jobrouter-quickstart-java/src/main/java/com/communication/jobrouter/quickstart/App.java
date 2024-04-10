package com.communication.jobrouter.quickstart;

import com.azure.communication.jobrouter.JobRouterAdministrationClient;
import com.azure.communication.jobrouter.JobRouterAdministrationClientBuilder;
import com.azure.communication.jobrouter.JobRouterClient;
import com.azure.communication.jobrouter.JobRouterClientBuilder;
import com.azure.communication.jobrouter.models.*;

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
            .setRequestedWorkerSelectors(List.of(
                new RouterWorkerSelector("Some-Skill", LabelOperator.GREATER_THAN, new RouterValue(10)))));

        RouterWorker worker = routerClient.createWorker(
            new CreateWorkerOptions("worker-1", 1)
                .setQueues(List.of(queue.getId()))
                .setLabels(Map.of("Some-Skill", new RouterValue(11)))
                .setChannels(List.of(new RouterChannel("voice", 1)))
                .setAvailableForOffers(true));

        Thread.sleep(10000);
        worker = routerClient.getWorker(worker.getId());
        for (RouterJobOffer offer : worker.getOffers()) {
            System.out.printf("Worker %s has an active offer for job %s\n", worker.getId(), offer.getJobId());
        }

        AcceptJobOfferResult accept = routerClient.acceptJobOffer(worker.getId(), worker.getOffers().get(0).getOfferId());
        System.out.printf("Worker %s is assigned job %s\n", worker.getId(), accept.getJobId());

        routerClient.completeJobWithResponse(accept.getJobId(), accept.getAssignmentId(), null);
        System.out.printf("Worker %s has completed job %s\n", worker.getId(), accept.getJobId());

        routerClient.closeJobWithResponse(accept.getJobId(), accept.getAssignmentId(), null);
        System.out.printf("Worker %s has closed job %s\n", worker.getId(), accept.getJobId());

        routerClient.deleteJob(accept.getJobId());
        System.out.printf("Deleted job %s\n", accept.getJobId());
    }
}
