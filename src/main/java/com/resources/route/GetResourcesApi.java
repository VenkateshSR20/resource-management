package main.java.com.resources.route;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class GetResourcesApi extends RouteBuilder {
    
    private static Logger logger = LoggerFactory.getLogger(GetResourcesApi.class);

	private static final String TRUE = "true";
	private static final String FALSE = "false";
	
	private static final String INTERNAL_SERVER_ERROR_CODE = "500";
	private static final String API_ID = "customer-service-customers";
	private static final String INFO_URL = "https://developers.api.com";
	private static final String QUERY_PARAM_KEY_FOR_MOCK = "api_mode";
	private static final String API_STUB_MODE_INDICATOR = "stub_api";
	private static final String HELP_RESPONSE_INDICATOR = "help";
	private static final String MOCK_BODY = "/mockBody/stub/";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String POLICY_ID = "policyId";
	private static final String BENEFIT_ID = "benefitId";
	

	public static final String GET_RESOURCES_REAL_ENDPOINT = "direct:ApiGetResources";
	
    @Override
    public void configure() throws Exception {
        /*
         * Enable the Camel Stream Caching so that Streams can be re-read if
         * required
         */
        getContext().setStreamCaching(true);
        
        Predicate stubMode = new Predicate(){
			@Override
			public boolean matches(Exchange exchange) {
				String apiMode = ApiTools.resolveGet(() -> exchange.getIn().getHeader(QUERY_PARAM_KEY_FOR_MOCK, String.class));
                return StringUtils.isNotBlank(apiMode) && (apiMode.toLowerCase().contains("help") || MockUtils.isStubbable(exchange, API_STUB_MODE_INDICATOR));
			}
        };
        /*
         * Configure the Netty4-Http component with the settings we want for
         * these services
         */
        restConfiguration()
                .component("jetty")
				.host("{{HTTPS.HOST}}")
				.port("{{HTTPS.PORT}}")
				.scheme("{{HTTPS.SCHEME}}")
                .contextPath("/resource-path/v1")
                .dataFormatProperty("json.out.prettyPrint", TRUE)
                .dataFormatProperty("xml.in.ignoreJAXBElement", TRUE)
                .dataFormatProperty("xml.out.prettyPrint", TRUE)
                .dataFormatProperty("xml.out.mustBeJAXBElement", FALSE)
                .dataFormatProperty("xml.out.ignoreJAXBElement", TRUE)
                .dataFormatProperty("json.out.disableFeatures", "WRITE_DATES_AS_TIMESTAMPS")
                .apiContextPath("/customers-api-docs")
                .apiProperty("api.title", "CanTech Customer Service Rest Service")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("api.contact.name", "Integration Services")
                .apiProperty("cors", TRUE)
                .bindingMode(RestBindingMode.off);
        
        configureExceptionHandling();
        
        rest()
        .get("/customers/{digitalId}/benefits/drug-coverages")
            .id("getCanTechCustomerServiceCustomersDrugCoveragesRest")
            .description("Gets the requested drug-coverages with the requested customer digitalId.")
            .outType(DrugCoveragesResponse.class)
            .route()
        	.to("apilog:aud?includeBody=true&message=Entering route /customers/{digitalId}/benefits/drug-coverages")
	        .choice()
	    		.when(stubMode)
	    			//return a API mock response
	    			.to("direct:invokeMockGetDrugCoverages")
	    		.otherwise()
		        	.routeId("getCanTechCustomerServiceCustomersDrugCoveragesV1Route")
		        	.setHeader(ApiIds.HEADER_NAME, constant(API_ID))
	            	.to("log:" + logger.getName() + "?level=DEBUG&showHeaders=true")
		            
	            	.to("direct-vm:dataSecurityIncoming") // performs datasecurity / privacy
		            .to("bean:getCustomersDrugCoverages?method=getCustomersDrugCoverages")
		            
		            .removeHeaders("*", ApiTools.GENERAL_HEADERS_WHITE_LIST.toArray(new String[0]))
		            .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
		            .marshal(loadJacksonDataFormatValue())
    		.end()
        	.to("apilog:aud?includeBody=true&message=Exiting route /customers/{digitalId}/benefits/drug-coverages");
        

        from("direct:invokeMockGetDrugCoverages")
		.routeId("getDrugCoveragesMockRoute")
        .process(new Processor(){
        	@Override
        	public void process(Exchange exchange) throws Exception {
        		String mockFileName = ApiTools.resolveGet(() -> exchange.getIn().getHeader(QUERY_PARAM_KEY_FOR_MOCK, String.class));
        		mockFileName = mockFileName.replace(API_STUB_MODE_INDICATOR+"_", "");
        	        		
        		if(mockFileName.toLowerCase().contains(HELP_RESPONSE_INDICATOR)){
        			mockFileName = HELP_RESPONSE_INDICATOR;
        		}
        		URL url = this.getClass().getResource(MOCK_BODY + mockFileName + ".json");
        		if (url != null){
        			String statusCode  = (mockFileName.length() >= 3 && NumberUtils.isNumber(mockFileName.substring(0, 3))) ? mockFileName.substring(0, 3) : "200";
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
                    exchange.getIn().setBody(IOUtils.toString(url));
        		}
        		else {
        			logger.error("Unable to set the Mock response from file for api_mode=stub_api, no url match");
					throw new UnableToReadMockResponseException();
        		}
        	}
        });
        
        from(GET_DRUGCOVERAGES_REAL_ENDPOINT)
        	.onException(Exception.class)
	        	.handled(true)
        	.end()
        	.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws Exception {
					Map<String, Object> reqCtx = new HashMap<>();
					String cxfEndpoint = exchange.getContext().resolvePropertyPlaceholders("{{API_BASEURL}}");
					logger.debug("ResourceApi endpoint" + cxfEndpoint);
					reqCtx.put(Message.ENDPOINT_ADDRESS, cxfEndpoint);
					exchange.getIn().removeHeader(Exchange.HTTP_PATH);
					
					logger.debug("policy id " + exchange.getProperty(POLICY_ID).toString());
					exchange.getIn().setHeader(POLICY_ID, exchange.getProperty(POLICY_ID));
					logger.debug("benefit id " + exchange.getProperty(BENEFIT_ID).toString());
					exchange.getIn().setHeader(BENEFIT_ID,exchange.getProperty(BENEFIT_ID));
					
					exchange.getIn().removeHeaders("DataSecurity*");
					exchange.getIn().setHeader(Client.REQUEST_CONTEXT, reqCtx);
					exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
				}
			})
        	.to("apilog:aud?includeBody=true&message=Calling old API benefits/v1/benefits/drug-coverages")
        	.to("{{API_BASEURL}}{{API_BASEURL_QUERY_OPTIONS}}")
        	.to("apilog:aud?includeBody=true&message=Finished calling old API benefits/v1/benefits/drug-coverages");
    }
    
	/**
	 * Returns a Jackson Data Formatter instance to be used in a Resources Camel
	 * Route
	 * 
	 * @return JacksonDataFormat jdf
	 */
	public static JacksonDataFormat loadJacksonDataFormatValue() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_ABSENT);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		mapper.setDateFormat(df);
		JacksonDataFormat jdf = new JacksonDataFormat(mapper, HashMap.class);
		jdf.setPrettyPrint(true);
		return jdf;
	}
    
    
    /**
     * Configures the exception handling for the route.
     */
    protected void configureExceptionHandling() {
    	onException(UnableToReadMockResponseException.class)
    		.process(new Processor() {
			public void process(Exchange exchange) {
				UnableToReadMockResponseException exception = (UnableToReadMockResponseException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
				logger.error(exception.getMessage(), exception);
				exchange.getIn().setHeader(Exchange.CONTENT_TYPE, CONTENT_TYPE_JSON);
				exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, INTERNAL_SERVER_ERROR_CODE);
				exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, "Internal Server Error");
				exchange.getIn().setBody(ApiTools.buildJsonErrorsString(String.format("500.%s.001", API_ID),
						"Internal server error",
						null,
						null,
						INFO_URL));
			}
		})
    	.removeHeaders("*", ApiTools.GENERAL_HEADERS_WHITE_LIST.toArray(new String[0]))
    	.handled(true);
    	
    	onException(ResourceCoveragesApiException.class)
        .to("eventPublisher:{{GENERAL_EXCEPTION_EVENT_NAME}}?category=PROBLEM&publisherId={{GENERAL_EXCEPTION_EVENT_PUBLISHER_ID}}&originator={{GENERAL_EXCEPTION_EVENT_ORIGINATOR}}&summary={{GENERAL_EXCEPTION_EVENT_SUMMARY}} - this was a MDM Exception error.&exceptionAsDesc=true")
    	.process(new Processor() {
			public void process(Exchange exchange) {
				ResourceCoveragesApiException exception = (ResourceCoveragesApiException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
				logger.error(exception.getMessage(), exception);
				exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
				exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.getStatusCode());
				exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, exception.getStatusMessage());
				exchange.getIn().setBody(exception.getResponseBody());
			}
		})
    	.removeHeaders("*", ApiTools.GENERAL_HEADERS_WHITE_LIST.toArray(new String[0]))
    	.handled(true);
    	
    	onException(JsonMappingException.class)
	        .to("eventPublisher:{{GENERAL_EXCEPTION_EVENT_NAME}}?category=PROBLEM&publisherId={{GENERAL_EXCEPTION_EVENT_PUBLISHER_ID}}&originator={{GENERAL_EXCEPTION_EVENT_ORIGINATOR}}&summary={{GENERAL_EXCEPTION_EVENT_SUMMARY}}&exceptionAsDesc=true")
	    	.process(new Processor() {
				public void process(Exchange exchange) {
					Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
					logger.error(exception.getMessage(), exception);
					exchange.getIn().setHeader(Exchange.CONTENT_TYPE, CONTENT_TYPE_JSON);
					exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, INTERNAL_SERVER_ERROR_CODE);
					exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, "Internal server error");
					exchange.getIn().setBody(ApiTools.buildJsonErrorsString(String.format("500.%s.001", API_ID),
							"Internal server error",
							null,
							null,
							INFO_URL));
				}
			})
	    	.removeHeaders("*", ApiTools.GENERAL_HEADERS_WHITE_LIST.toArray(new String[0]))
	    .handled(true);
    	
    	onException(Exception.class)
	        .to("eventPublisher:{{GENERAL_EXCEPTION_EVENT_NAME}}?category=PROBLEM&publisherId={{GENERAL_EXCEPTION_EVENT_PUBLISHER_ID}}&originator={{GENERAL_EXCEPTION_EVENT_ORIGINATOR}}&summary={{GENERAL_EXCEPTION_EVENT_SUMMARY}}&exceptionAsDesc=true")
	    	.process(new Processor() {
				public void process(Exchange exchange) {
					Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
					logger.error(exception.getMessage(), exception);
					exchange.getIn().setHeader(Exchange.CONTENT_TYPE, CONTENT_TYPE_JSON);
					exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, INTERNAL_SERVER_ERROR_CODE);
					exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, "Internal Server Error");
					exchange.getIn().setBody(ApiTools.buildJsonErrorsString(String.format("500.%s.001", API_ID),
							"Internal server error",
							null,
							null,
							INFO_URL));
				}
			})
	    	.removeHeaders("*", ApiTools.GENERAL_HEADERS_WHITE_LIST.toArray(new String[0]))
	    .handled(true);
   }
}
