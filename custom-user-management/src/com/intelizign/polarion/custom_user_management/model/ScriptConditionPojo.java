package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;

public class ScriptConditionPojo {
	private String workItemTypeAction;
	private String scriptWorkItemType;
	private String scriptName;

	public ScriptConditionPojo(IAction workItemTypeAction, ITypeOpt scriptWorkItemType, String scriptName) {
		super();
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
