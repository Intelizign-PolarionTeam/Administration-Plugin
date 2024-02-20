package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;

public class CustomEnumerationPojo {
	private int customEnumerationCount;
	private String customWorkItemType;
	
	public CustomEnumerationPojo(int customEnumerationCount, ITypeOpt customWorkItemType) {
		super();
		this.customEnumerationCount = customEnumerationCount;
		this.customWorkItemType = customWorkItemType.getName();
	}
	public int getCustomEnumerationCount() {
		return customEnumerationCount;
	}
	public void setCustomEnumerationCount(int customEnumerationCount) {
		this.customEnumerationCount = customEnumerationCount;
	}
	public String getCustomWorkItemType() {
		return customWorkItemType;
	}
	public void setCustomWorkItemType(String customWorkItemType) {
		this.customWorkItemType = customWorkItemType;
	}
	
}
