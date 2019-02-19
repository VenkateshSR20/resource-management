package com.resourceca.api.customerservice.customers.drugcoverages.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Registry;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourceca.api.customerservice.customers.drugcoverages.route.GetCustomersDrugCoveragesApi;
import com.resourceca.api.customerservice.customers.drugcoverages.transform.CustomersDrugCoveragesResponseHelper;
import com.resourceca.api.customerservice.customers.model.DentalCoveragesResponse;
import com.resourceca.api.customerservice.customers.model.DrugCoveragesResponse;
import com.resourceca.api.utils.ApiTools;

import resource.api.utils.security.Actor;
import resource.api.utils.security.Actor.Role;
import resource.api.utils.security.BenefitMapping;
import resource.api.utils.security.HoldingMapping;
import resource.api.utils.security.PolicyMapping;
import resource.api.utils.security.Sponsor;


public class GetCustomerDrugCoveragesTest {

	@Mock
	private Exchange mockExchange;
	@Mock
	private Registry mockRegistry;
	@Mock
	private CamelContext mockCamelContext;
	@Mock
	private ProducerTemplate mockProducerTemplate;
	@Mock
	private ProducerTemplate newProducerTemplate;
	@Mock
	private Message mockMessage;
	@Mock
	private CusResourcesResponseHelper mockCustomersDrugCoveragesResponseHelper;
	@Mock
	private CusResourcesServiceHelper mockCustomersDrugCoveragesServiceHelper;
	
	//Non-Mocks
	private CusResourcesResponseHelper actualCustomersDrugCoveragesResponseHelper =
			new CusResourcesResponseHelper();
	private static final String SAMPLE_RESPONSE = "/mockBody/test/200.json";
	private static final String INVALID_PAYLOAD = "/mockBody/test/InvalidJson.json";
	private ArrayList<SimpleEntry<String, String>> benefitPairsList = new ArrayList<SimpleEntry<String, String>>();
	private SimpleEntry<String, String> simpleEntry = new SimpleEntry<String, String>("5678", "67890");
	@Spy
	private GetCusResources customersDrugCoverages;
	
	private static Logger logger = LoggerFactory.getLogger(GetCusResources.class);
	
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();
	
	@Before
	public void initializeRequest() {
		MockitoAnnotations.initMocks(this);
		
		customersDrugCoverages.setContext(mockCamelContext);
		customersDrugCoverages.setCustomersDrugCoveragesResponseHelper(actualCustomersDrugCoveragesResponseHelper);
		customersDrugCoverages.setCustomersDrugCoveragesServiceHelper(mockCustomersDrugCoveragesServiceHelper);
		
		this.mockExchange = new DefaultExchange(mockCamelContext);
	}
	
	
	@Test
	public void testgetCustomersDrugCoveragesFailure() throws Exception {
		//Arrange
		CamelContext context = new DefaultCamelContext();
		Exchange exchange = new DefaultExchange(context);
		
		when(mockCustomersDrugCoveragesServiceHelper.isStubable(exchange)).thenReturn(true);
		
		try{
			//Act
			customersDrugCoverages.getCustomersDrugCoverages(exchange);
		}
		catch(Exception e) {
			//Assert
			assertNull(e.getMessage());
		}
	}
	
	@Test
	public void testLoadEncryptedElementsListSuccess() throws Exception {
		
		customersDrugCoverages.loadEncryptedElementsList(mockExchange);
		assertNull(mockExchange.getIn().getHeader("elementsToEncrypt"));
		
		when(mockCamelContext.resolvePropertyPlaceholders("{{ELEMENTS_TO_ENCRYPT}}")).thenReturn("configPropertyValue");
		
		customersDrugCoverages.loadEncryptedElementsList(mockExchange);
		assertEquals("configPropertyValue", mockExchange.getIn().getHeader("elementsToEncrypt"));
	}
	
	@Test
	public void testLoadEncryptedElementsListFailure() {
		try {
			customersDrugCoverages.loadEncryptedElementsList(null);
		}
		catch(IllegalStateException ise) {
			assertEquals("Unable to read ELEMENTS_TO_ENCRYPT configurations in this environment.", ise.getMessage());
		}
	}
	
