package com.intelizign.polarion.custom_user_management.model;


import com.polarion.alm.tracker.model.ITypeOpt;


public class CustomDetailsPojo {

	private int customFieldsCount;
	private String workItemType;
	
	

	public CustomDetailsPojo(int customFieldsCount, ITypeOpt typeEnumObj) {
		super();
		this.customFieldsCount = customFieldsCount;
		this.workItemType = typeEnumObj.getName();
		}

//	public String getCustomFieldName() {
//		return customFieldName;
//	}

//	public void setCustomFieldName(String customFieldName) {
//		this.customFieldName = customFieldName;
	//}

	public String getWorkItemType() {
		return workItemType;
	}

	
	public void setWorkItemType(String workItemType) {
		this.workItemType = workItemType;
	}

	public int getCustomFieldsCount() {
		return customFieldsCount;
	}

	public void setCustomFieldsCount(int customFieldsCount) {
		this.customFieldsCount = customFieldsCount;
	}


}
