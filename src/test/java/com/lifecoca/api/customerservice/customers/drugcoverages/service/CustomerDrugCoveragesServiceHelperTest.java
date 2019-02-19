package com.resourceca.api.customerservice.customers.drugcoverages.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Registry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.resourceca.api.customerservice.customers.drugcoverages.service.CustomersDrugCoveragesServiceHelper;

public class CustomerDrugCoveragesServiceHelperTest {

	private CusResourcesServiceHelper customersDrugCoveragesServiceHelper;
	
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
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void initializeRequest() {
		customersDrugCoveragesServiceHelper = new CusResourcesServiceHelper();
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void test_setExchangeCorrelationAndRequestHeaders_SuccessfullySetsHeaderForValidValues() throws Exception{
		//Arrange
		CamelContext cc = new DefaultCamelContext();
		Exchange exchange = new DefaultExchange(cc);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("X-Correlation-ID", "12345");
		headers.put("X-Request-ID", "789456");
		
		//Act
		CusResourcesServiceHelper.setExchangeCorrelationAndRequestHeaders(headers, exchange);
		
		//Assert
		assertEquals("12345", exchange.getIn().getHeader("X-Correlation-ID"));
		assertEquals("789456", exchange.getIn().getHeader("X-Request-ID"));
	}
	
	@Test
	public void test_setExchangeCorrelationAndRequestHeaders_DoesNotSetHeaderForNullHeader() throws Exception{
		//Arrange
		CamelContext cc = new DefaultCamelContext();
		Exchange exchange = new DefaultExchange(cc);
		Map<String, Object> headers = null;
		
		//Act
		CusResourcesServiceHelper.setExchangeCorrelationAndRequestHeaders(headers, exchange);
		
		//Assert
		assertNull(exchange.getIn().getHeader("X-Correlation-ID"));
		assertNull(exchange.getIn().getHeader("X-Request-ID"));
	}	

	
	@Test
	 public void test_lookupProducerTemplate_ReturnsExistingProducerTemplate(){
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.getRegistry()).thenReturn(mockRegistry);
		when(mockExchange.getContext().getRegistry().lookupByName("customerServiceCustomersDrugCoveragesProducerTemplate")).thenReturn(mockProducerTemplate);
		
		//Act
		ProducerTemplate producerTemplate = customersDrugCoveragesServiceHelper.lookupProducerTemplate(mockExchange);
		
		//Assert
		assertTrue(mockProducerTemplate.equals(producerTemplate));
	}
	
	@Test
	public void test_lookupProducerTemplate_ReturnsNewProducerTemplate(){
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.getRegistry()).thenReturn(mockRegistry);		
		when(mockExchange.getContext().getRegistry().lookupByName("customerServiceCustomersDrugCoveragesProducerTemplate")).thenReturn(null);
		when(mockCamelContext.createProducerTemplate()).thenReturn(newProducerTemplate);	
		
		//Act
		ProducerTemplate producerTemplate = customersDrugCoveragesServiceHelper.lookupProducerTemplate(mockExchange);
		
		//Assert
		assertTrue(newProducerTemplate.equals(producerTemplate));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test_isStubable_ThrowsIllegalStateExceptionWhenPropertyCanNotBeResolved() throws Exception{
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_ENABLED}}")).thenThrow(Exception.class);
		
		//Act and Assert
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Unable to read FEATURE_STUB_RESPONSES configurations in this environment.");
		customersDrugCoveragesServiceHelper.isStubable(mockExchange);
	}
	
	@Test
	public void test_isStubable_ReturnsFalseWhenStubResponseIsNotEnabled() throws Exception{
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_ENABLED}}")).thenReturn("false");
		
		//Act and Assert
		assertFalse(customersDrugCoveragesServiceHelper.isStubable(mockExchange));
	}
	
	@Test
	public void test_isStubable_ReturnsTruwWhenStubResponseIsBothEnabledAndDefaulted() throws Exception{
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_ENABLED}}")).thenReturn("true");
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_DEFAULT}}")).thenReturn("true");
		
		//Act and Assert
		assertTrue(customersDrugCoveragesServiceHelper.isStubable(mockExchange));
	}
	
	@Test
	public void test_isStubable_ReturnsTruwWhenStubResponseIsEnabledAndApiModeIsNotBlank() throws Exception{
		//Arrange
		when(mockExchange.getContext()).thenReturn(mockCamelContext);
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_ENABLED}}")).thenReturn("true");
		when(mockCamelContext.resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_DEFAULT}}")).thenReturn("false");
		when(mockExchange.getIn()).thenReturn(mockMessage);
		when(mockMessage.getHeader("api_mode", String.class)).thenReturn("stub_api_200");
		
		//Act and Assert
		assertTrue(customersDrugCoveragesServiceHelper.isStubable(mockExchange));
	}
}
