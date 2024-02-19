package com.intelizign.admin_custom_management.model;

public class CustomizationWorkItemData {
    private String typeId;
    private String typeName;
    private int scriptCount;
    private int scriptFunctionCount;
    private int customEnumerationCount;
    private int customFieldCount;

    public CustomizationWorkItemData(String typeId, String typeName, int scriptCount, int scriptFunctionCount, int customEnumerationCount, int customFieldCount) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.scriptCount = scriptCount;
        this.scriptFunctionCount = scriptFunctionCount;
        this.customEnumerationCount = customEnumerationCount;
        this.customFieldCount = customFieldCount;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getScriptCount() {
        return scriptCount;
    }

    public void setScriptCount(int scriptCount) {
        this.scriptCount = scriptCount;
    }

    public int getScriptFunctionCount() {
        return scriptFunctionCount;
    }

    public void setScriptFunctionCount(int scriptFunctionCount) {
        this.scriptFunctionCount = scriptFunctionCount;
    }

    public int getCustomEnumerationCount() {
        return customEnumerationCount;
    }

    public void setCustomEnumerationCount(int customEnumerationCount) {
        this.customEnumerationCount = customEnumerationCount;
    }

    public int getCustomFieldCount() {
        return customFieldCount;
    }

    public void setCustomFieldCount(int customFieldCount) {
        this.customFieldCount = customFieldCount;
    }
}
