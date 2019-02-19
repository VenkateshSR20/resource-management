package main.java.com.resources.service;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CusResourcesServiceHelper {
	
	private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
	private static final String HEADER_REQUEST_ID = "X-Request-ID";
	private static final String API_MODE = "api_mode";
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    
	/**
	 * This method checks the exchange to determine if we should MOCK 
	 * the update responses in Orchestration based on configuration settings
	 * and whether the api_mode=stub parameter was passed.
	 * 
	 * @param exchange
	 * @return Boolean
	 * 
	 */
	boolean isStubable(Exchange exchange) {
		String apiMode = ApiTools.resolveGet(() -> exchange.getIn().getHeader(API_MODE, String.class));
		logger.debug("api_mode string: {}", apiMode);
		try {
			boolean isFeatureStubResponseEnabled = Boolean.parseBoolean(exchange.getContext().resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_ENABLED}}"));
			logger.debug("FEATURE_STUB_RESPONSES_ENABLED: {}", isFeatureStubResponseEnabled);
			//if FALSE, never stub response
			if(isFeatureStubResponseEnabled) {
				boolean isFeatureStubResponseDefault = Boolean.parseBoolean(exchange.getContext().resolvePropertyPlaceholders("{{FEATURE_STUB_RESPONSES_DEFAULT}}"));
				logger.debug("FEATURE_STUB_RESPONSES_DEFAULT: {}" ,isFeatureStubResponseDefault);
				//if TRUE stub, if FALSE stub only when param api_mode=stub param is passed
				if(isFeatureStubResponseDefault) {
					return true;
				}
				else {
					return StringUtils.isNotBlank(apiMode);
				}
			}
			
			return false;
		}
		catch (Exception e) {
			//results in 500 error if config values are not present or cannot be read
			logger.error("Unable to read FEATURE_STUB_RESPONSES configurations in this environment. {}", e.getMessage());
			throw new IllegalStateException("Unable to read FEATURE_STUB_RESPONSES configurations in this environment.", e);
		}
	}
	
	
	/**
	 * Set headers X-Correlation-ID, X-Request-ID to the exchange.
	 * 
	 * @param Map<String, Object> headers
	 * @param Exchange exchange
	 */
	public static void setExchangeCorrelationAndRequestHeaders(Map<String, Object> headers, Exchange exchange) {
		if(headers != null) {
			exchange.getIn().setHeader(HEADER_CORRELATION_ID, headers.get(HEADER_CORRELATION_ID));
			exchange.getIn().setHeader(HEADER_REQUEST_ID, headers.get(HEADER_REQUEST_ID));
		}
	}

	
	/**
	 * Set headers X-Correlation-ID, X-Request-ID to the exchange.
	 * 
	 * @param Map<String, Object> headers
	 * @param Exchange exchange
	 */
	public static void setExchangeHeaders(Map<String, Object> headers, Exchange exchange) {
		if(headers != null) {
			exchange.getIn().setHeader(HEADER_CORRELATION_ID, headers.get(HEADER_CORRELATION_ID));
			exchange.getIn().setHeader(HEADER_REQUEST_ID, headers.get(HEADER_REQUEST_ID));
		}
	}
    
	/**
	 * This method lookup for the Producer template by getting from the camel
	 * registry, if not found then it will create a new one.
	 * 
	 * @param Exchange
	 * @throws Exception
	 * @return ProducerTemplate
	 * 
	 */
	public ProducerTemplate lookupProducerTemplate(Exchange exchange) {
		ProducerTemplate producerTemplate = (ProducerTemplate) ApiTools.resolveGet(() -> exchange.getContext().getRegistry().lookupByName("customerServiceCustomersDrugCoveragesProducerTemplate"));
		
		if (producerTemplate == null) {
			producerTemplate = exchange.getContext().createProducerTemplate();
		}
		
		return producerTemplate;
	}
}