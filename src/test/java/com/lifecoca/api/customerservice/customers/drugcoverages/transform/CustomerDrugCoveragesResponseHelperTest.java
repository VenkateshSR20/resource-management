package com.resourceca.api.customerservice.customers.drugcoverages.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.resourceca.api.customerservice.customers.drugcoverages.exception.DrugCoveragesApiException;
import com.resourceca.api.customerservice.customers.model.DrugCoverage;
import com.resourceca.api.customerservice.customers.model.DrugCoveragesCustomerRecord;
import com.resourceca.api.customerservice.customers.model.DrugCoveragesData;
import com.resourceca.api.customerservice.customers.model.DrugCoveragesResponse;
import com.resourceca.api.customerservice.customers.model.DrugPolicy;
import com.resourceca.api.customerservice.customers.model.IdReference;

import aisc.core.security.api.PropertyPlaceholderCryptoHandler;
import resource.api.utils.security.Actor;
import resource.api.utils.security.Actor.Role;
import resource.api.utils.security.HoldingMapping;
import resource.api.utils.security.PolicyMapping;
import resource.api.utils.security.Sponsor;

public class CustomerDrugCoveragesResponseHelperTest {

	@Mock
	private PropertyPlaceholderCryptoHandler mockCryptoHandler;
	
	@Spy
	private CusResourcesResponseHelper spyCustomersDrugCoveragesResponseHelper;
	
	//Non-mocks
	private CusResourcesResponseHelper customersDrugCoveragesResponseHelper;
	private static final String SAMPLE_RESPONSE = "/mockBody/test/200.json";
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void initializeRequest() {
		customersDrugCoveragesResponseHelper = new CusResourcesResponseHelper();
		MockitoAnnotations.initMocks(this);
	}
	
	public Actor getActor() {
		Actor actor = new Actor(Role.CALL_CENTER_USER);
		actor.setSponsors(new ArrayList<Sponsor>());
		actor.getSponsors().add(new Sponsor());
		actor.getSponsors().get(0).setPolicyMappingMap(new PolicyMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().setHoldingMappings(new ArrayList<HoldingMapping>());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().add(new HoldingMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setPolicyNumber("12345");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setKey("5678");
		
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().add(new HoldingMapping());
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).setPolicyNumber("12345");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(1).setKey("999");
		return actor;
	}
	
	protected String getFileAsString(String fileName) throws IOException {
		return IOUtils.toString(this.getClass().getResource(fileName));
	}
	
	@Test
	public void test_mapPolicyNumberFromPolicyKey_SetsPolicyNumberForUnencryptedPolicyId() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("999");
		
		//Act
		customersDrugCoveragesResponseHelper.mapPolicyNumberFromPolicyKey(actor, drugCoverage);
		
		//Assert
		assertEquals("12345", drugCoverage.getPolicy().getPolicyNumber());
	}

	@Test
	public void test_mapPolicyNumberFromPolicyKey_DoesNotSetPolicyNumberAsPolicyIdDoesNotMatchHoldingKey() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("888");
		
		//Act
		customersDrugCoveragesResponseHelper.mapPolicyNumberFromPolicyKey(actor, drugCoverage);
		
