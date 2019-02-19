package main.java.com.resources.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;


public class ActorHelper {

	Actor actor = null;
	ArrayList<AbstractMap.SimpleEntry<String, String>> listOfPolicyIdBenefitIdPairs;
	
	
	public void setActor(Actor actor){
		listOfPolicyIdBenefitIdPairs = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
		this.actor = actor;
	}
	
	public Actor getActor(){
		return this.actor;
	}
	
	public void addPair(String key, String val){
		this.listOfPolicyIdBenefitIdPairs.add(new AbstractMap.SimpleEntry<String, String>(key, val));
	}
	
	private void exctractListofPolicyIdBenefitIdPairs(String type, String policyNumber) {
		for (Sponsor element : this.actor.getSponsors()) {
			for (HoldingMapping holdingElement : element.getPolicyMappingMap().getHoldingMappings()) {
				String policyId = holdingElement.getKey();
				
				//Add all if its blank
				//Or if it matches with holdingElement.policyNumber
				if (StringUtils.isBlank(policyNumber) || holdingElement.getPolicyNumber().compareTo(policyNumber) == 0) {
					for (BenefitMapping bm : holdingElement.getBenefitMappings()) {
						if (bm.getType().equals(type)) {
							this.addPair(policyId, bm.getKey());
						}
					}
				}
			}
		}
	}
	
	public ArrayList<AbstractMap.SimpleEntry<String, String>> getPolicyIdBenefitIdPairs(String type, String policyNumber){
		if (this.listOfPolicyIdBenefitIdPairs.size() == 0){
			this.exctractListofPolicyIdBenefitIdPairs(type, policyNumber);
		}
		return this.listOfPolicyIdBenefitIdPairs;
	}
}
