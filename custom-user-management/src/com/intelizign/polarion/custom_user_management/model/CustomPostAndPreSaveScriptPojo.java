package com.intelizign.polarion.custom_user_management.model;

import java.util.List;

import com.polarion.alm.tracker.model.ITypeOpt;

public class CustomPostAndPreSaveScriptPojo {
    private int jsFileCount;
    private ITypeOpt wiTypeEnum;
    private List<String> jsFileNames;

    public CustomPostAndPreSaveScriptPojo(int jsFileCount, ITypeOpt wiTypeEnum, List<String> jsFileNames) {
    	super();
    	this.jsFileCount = jsFileCount;
        this.wiTypeEnum = wiTypeEnum;
        this.jsFileNames = jsFileNames;
    }

    public int getJsFileCount() {
        return jsFileCount;
    }

    public void setJsFileCount(int jsFileCount) {
        this.jsFileCount = jsFileCount;
    }

    public ITypeOpt getWiTypeEnum() {
        return wiTypeEnum;
    }

    public void setWiTypeEnum(ITypeOpt wiTypeEnum) {
        this.wiTypeEnum = wiTypeEnum;
    }

    public List<String> getJsFileNames() {
        return jsFileNames;
    }

    public void setJsFileNames(List<String> jsFileNames) {
        this.jsFileNames = jsFileNames;
    }

  
}