	@Test
	public void testConvertResponseObjectFailure(){
		try {
			customersDrugCoverages.convertResponseObject(null);
		}
		catch(Exception e) {
			assertEquals("Error converting the JSON payload.", e.getMessage());
		}
		
		try {
			CamelContext context = new DefaultCamelContext();
			Exchange exchange = new DefaultExchange(context);
			customersDrugCoverages.convertResponseObject(exchange);
		}
		catch(Exception e) {
			assertEquals("Error converting the JSON payload.", e.getMessage());
		}
		
		try{
			CamelContext context = new DefaultCamelContext();
			Exchange exchange = new DefaultExchange(context);
			exchange.getIn().setBody(new Object());
			customersDrugCoverages.convertResponseObject(exchange);
		}
		catch(Exception e) {
			assertEquals("Error converting the JSON payload.", e.getMessage());
		}
	}
	
	
	@Test
	 public void createRequestAndSendToDrugCoveragesEndpointIsSuccessfulForValidRequest() throws Exception{
		
		CamelContext context = new DefaultCamelContext();
		Exchange exchange = new DefaultExchange(context);
		String jsonPayload = getJsonPayload();
		DrugCoveragesResponse expectedDrugCoveragesResponse = null;
		
		ObjectMapper objMapper = new ObjectMapper();
		objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			expectedDrugCoveragesResponse = objMapper.readValue(jsonPayload, DrugCoveragesResponse.class);
		}catch(IOException e){
			//fail(e.getMessage());
			logger.debug(e.getMessage());
		}
		
		DrugCoveragesResponse actualDrugCoveragesResponse = null;
		
		when(mockCustomersDrugCoveragesServiceHelper.isStubable(exchange)).thenReturn(false);
		when(mockCustomersDrugCoveragesServiceHelper.lookupProducerTemplate(exchange)).thenReturn(mockProducerTemplate);
		when(mockProducerTemplate.send(GetResourcesApi.GET_DRUGCOVERAGES_REAL_ENDPOINT,exchange)).thenReturn(exchange);//If needed need to change to multicast
		
		exchange.getIn().setBody(expectedDrugCoveragesResponse);
		
		//Act
		try {
			customersDrugCoverages.setProducerTemplate(mockProducerTemplate);
			actualDrugCoveragesResponse = customersDrugCoverages.createRequestAndSendToDrugCoveragesEndpoint(exchange, true, "test_api_200", "");
		}catch(Exception e) {
			assertNull(e.getMessage());
			logger.debug("Sending request to REAL Covered Persons Backend.");
		}
		
