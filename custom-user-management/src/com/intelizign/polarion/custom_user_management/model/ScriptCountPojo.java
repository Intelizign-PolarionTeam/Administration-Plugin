package com.intelizign.polarion.custom_user_management.model;

import com.polarion.alm.tracker.model.ITypeOpt;

public class ScriptCountPojo {
	int scriptCount;
	private String wiTypeEnum;

	public ScriptCountPojo(int scriptCount, ITypeOpt wiTypeEnum) {
		super();
		this.scriptCount = scriptCount;
		this.wiTypeEnum = wiTypeEnum.getName();
	}

	public int getScriptCount() {
		return scriptCount;
	}

	public void setScriptCount(int scriptCount) {
		this.scriptCount = scriptCount;
	}

	public String getWiTypeEnum() {
		return wiTypeEnum;
	}

	public void setWiTypeEnum(ITypeOpt wiTypeEnum) {
		this.wiTypeEnum = wiTypeEnum.getName();
	}

}
