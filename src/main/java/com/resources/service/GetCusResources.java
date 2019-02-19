package main.java.com.resources.service;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class GetCusResources {
	
	private static final String API_MODE = "api_mode";
	private static final String MOCK_BODY = "/mockBody/test/";
	private static final String API_TEST_MODE_INDICATOR = "test_api";
	private static final String INTERNAL_SERVER_ERROR_RESPONSE = "500";
	private static final String BAD_REQUEST_CODE = "400";
	private static final String INFO_URL = "https://developers.api.com";
	private static final String DRUGS = "DRUGS";
	private static final String API_ID = "customer-service-customers";
	private static final String POLICY_ID = "policyId";
	private static final String BENEFIT_ID = "benefitId";
	private static Logger logger = LoggerFactory.getLogger(GetCusResources.class);
	private CusResourcesServiceHelper customersDrugCoveragesServiceHelper;
	private CusResourcesResponseHelper customersDrugCoveragesResponseHelper;
	private CamelContext context;
	ProducerTemplate producerTemplate;
	ActorHelper actorHelper = null;
	
	public DrugCoveragesResponse getCustomersDrugCoverages(Exchange exchange) throws Exception {
		String policyNumber = ApiTools.resolveGet(() -> exchange.getIn().getHeader("policyNumber", String.class));
		
		Actor actor  = (Actor) exchange.getIn().getHeader(ApiTools.DATA_SECURITY_HEADER_ACTOR);
		this.setActorHelper(new ActorHelper());
		this.actorHelper.setActor(actor);
		logger.debug("Value of getPolicyIdBenefitIdPairs : "+this.actorHelper.getPolicyIdBenefitIdPairs(DRUGS, policyNumber).toString());
		
	    String apiMode = ApiTools.resolveGet(() -> exchange.getIn().getHeader(API_MODE, String.class));
		boolean isMockResponseExpected = customersDrugCoveragesServiceHelper.isStubable(exchange); 
		DrugCoveragesResponse drugcoveragesResponse = createRequestAndSendToDrugCoveragesEndpoint(exchange, isMockResponseExpected, apiMode, policyNumber);
		return drugcoveragesResponse;
    }
	
	DrugCoveragesResponse createRequestAndSendToDrugCoveragesEndpoint(Exchange exchange, boolean isMockResponseExpected, String apiMode, String policyNumber) throws Exception {
		Exchange exchangeCopy = exchange.copy();
		this.producerTemplate = customersDrugCoveragesServiceHelper.lookupProducerTemplate(exchangeCopy);
		DrugCoveragesResponse drugcoveragesResponse = null;
		ArrayList<DrugCoveragesResponse> listOfResp = new ArrayList<DrugCoveragesResponse>();
			
		List<SimpleEntry<String, String>> benefitpairslist = this.actorHelper.getPolicyIdBenefitIdPairs(DRUGS, policyNumber);

		logger.debug("this.actorHelper.getPolicyIdBenefitIdPairs(DRUGS)"+ benefitpairslist.toString());
			
		if (benefitpairslist != null && !benefitpairslist.isEmpty()) {
			for(SimpleEntry<String, String> benefitpairs:benefitpairslist){
					String policyId = benefitpairs.getKey();
					String benefitId = benefitpairs.getValue();
					
					if (!isMockResponseExpected){
						exchangeCopy.setProperty(POLICY_ID, policyId);
						exchangeCopy.setProperty(BENEFIT_ID, benefitId);
						producerTemplate.send(GetResourcesApi.GET_DRUGCOVERAGES_REAL_ENDPOINT, exchangeCopy);
						
						if (!getCustomersDrugCoveragesResponseHelper().hasException(exchangeCopy)) {
							String actualJson = exchangeCopy.getIn().getBody(String.class);
							logger.debug("Json fetched from south bound call" + actualJson);
							drugcoveragesResponse = getCustomersDrugCoveragesResponseHelper().unmarshall(this.actorHelper.getActor(), actualJson);
							listOfResp.add(drugcoveragesResponse);
						}
					}
					else {
						logger.debug("Sending request to MOCK DrugCoverages Backend.");
						drugcoveragesResponse = getTestMockResponse(apiMode, policyId, benefitId);
						listOfResp.add(drugcoveragesResponse);
					}
			}
		}
		if (listOfResp.size() > 0 ){
			drugcoveragesResponse = this.aggregateResponses(listOfResp);
		}
		
		//if the response object has not been populated, we don't have the requested benefit available.
		//This could happen for various reasons: plan member does not have benefit available, error in the backend, connection issues, etc.
		//Throw 400 error.
		if (drugcoveragesResponse == null) {
			throw new ResourceCoveragesApiException("Requested benefit not found", BAD_REQUEST_CODE, "Requested benefit not found", ApiTools.buildJsonErrorsString(String.format("400.%s.2000", API_ID),
						"Requested benefit not found", "Drug Benefit Info is not available for the provided request", null, INFO_URL));
		}
		return drugcoveragesResponse;
	}
	
	DrugCoveragesResponse aggregateResponses(ArrayList<DrugCoveragesResponse> drugCoveragerespList){
		DrugCoveragesResponse dr = new DrugCoveragesResponse();
		DrugCoveragesData drugCoverageData = new DrugCoveragesData();
		
		for (DrugCoveragesResponse drugCovResp : drugCoveragerespList){
			// if there are multiple responses, first check if the aggregated object already has customer records list. If not, copy the list over. Otherwise, add to the existing list.
			if (drugCoverageData.getCustomerRecords() == null || drugCoverageData.getCustomerRecords().isEmpty()) { 
				drugCoverageData.setCustomerRecords(drugCovResp.getData().getCustomerRecords());
			} else {
				drugCoverageData.getCustomerRecords().addAll(drugCovResp.getData().getCustomerRecords());
			}
		}
		
		dr.setData(drugCoverageData);
		dr.setMeta(drugCoveragerespList.get(0).getMeta());
		
		ObjectMapper om = new ObjectMapper();
		try {
			logger.debug("aggregated responses" + om.writeValueAsString(dr));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return dr;
	}
	
	DrugCoveragesResponse getTestMockResponse(String apiMode, String policyId, String benefitId) throws Exception {
		
		String fileName = apiMode.replace(API_TEST_MODE_INDICATOR + "_", "");
		logger.debug("Looking for filename = " + fileName);
		
		if (fileName.equals(INTERNAL_SERVER_ERROR_RESPONSE)) {
			logger.debug("Manually throwing 500 Internal Server Error");
			throw new Exception();
		}
		
		URL url = getResourceURL(fileName);
		
		if(url != null) {
			String mockJson;
			try {
				mockJson = IOUtils.toString(url);
				logger.debug("Mock file = " + mockJson);
			}
			catch (IOException e) {
				logger.error("Unable to read from file for api_mode=test");
				throw new UnableToReadMockResponseException();
			}
	        return getCustomersDrugCoveragesResponseHelper().unmarshallTestMock(this.actorHelper.getActor(), mockJson, policyId, benefitId);
		}
		else {
			logger.error("Unable to set the Mock response from file for api_mode=test");
			throw new UnableToReadMockResponseException();
		}
	}
	
	
	public void loadEncryptedElementsList(Exchange exchange) {
	    try {
	        String elementsToEncrypt = exchange.getContext().resolvePropertyPlaceholders("{{ELEMENTS_TO_ENCRYPT}}");
	        if(!StringUtils.isBlank(elementsToEncrypt)){
	            exchange.getIn().setHeader("elementsToEncrypt", elementsToEncrypt);
	        } else {
	            logger.debug("No key paths set to encrypt on this resource");
	        }
	    } catch (Exception e) {
	        //results in 500 error if config values are not present or cannot be read
	        logger.error("Unable to read ELEMENTS_TO_ENCRYPT configurations in this environment.");
	        throw new IllegalStateException("Unable to read ELEMENTS_TO_ENCRYPT configurations in this environment.", e);
	    }
	} 

	
	public void convertResponseObject(Exchange exchange) throws Exception {     
	    try {
	        Object responseBody = exchange.getIn().getBody();
	        if (responseBody == null) {
	            throw new Exception("The JSON response body appears to be empty.");
	        }
	        ObjectMapper mapperObj = new ObjectMapper();
	        String jsonStr = mapperObj.writeValueAsString(responseBody);
	        exchange.getIn().setBody(mapperObj.readValue(jsonStr, DrugCoveragesResponse.class));
	         
	    }
	    catch(JsonMappingException jme) {
	        logger.error("Error converting the JSON payload. {}. {}", ApiTools.buildJsonMappingErrorFieldPath(jme), ApiTools.buildJsonMappingErrorClue(jme));
	        throw new Exception("Error converting the JSON payload.", jme);
	    }
	    catch (Exception e) {
	        logger.error(e.getMessage());
	        throw new Exception("Error converting the JSON payload.", e);
	    }
	}  	

	URL getResourceURL(String fileName) {
		return this.getClass().getResource(MOCK_BODY + fileName + ".json");
	}

	public CusResourcesServiceHelper getCustomersDrugCoveragesServiceHelper() {
		return customersDrugCoveragesServiceHelper;
	}

	public void setCustomersDrugCoveragesServiceHelper(CusResourcesServiceHelper customersDrugCoveragesServiceHelper) {
		this.customersDrugCoveragesServiceHelper = customersDrugCoveragesServiceHelper;
	}
	
    public CamelContext getContext() {
		return context;
	}

	public void setContext(CamelContext context) {
		this.context = context;
	}

	public CusResourcesResponseHelper getCustomersDrugCoveragesResponseHelper() {
		return this.customersDrugCoveragesResponseHelper;
	}

	public void setCustomersDrugCoveragesResponseHelper(CusResourcesResponseHelper customersDrugCoveragesResponseHelper) {
		this.customersDrugCoveragesResponseHelper = customersDrugCoveragesResponseHelper;
	}

	public void setProducerTemplate(ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;	
	}
	
	public void setActorHelper(ActorHelper actorHelper) {
		this.actorHelper = actorHelper;
	}
	
	public ActorHelper getActorHelper() {
		return this.actorHelper;
	}
}
