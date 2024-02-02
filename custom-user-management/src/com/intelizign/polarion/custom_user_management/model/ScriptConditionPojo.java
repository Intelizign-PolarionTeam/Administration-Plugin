package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;

public class ScriptConditionPojo {
	private int scriptCount;
	private String workItemTypeAction;
	private String scriptWorkItemType;
	private String scriptName;


	public ScriptConditionPojo(int scriptCount, IAction workItemTypeAction, ITypeOpt scriptWorkItemType, String scriptName) {
		super();
		this.scriptCount = scriptCount;
		this.workItemTypeAction = workItemTypeAction.getName();
		this.scriptWorkItemType = scriptWorkItemType.getName();
		this.scriptName = scriptName;
	}

	public String getScriptName() {
		return scriptName;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}


	public int getScriptCount() {
		return scriptCount;
	}

	public void setScriptCount(int scriptCount) {
		this.scriptCount = scriptCount;
	}

	public String getWorkItemTypeAction() {
		return workItemTypeAction;
	}

	public void setWorkItemTypeAction(String workItemTypeAction) {
		this.workItemTypeAction = workItemTypeAction;
	}

	public String getScriptWorkItemType() {
		return scriptWorkItemType;
	}

	public void setScriptWorkItemType(String scriptWorkItemType) {
		this.scriptWorkItemType = scriptWorkItemType;
	}

}