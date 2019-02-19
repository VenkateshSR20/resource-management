package com.resourceca.api.customerservice.customers.drugcoverages.service;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import resource.api.utils.security.Actor;
import resource.api.utils.security.BenefitMapping;
import resource.api.utils.security.HoldingMapping;
import resource.api.utils.security.PolicyMapping;
import resource.api.utils.security.Sponsor;
import resource.api.utils.security.Actor.Role;

public class ActorHelperTest {
	
	private ActorHelper actorHelper;
	private Actor actor;
	
	@Before
	public void initializeRequest() {
		this.actorHelper = new ActorHelper();
		this.actor = new Actor(Role.CALL_CENTER_USER);
		Sponsor sponsor = new Sponsor();
		sponsor.setPolicyMappingMap(new PolicyMapping());
		sponsor.getPolicyMappingMap().setHoldingMappings(new ArrayList<HoldingMapping>());
		HoldingMapping holdingElement = new HoldingMapping();
		holdingElement.setPolicyNumber("12345");
		holdingElement.setKey("5656");
		
		BenefitMapping benefitMapping = new BenefitMapping();
		benefitMapping.setType("DENT");
		benefitMapping.setKey("30");
		holdingElement.setBenefitMappings(new ArrayList<BenefitMapping>());
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("DRUGS");
		benefitMapping.setKey("31");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("HCARE");
		benefitMapping.setKey("32");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("DRUGS");
		benefitMapping.setKey("33");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("VIS");
		benefitMapping.setKey("34");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("HCSA");
		benefitMapping.setKey("35");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		benefitMapping = new BenefitMapping();
		benefitMapping.setType("HPS");
		benefitMapping.setKey("36");
		benefitMapping.setHspKey("1");
		holdingElement.getBenefitMappings().add(benefitMapping);
		
		sponsor.getPolicyMappingMap().getHoldingMappings().add(holdingElement);
		
		this.actor.getSponsors().add(sponsor);
		
		//2nd Sponsor
		Sponsor sponsor2 = new Sponsor();
		
		sponsor2.setPolicyMappingMap(new PolicyMapping());
		sponsor2.getPolicyMappingMap().setHoldingMappings(new ArrayList<HoldingMapping>());
		HoldingMapping holdingElement2 = new HoldingMapping();
		holdingElement2.setPolicyNumber("54321");
		holdingElement2.setKey("6565");
		
		BenefitMapping benefitMapping2 = new BenefitMapping();
		benefitMapping2.setType("DRUGS");
		benefitMapping2.setKey("29");
		holdingElement2.setBenefitMappings(new ArrayList<BenefitMapping>());
		holdingElement2.getBenefitMappings().add(benefitMapping2);
		
		sponsor2.getPolicyMappingMap().getHoldingMappings().add(holdingElement2);
		
		this.actor.getSponsors().add(sponsor2);
	}
	
	@Test 
	public void test_addPair_successful() {
		//Arrange
		this.actorHelper.setActor(actor);
		assertTrue(this.actorHelper.getActor()!= null);
		this.actorHelper.addPair("Hello", "world");
		
		//Act
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = this.actorHelper.getPolicyIdBenefitIdPairs("DRUGS", null);
		
		//Assert
		assertNotNull(retVal);
		assertEquals(1, retVal.size());
	}
	
	@Test 
	//public void testAddPair(){
	public void test_policyFiltering_returnsOnePolicy() {
		//Arrange
		this.actorHelper.setActor(actor);
		//this.actorHelper.addPair("Hello", "world");
		/*
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = 
				this.actorHelper.getPolicyIdBenefitIdPairs("DRUG");
		assertTrue(retVal!=null);
		assertEquals(retVal.size(), 1);*/
		
		//Act
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = this.actorHelper.getPolicyIdBenefitIdPairs("DRUGS", "54321");
				
		//Assert
		assertNotNull(retVal);
		assertEquals(1, retVal.size());
	}
	
	@Test 
	public void test_policyFiltering_returnsTwoPolicies() {
		//Arrange
		this.actorHelper.setActor(actor);
		
		//Act
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = this.actorHelper.getPolicyIdBenefitIdPairs("DRUGS", "12345");
		
		//Assert
		assertNotNull(retVal);
		assertEquals(2, retVal.size());
	}
	
	@Test 
	public void test_policyFiltering_returnsThreePoliciesWithNullPolicyBeingPassed() {
		//Arrange
		this.actorHelper.setActor(actor);
		
		//Act
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = this.actorHelper.getPolicyIdBenefitIdPairs("DRUGS", null);
		
		//Assert
		assertNotNull(retVal);
	}
	
	@Test 
	public void test_policyFiltering_returnsNoPolicies() {
		//Arrange
		this.actorHelper.setActor(actor);
		
		//Act
		ArrayList<AbstractMap.SimpleEntry<String, String>> retVal = this.actorHelper.getPolicyIdBenefitIdPairs("DRUGS", "98765");
		
		//Assert
		assertNotNull(retVal);
		assertEquals(0, retVal.size());
	}
}
