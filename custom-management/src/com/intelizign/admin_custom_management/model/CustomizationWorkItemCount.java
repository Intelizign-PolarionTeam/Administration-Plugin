package com.intelizign.admin_custom_management.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomizationWorkItemCount {
    private String type;
    private String column;
    private String projectId;
    private List<String> customFieldIds;
    private List<String> customFieldNames;
    private Map<String, String> actionScripts = new HashMap<>();
    private List<String> typeIdPrefixList;
    
    private List<String> customIds;
    private List<String> customNames;
    private Map<String, String> customFields;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<String> getCustomFieldIds() {
        return customFieldIds;
    }

    public void setCustomFieldIds(List<String> customFieldIds) {
        this.customFieldIds = customFieldIds;
    }

    public List<String> getCustomFieldNames() {
        return customFieldNames;
    }

    public void setCustomFieldNames(List<String> customFieldNames) {
        this.customFieldNames = customFieldNames;
    }

    
    public void addActionScript(String actionId, String scriptValue) {
        actionScripts.put(actionId, scriptValue);
    }

    // Getter for the actionScripts map
    public Map<String, String> getActionScripts() {
        return actionScripts;
    }

    

 // Constructor
    public CustomizationWorkItemCount() {
        typeIdPrefixList = new ArrayList<>(); 
        customFields = new HashMap<>();
    }

    // Other methods
    public void addTypeIdPrefix(String typeIdPrefix) {
        typeIdPrefixList.add(typeIdPrefix);
    }

    // Getters and setters
    public List<String> getTypeIdPrefixList() {
        return typeIdPrefixList;
    }

    public void setTypeIdPrefixList(List<String> typeIdPrefixList) {
        this.typeIdPrefixList = typeIdPrefixList;
    }

	
    
    public void addCustomField(String id, String name) {
        customFields.put(id, name);
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }
    
    
	
	
    
}