		//Assert
		assertNull(drugCoverage.getPolicy().getPolicyNumber());
	}
	
	@Test
	public void test_mapPolicyNumberFromPolicyKey_SetsPolicyNumberForEncryptedPolicyId() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("ENC_NzczZWJhMTYtNDQ0MS00ZjgzLWEzYmUtNTA3ODQzMjQ0NWZhOmswd1drdmptRWZ1TENXVWErY1NlUlE9PQ");
		
		spyCustomersDrugCoveragesResponseHelper.setCryptoHandler(mockCryptoHandler);
		when(mockCryptoHandler.decrypt(anyString())).thenReturn("999");
		
		//Act
		spyCustomersDrugCoveragesResponseHelper.mapPolicyNumberFromPolicyKey(actor, drugCoverage);
		
		//Assert
		assertEquals("12345", drugCoverage.getPolicy().getPolicyNumber());

	}
	
	@Test
	public void test_mapPolicyNumberFromPolicyKey_DoesNotSetPolicyNumberAsEncryptedPolicyIdDoesNotMatchHoldingKey() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("ENC_NzczZWJhMTYtNDQ0MS00ZjgzLWEzYmUtNTA3ODQzMjQ0NWZhOmswd1drdmptRWZ1TENXVWErY1NlUlE9PQ");
		
		spyCustomersDrugCoveragesResponseHelper.setCryptoHandler(mockCryptoHandler);
		when(mockCryptoHandler.decrypt(anyString())).thenReturn("888");
		
		//Act
		spyCustomersDrugCoveragesResponseHelper.mapPolicyNumberFromPolicyKey(actor, drugCoverage);
		
		//Assert
		assertNull(drugCoverage.getPolicy().getPolicyNumber());
	}
	
	@Test
	public void test_updateResponseWithPolicyNumber_SetsPolicyNumberForMatchingPolicyId() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoveragesResponse response = new DrugCoveragesResponse();
		DrugCoveragesData drugCoveragesData = new DrugCoveragesData();
		response.setData(drugCoveragesData);
		drugCoveragesData.getCustomerRecords().add(new DrugCoveragesCustomerRecord());
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("999");
		drugCoveragesData.getCustomerRecords().get(0).getDrugCoverages().add(drugCoverage);
		
		//Act
		customersDrugCoveragesResponseHelper.updateResponseWithPolicyNumber(actor, response);
		
		//Assert
		assertEquals("12345", drugCoverage.getPolicy().getPolicyNumber());
	}
	
	
	@Test
	public void test_updateResponseWithPolicyNumber_DoesNotSetPolicyNumberAsPolicyIdDoesNotMatchHoldingKey() throws Exception {
		//Arrange
		Actor actor = getActor();
		
		DrugCoveragesResponse response = new DrugCoveragesResponse();
		DrugCoveragesData drugCoveragesData = new DrugCoveragesData();
		response.setData(drugCoveragesData);
		drugCoveragesData.getCustomerRecords().add(new DrugCoveragesCustomerRecord());
		DrugCoverage drugCoverage = new DrugCoverage();
		drugCoverage.setPolicy(new DrugPolicy());
		drugCoverage.getPolicy().setId("888");
		drugCoveragesData.getCustomerRecords().get(0).getDrugCoverages().add(drugCoverage);
		
		//Act
		customersDrugCoveragesResponseHelper.mapPolicyNumberFromPolicyKey(actor, drugCoverage);
		
		//Assert
		assertNull(drugCoverage.getPolicy().getPolicyNumber());
	}
	
	@Test
	public void test_hasException_returnsFalseWhenNoExceptionCaught() throws Exception {
		//Arrange
    	CamelContext context = new DefaultCamelContext();
    	Exchange exchange = new DefaultExchange(context);
		
		//Act and Assert
		assertFalse(customersDrugCoveragesResponseHelper.hasException(exchange));
	}
	
	@Test
	public void test_hasException_returnsTrueWhenAnExceptionCaught() throws Exception {
		//Arrange
    	CamelContext context = new DefaultCamelContext();
    	Exchange exchange = new DefaultExchange(context);
    	HttpOperationFailedException exception = new HttpOperationFailedException("uri", 500, "statusText", null, null, null);
    	exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
		
		//Act
    	try {
    		customersDrugCoveragesResponseHelper.hasException(exchange);
    		fail();
    	}
		catch(ResourceCoveragesApiException e) {
		//Assert	
			assertEquals("500", e.getStatusCode());
			assertEquals("statusText", e.getStatusMessage());
		}
	}
	
	@Test
	public void test_updateTestMockResponseWithPolicyIdAndBenefitId_updatesThePolicyIdAndBenefitId(){
		
		//Arrange
		DrugCoveragesResponse drugCoveragesResponse = new DrugCoveragesResponse();
		drugCoveragesResponse.setData(new DrugCoveragesData());
		drugCoveragesResponse.getData().setCustomerRecords(new ArrayList<DrugCoveragesCustomerRecord>());
		drugCoveragesResponse.getData().getCustomerRecords().add(new DrugCoveragesCustomerRecord());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).setDrugCoverages(new ArrayList<DrugCoverage>());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().add(new DrugCoverage());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).setPolicy(new DrugPolicy());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().setPolicyNumber("12345");
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).setBenefit(new IdReference());
		
		//Act
		customersDrugCoveragesResponseHelper.updateTestMockResponseWithPolicyIdAndBenefitId(drugCoveragesResponse, "789456", "11111");
		
		//Assert
		assertEquals("789456",drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getId());
		assertEquals("11111",drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefit().getId());
	}
	
	
	@Test
	public void test_updateTestMockResponseWithPolicyIdAndBenefitId_doesNotUpdateThePolicyIdAndBenefitIdWhenDentalBalanceDoesNotExist(){
		
		//Arrange
		DrugCoveragesResponse drugCoveragesResponse = new DrugCoveragesResponse();
		drugCoveragesResponse.setData(new DrugCoveragesData());
		drugCoveragesResponse.getData().setCustomerRecords(new ArrayList<DrugCoveragesCustomerRecord>());
		drugCoveragesResponse.getData().getCustomerRecords().add(new DrugCoveragesCustomerRecord());
		
		//Act
		customersDrugCoveragesResponseHelper.updateTestMockResponseWithPolicyIdAndBenefitId(drugCoveragesResponse, "789456", "11111");
		
		//Assert
		assertTrue(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().size() == 0);
	}
	
	
	@Test
	public void test_unmarshallTestMock_returnsValidDrugCoveragesResponse() throws Exception{
		//Arrange
		String body = this.getFileAsString(SAMPLE_RESPONSE);
		Actor actor = getActor();
		
		//Act
		DrugCoveragesResponse drugCoveragesResponse = customersDrugCoveragesResponseHelper.unmarshallTestMock(actor, body, "5678", "67890");
		
		//Assert
		assertNotNull(drugCoveragesResponse.getData());
		assertNotNull(drugCoveragesResponse.getMeta());
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getId(), "1234");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getId(), "5678");
		//assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getPolicyNumber(), "12345");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getCoverageStatus().getCode(), "9001");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefitEffectiveDateMessage(), "Our records show you will have coverage December 15, 2017.");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getFeatureExclusion().getCode(), "3");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getHasCustomPriorAuthorizationForms(), false);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsDrugLookupFeatureAllowed(), true);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getTierPlan(), "three");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getLimit(), "$5.00");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getPayableAt(), "100%");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsSmartPlan(), null);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDeductibles().size(), 1);
		assertEquals("190d4b2d-01c6-42bb-9d49-5edf646af874",drugCoveragesResponse.getMeta().getCorrelationId());
		assertEquals("8737ccbc-210f-4d47-a65f-3f1bd149824c", drugCoveragesResponse.getMeta().getRequestId());
		assertEquals("ae87448d-e0ff-40c6-b64d-2c118d62cb1c", drugCoveragesResponse.getMeta().getUuid());
	}
	
	@Test
	public void test_getDrugCoveragesResponse_PopulateAllFields() throws Exception {
		//Arrange
		String body = this.getFileAsString(SAMPLE_RESPONSE);
		
		//Act
		DrugCoveragesResponse drugCoveragesResponse = customersDrugCoveragesResponseHelper.getDrugCoveragesResponse(body);
		assertAllFields(drugCoveragesResponse);
	}
	
	@Test
	public void test_Unmarshall_PopulateAllFields() throws Exception {
		//Arrange
		String body = this.getFileAsString(SAMPLE_RESPONSE);
		Actor actor = getActor();
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setPolicyNumber("12345");
		actor.getSponsors().get(0).getPolicyMappingMap().getHoldingMappings().get(0).setKey("5678");
		//Act
		DrugCoveragesResponse drugCoveragesResponse = customersDrugCoveragesResponseHelper.unmarshall(actor, body);
		//Assert
		assertAllFieldsExceptPolicyNumber(drugCoveragesResponse);
	}

	/**
	 * @param dentalBalanceResponse
	 */
	
	
	public void assertAllFieldsExceptPolicyNumber(DrugCoveragesResponse drugCoveragesResponse) {
		assertNotNull(drugCoveragesResponse.getData());
		assertNotNull(drugCoveragesResponse.getMeta());
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getId(), "1234");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getId(), "5678");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefit().getId(), "67890");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getCoverageStatus().getCode(), "9001");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefitEffectiveDateMessage(), "Our records show you will have coverage December 15, 2017.");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getFeatureExclusion().getCode(), "3");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getHasCustomPriorAuthorizationForms(), false);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsDrugLookupFeatureAllowed(), true);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getTierPlan(), "three");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getLimit(), "$5.00");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getPayableAt(), "100%");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsSmartPlan(), null);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDeductibles().size(), 1);
		assertEquals("190d4b2d-01c6-42bb-9d49-5edf646af874",drugCoveragesResponse.getMeta().getCorrelationId());
		assertEquals("8737ccbc-210f-4d47-a65f-3f1bd149824c", drugCoveragesResponse.getMeta().getRequestId());
		assertEquals("ae87448d-e0ff-40c6-b64d-2c118d62cb1c", drugCoveragesResponse.getMeta().getUuid());
	}
	
	public void assertAllFields(DrugCoveragesResponse drugCoveragesResponse) {
		assertNotNull(drugCoveragesResponse.getData());
		assertNotNull(drugCoveragesResponse.getMeta());
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getId(), "1234");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getId(), "5678");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefit().getId(), "67890");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().getPolicyNumber(), null);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getCoverageStatus().getCode(), "9001");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getBenefitEffectiveDateMessage(), "Our records show you will have coverage December 15, 2017.");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getFeatureExclusion().getCode(), "3");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getHasCustomPriorAuthorizationForms(), false);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsDrugLookupFeatureAllowed(), true);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getTierPlan(), "three");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getLimit(), "$5.00");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDispenseFeeLimits().getTierOne().getPayableAt(), "100%");
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getIsSmartPlan(), null);
		assertEquals(drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getDeductibles().size(), 1);
		assertEquals("190d4b2d-01c6-42bb-9d49-5edf646af874",drugCoveragesResponse.getMeta().getCorrelationId());
		assertEquals("8737ccbc-210f-4d47-a65f-3f1bd149824c", drugCoveragesResponse.getMeta().getRequestId());
		assertEquals("ae87448d-e0ff-40c6-b64d-2c118d62cb1c", drugCoveragesResponse.getMeta().getUuid());
	}
	
	public DrugCoveragesResponse getDrugCoveragesResponse() {
		DrugCoveragesResponse drugCoveragesResponse = new DrugCoveragesResponse();
		drugCoveragesResponse.setData(new DrugCoveragesData());
		drugCoveragesResponse.getData().setCustomerRecords(new ArrayList<DrugCoveragesCustomerRecord>());
		drugCoveragesResponse.getData().getCustomerRecords().add(new DrugCoveragesCustomerRecord());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).setDrugCoverages(new ArrayList<DrugCoverage>());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().add(new DrugCoverage());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).setPolicy(new DrugPolicy());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().setPolicyNumber("12345");
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).getPolicy().setId("999");
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(0).setBenefit(new IdReference());

		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().add(new DrugCoverage());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(1);
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(1).setPolicy(new DrugPolicy());
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(1).getPolicy().setId("999");
		drugCoveragesResponse.getData().getCustomerRecords().get(0).getDrugCoverages().get(1).getPolicy().setPolicyNumber("189781");
		
		return drugCoveragesResponse;
	}
	
}