		logger.debug(objMapper.writeValueAsString(actualDrugCoveragesResponse));
		//Assert
		assertEquals(null, actualDrugCoveragesResponse);
	}
	
	@Test
	public void convertResponseObject_JsonInvalid_JsonMappingExceptionExceptionThrown() throws Exception{
	
		//Arrange
		ObjectMapper mapperObj = new ObjectMapper();
		String jsonPayload = this.getFileAsString(INVALID_PAYLOAD);
		Exchange exchange = new DefaultExchange(mockCamelContext);
		exchange.getIn().setBody(jsonPayload);
		
		//Assert & Act
		exceptionRule.expect(Exception.class);
		exceptionRule.expectMessage("Error converting the JSON payload.");	
		customersDrugCoverages.convertResponseObject(exchange);
	}
	
	
	@Test
	public void createRequestAndSendToBenefitEndpointIsFailureForMockRequest() {
		
		//Arrange
		String apiMode = "test_api_500";
		
		//Act
		try {
			customersDrugCoverages.getTestMockResponse(apiMode, null, null);
		}catch(Exception e) {
			//assertNotNull(e.getMessage());
			return;
		}
		//Assert
		fail("Should throw exception");
	}
	
	@Test
	public void convertResponseObjectTest() {
		//Arrange
		CamelContext context = new DefaultCamelContext();
		Exchange exchange = new DefaultExchange(context);
		String jsonPayload = getJsonPayload();
		
		ObjectMapper objMapper = new ObjectMapper();
		objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			DrugCoveragesResponse response = objMapper.readValue(jsonPayload, DrugCoveragesResponse.class);
			exchange.getIn().setBody(response);
		}catch(IOException e){
			logger.debug(e.getMessage());
		}
		
		//Act
		try{
			customersDrugCoverages.convertResponseObject(exchange);
		}
		catch(Exception ex) {
			fail(ex.getMessage());
			logger.debug(ex.getMessage());
		}
	}
	
	private String getJsonPayload() {
		
		String jsonPayload = "{\"data\": "
				+ "{\"customerRecords\": [{\"id\": \"1234\","
				+ "\"drugCoverages\": "
				+ "[{\"policy\": {\"id\": \"5678\","
				+ "\"policyNumber\": \"189432\"},\"benefit\": "
				+ "{\"id\": \"67890\"},"
				+ "\"coverageStatus\": "
				+ "{\"code\": \"9001\",\"description\": "
				+ "\"Family Coverage\"},\"featureExclusion\": "
				+ "{\"code\": \"3\","
				+ "\"description\": \"Special\","
				+ "\"messages\": [{\"url\": \"http:test.com\","
				+ "\"text\": \"www.test.com\"}]},"
				+ "\"benefitEffectiveDateMessage\": "
				+ "\"Our records show you will have coverage December 15, 2017.\","
				+ "\"hasCustomPriorAuthorizationForms\": false,"
				+ "\"isDrugLookupFeatureAllowed\": true,"
				+ "\"tierPlan\": \"three\","
				+ "\"tiers\": [{\"type\": \"tierOne\","
				+ "\"name\": \"Specialty\"},"
				+ "{\"type\": \"tierTwo\",\"name\": \"Acute\"},"
				+ "{\"type\": \"tierThree\",\"name\": \"Maintenance\"}],"
				+ "\"deductibles\": [{\"detail\": "
				+ "{\"tierOne\": [\"Not Applicable\"],"
				+ "\"tierTwo\": [\"Not Applicable\"],"
				+ "\"tierThree\": [\"Not Applicable\"]},"
				+ "\"applicableBenefits\": [{\"name\": {\"code\": \"DRUGS\","
				+ "\"description\": \"Drugs\"},"
				+ "\"isCombinedWithOtherPolicy\": false},"
				+ "{\"name\": {\"code\": \"VIS\",\"description\": \"Vision\"},"
				+ "\"isCombinedWithOtherPolicy\": false},"
				+ "{\"name\": {\"code\": \"HCARE\","
				+ "\"description\": \"Health\"},\"isCombinedWithOtherPolicy\": false}],"
				+ "\"preferredProviderMessage\": [\"Your plan has a Pharmacy Network Value Plan. Eligible expenses for prescription drugs....\"]}],"
				+ "\"overallMaximums\": [{\"details\": [\"$11,000,000 lifetime\"],"
				+ "\"applicableBenefits\": [{\"name\": {\"code\": \"HCARE\","
				+ "\"description\": \"Health\"},\"isCombinedWithOtherPolicy\": false},"
				+ "{\"name\": {\"code\": \"VIS\",\"description\": \"Vision\"},"
				+ "\"isCombinedWithOtherPolicy\": false}]}],"
				+ "\"overallReinstatements\": [{\"details\": [\"$1,000 annually\"],"
				+ "\"applicableBenefits\": [{\"name\": {\"code\": \"HCARE\",\"description\": "
				+ "\"Health\"},\"isCombinedWithOtherPolicy\": false},"
				+ "{\"name\": {\"code\": \"VIS\",\"description\": \"Vision\"},"
				+ "\"isCombinedWithOtherPolicy\": false}]}],\"payables\": "
				+ "[{\"applicableBenefits\": [{\"name\": {\"code\": \"HCARE\","
				+ "\"description\": \"Health\"},\"isCombinedWithOtherPolicy\": false},"
				+ "{\"name\": {\"code\": \"VIS\",\"description\": \"Vision\"},"
				+ "\"isCombinedWithOtherPolicy\": false}],\"preferredProviderDescriptions\": [],"
				+ "\"preferredProviderMessages\": [\"95% for drugs purchased from Costco "
				+ "Wholesale Canada Ltd or one of its affiliates using the prescription drug "
				+ "identification card (does not apply to purchases in Quebec).\"]}],"
				+ "\"maximumsAndFrequencies\": "
				+ "[{\"coverages\": [{\"tierOne\": [\"$10,000 per calendar year(s)\"],"
				+ "\"tierTwo\": [\"$15,000 per calendar year(s)\"],"
				+ "\"tierThree\": [\"Not Applicable\"]}],\"applicableBenefits\": []}],"
				+ "\"dispenseFeeLimits\": {\"tierOne\": {\"limit\": \"$5.00\",\"payableAt\": \"100%\"},"
				+ "\"tierTwo\": {\"limit\": \"$7.50\",\"payableAt\": \"80%\"},"
				+ "\"tierThree\": {\"limit\": \"$9.00\",\"payableAt\": \"75%\"}},"
				+ "\"planVariationMessage\": {\"tierOne\": [\"Your plan covers specialty medications "
				+ "which are generally used to treat complex or rare conditions.\"],"
				+ "\"tierTwo\": [\"Your plan covers acute medications used treat "
				+ "one-time or short-term conditions.\"],"
				+ "\"tierThree\": [\"Your plan covers maintenance medications "
				+ "which are often prescribed to treat chronic or long-term conditions.\"],"
				+ "\"therapeuticClassPricing\": [\"Your plan has Therapeutic Class Pricing. "
				+ "<href >Click here for details.<href>\"],"
				+ "\"priceFiles\": [\"lorem ipsum\"],"
				+ "\"policyVariations\": [\"Your plan contains cost containment and "
				+ "cost-sharing options. For complete details of this feature, "
				+ "please refer to the benefit information provided by your "
				+ "plan sponsor or contact a customer service representative "
				+ "for assistance.\"],"
				+ "\"deferredReimbursements\": [\"lorem ipsum\"]},"
				+ "\"policyVariationBenefitMessages\": "
				+ "[{\"messages\": [\"Your plan includes Deferred Reimbursement, "
				+ "please refer to the benefits information\"]}]}]}]},"
				+ "\"meta\": {\"uuid\": \"ae87448d-e0ff-40c6-b64d-2c118d62cb1c\","
				+ "\"requestId\": \"8737ccbc-210f-4d47-a65f-3f1bd149824c\","
				+ "\"correlationId\": \"190d4b2d-01c6-42bb-9d49-5edf646af874\"}}";
				
		return jsonPayload;
	}
	
	public Actor getActor() {			
		Actor actor = new Actor(Role.CALL_CENTER_USER);
		actor.setSponsors(new ArrayList<Sponsor>());
		actor.getSponsors().add(new Sponsor());
		actor.getSponsors().get(0).setPolicyMappingMap(new PolicyMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().setHoldingMappings(new ArrayList<HoldingMapping>());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().add(new HoldingMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setPolicyNumber("12345");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setKey("999");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setBenefitMappings(new ArrayList<BenefitMapping>());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).getBenefitMappings().add(new BenefitMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).getBenefitMappings().get(0).setKey("5678");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).getBenefitMappings().get(0).setHspKey("hspkey");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).getBenefitMappings().get(0).setType("testActor");
		
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().add(new HoldingMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).setPolicyNumber("12345");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).setKey("5678");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).setBenefitMappings(new ArrayList<BenefitMapping>());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).getBenefitMappings().add(new BenefitMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).getBenefitMappings().get(0).setKey("67890");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).getBenefitMappings().get(0).setType("DRUGS");
		
		return actor;
	}
	
	protected String getFileAsString(String fileName) throws IOException {
		return IOUtils.toString(this.getClass().getResource(fileName));
	}
}
