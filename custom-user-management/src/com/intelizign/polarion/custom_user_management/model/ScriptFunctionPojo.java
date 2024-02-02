package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;

public class ScriptFunctionPojo {
	private int scriptFunctionCount;
	private String workItemTypeAction;
	private String scriptWorkItemType;
	private String scriptFunctionName;

	public ScriptFunctionPojo(int scriptFunctionCount, IAction workItemTypeAction, ITypeOpt scriptWorkItemType,
			String scriptFunctionName) {
		super();
		this.scriptFunctionCount = scriptFunctionCount;
		this.workItemTypeAction = workItemTypeAction.getName();
		this.scriptWorkItemType = scriptWorkItemType.getName();
		this.scriptFunctionName = scriptFunctionName;
	}

	public int getScriptFunctionCount() {
		return scriptFunctionCount;
	}

	public void setScriptFunctionCount(int scriptFunctionCount) {
		this.scriptFunctionCount = scriptFunctionCount;
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
