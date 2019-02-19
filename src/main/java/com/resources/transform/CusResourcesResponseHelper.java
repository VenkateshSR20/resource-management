package main.java.com.resources.transform;


import java.util.Base64;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import aisc.core.security.api.PropertyPlaceholderCryptoHandler;


public class CusResourcesResponseHelper {

	private static Logger logger = LoggerFactory.getLogger(CusResourcesResponseHelper.class);
	
	private PropertyPlaceholderCryptoHandler cryptoHandler;
	
	public DrugCoveragesResponse unmarshall(Actor actor, String payload) throws Exception {
	
		return updateResponseWithPolicyNumber(actor, getDrugCoveragesResponse(payload));

	}
	
	DrugCoveragesResponse getDrugCoveragesResponse(String payload) throws Exception {
		DrugCoveragesResponse response = null;
		ObjectMapper objMapper = new ObjectMapper();
		objMapper.setSerializationInclusion(Include.NON_NULL);
		objMapper.setSerializationInclusion(Include.NON_EMPTY);
		objMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		objMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		objMapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
		objMapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
		objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		logger.debug("incoming json from DrugCoverages API DrugCoverages  resource " + payload);
		response = objMapper.readValue(payload, DrugCoveragesResponse.class);
		
		//replace {} empty objects
		String sJson = objMapper.writeValueAsString(response);
		String sReplaced = sJson.replace("{ }", "null");
		
		logger.debug("outgoing json from Customer Service Customers DrugCoverages resource " + sReplaced );
		response = objMapper.readValue(sReplaced, DrugCoveragesResponse.class);		
		return response;
	}
	
	/*
	 * riko - Nov. 28, 2018.
	 * New method to handle Test mock response. It will make sure that the response is updated with correct policy and benefit ids and all security restrictions/policy filtering are applied
	 * */
	public DrugCoveragesResponse unmarshallTestMock(Actor actor, String payload, String policyId, String benefitId) throws Exception {
		DrugCoveragesResponse response = null;
		//Gets cleaned up response JSON first. Just like a real response.
		response = getDrugCoveragesResponse(payload);
		//Since it is a test mock, the policy and benefit ids might not be applicable to the DigitalID provided in the request.
		//This updates mock response with the correct values for the provided DigitalID
		response = updateTestMockResponseWithPolicyIdAndBenefitId(response, policyId, benefitId);
		logger.debug("updated json for Customer Service Customers DrugCoverages after updateTestMockResponseWithPolicyIdAndBenefitId got called " + response );
		//Applies security rules and policy filtering just like a real response would do.
		response =  updateResponseWithPolicyNumber(actor, response);
		
		return response;
	}
	
	 DrugCoveragesResponse updateTestMockResponseWithPolicyIdAndBenefitId(DrugCoveragesResponse response, String policyId, String benefitId) {
		List<DrugCoveragesCustomerRecord> customerRecordList = response.getData().getCustomerRecords();
		for(DrugCoveragesCustomerRecord customerRecord: customerRecordList) {
			List<DrugCoverage> drugList = customerRecord.getDrugCoverages();
			for(DrugCoverage coverage: drugList){
				coverage.getPolicy().setId(policyId);
				coverage.getBenefit().setId(benefitId);
			}
		}
		return response;
	}
	
	public  boolean hasException(Exchange exchange) throws ResourceCoveragesApiException {
		Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
		
		if (exception != null) {
			logger.error("A DrugCoverages API exception has been thrown");

	    	HttpOperationFailedException e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
	    	String responseBody = e.getResponseBody();
	    	String statusMessage = e.getStatusText();
	        int statusCode = e.getStatusCode();
			
			logger.error("Received error from DrugCoverages API");
			throw new ResourceCoveragesApiException("Received error from DrugCoverages API.", Integer.toString(statusCode), statusMessage, responseBody);
			}
		return false;
	}
	
	DrugCoveragesResponse updateResponseWithPolicyNumber(Actor actor, DrugCoveragesResponse response) {
		List<DrugCoveragesCustomerRecord> customerRecordList = response.getData().getCustomerRecords();
		for(DrugCoveragesCustomerRecord customerRecord: customerRecordList) {
			List<DrugCoverage> drugList = customerRecord.getDrugCoverages();
			for(DrugCoverage coverage: drugList){
				this.mapPolicyNumberFromPolicyKey(actor, coverage);
			}
		}
		return response;
	}
	
	void mapPolicyNumberFromPolicyKey(Actor actor, DrugCoverage drugCoverage) {
		String policyId = drugCoverage.getPolicy().getId();
		String decryptedValue = policyId;
		
		if(policyId.startsWith("ENC_")) {
			//if encrypted, grab the value between brackets
			String encodedBytes = policyId.substring(4, policyId.length());
			//decode the value from the Base64 encoding
			byte[] decodedValue = Base64.getUrlDecoder().decode(encodedBytes);
			String encryptedValue = new String(decodedValue);
			logger.debug("Encrypted value: {}", encryptedValue);
			
			//decrypt the value
			decryptedValue = getCryptoHandler().decrypt(encryptedValue);
		}
		
		logger.debug("decryptedValue = " + decryptedValue);
		
		String policyNumber = null;
		List<Sponsor> sponsorList = actor.getSponsors();
		for(Sponsor sponsor:sponsorList) {
			List<HoldingMapping> holdingMappings = sponsor.getPolicyMappingMap().getHoldingMappings();
			for(HoldingMapping holding:holdingMappings) {
				if(holding.getKey().equalsIgnoreCase(decryptedValue)) {
					policyNumber = holding.getPolicyNumber();
					break;
				}
			}
		}
		drugCoverage.getPolicy().setPolicyNumber(policyNumber);
	}
	
	public PropertyPlaceholderCryptoHandler getCryptoHandler() {
		return cryptoHandler;
	}

	public void setCryptoHandler(PropertyPlaceholderCryptoHandler cryptoHandler) {
		this.cryptoHandler = cryptoHandler;
	}
}