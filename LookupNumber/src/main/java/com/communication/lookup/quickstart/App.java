package com.communication.lookup.quickstart;

import com.azure.communication.phonenumbers.*;
import com.azure.communication.phonenumbers.models.*;
import com.azure.core.http.rest.*;
import com.azure.core.util.Context;
import com.azure.identity.*;
import java.io.*;
import java.util.ArrayList;

public class App
{
    public static void main( String[] args ) throws IOException
    {
        System.out.println("Azure Communication Services - Number Lookup Quickstart");

        // This code retrieves your connection string from an environment variable
        String connectionString = System.getenv("COMMUNICATION_SERVICES_CONNECTION_STRING");

        PhoneNumbersClient phoneNumberClient = new PhoneNumbersClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        ArrayList<String> phoneNumbers = new ArrayList<String>();
        phoneNumbers.add("+13154805277");

        // Use the free number lookup functionality to get number formatting information
        OperatorInformationResult formattingResult = phoneNumberClient.searchOperatorInformation(phoneNumbers);
        OperatorInformation formattingInfo = formattingResult.getValues().get(0);
        System.out.println(formattingInfo.getPhoneNumber() + " is formatted " 
            + formattingInfo.getInternationalFormat() +  " internationally, and "
            + formattingInfo.getNationalFormat() + " nationally");

        // Use the paid number lookup functionality to get operator specific details
        // IMPORTANT NOTE: Invoking the method below will incur a charge to your account
        OperatorInformationOptions options = new OperatorInformationOptions();
        options.setIncludeAdditionalOperatorDetails(true);
        Response<OperatorInformationResult> result = phoneNumberClient.searchOperatorInformationWithResponse(phoneNumbers, options, Context.NONE);
        OperatorInformation operatorInfo = result.getValue().getValues().get(0);

        String numberType = operatorInfo.getNumberType() == null ? "unknown" : operatorInfo.getNumberType().toString();
        String operatorName = "an unknown operator";
        if (operatorInfo.getOperatorDetails()!= null && operatorInfo.getOperatorDetails().getName() != null)
        {
            operatorName = operatorInfo.getOperatorDetails().getName();
        }
        System.out.println(operatorInfo.getPhoneNumber() + " is a " + numberType + " number, operated in "
            + operatorInfo.getIsoCountryCode() + " by " + operatorName);
    }
}