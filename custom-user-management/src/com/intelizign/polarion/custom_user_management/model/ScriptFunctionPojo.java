package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;

public class ScriptFunctionPojo {
	private String workItemTypeAction;
	private String scriptWorkItemType;
	private String scriptFunctionName;

	public ScriptFunctionPojo(IAction workItemTypeAction, ITypeOpt scriptWorkItemType, String scriptFunctionName) {
		super();
		this.workItemTypeAction = workItemTypeAction.getName();
		this.scriptWorkItemType = scriptWorkItemType.getName();
		this.scriptFunctionName = scriptFunctionName;
	}

	public String getScriptWorkItemType() {
		return scriptWorkItemType;
	}

	public void setScriptWorkItemType(String scriptWorkItemType) {
		this.scriptWorkItemType = scriptWorkItemType;
	}

	public String getWorkItemTypeAction() {
		return workItemTypeAction;
	}

	public void setWorkItemTypeAction(String workItemTypeAction) {
		this.workItemTypeAction = workItemTypeAction;
	}

	public String getScriptFunctionName() {
		return scriptFunctionName;
	}

	public void setScriptFunctionName(String scriptFunctionName) {
		this.scriptFunctionName = scriptFunctionName;
	}

}
