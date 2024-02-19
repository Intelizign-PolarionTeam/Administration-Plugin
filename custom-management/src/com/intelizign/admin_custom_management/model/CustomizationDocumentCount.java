package com.intelizign.admin_custom_management.model;

public class CustomizationDocumentCount {
    private String id;
    private String name;
    private int customFieldListModuleCount;
    private int scriptDocumentCount;
    private int scriptDocumentFunctionCount;

    // Constructor
    public CustomizationDocumentCount(String id, String name, int customFieldListModuleCount,
                                          int scriptDocumentCount, int scriptDocumentFunctionCount) {
        this.id = id;
        this.name = name;
        this.customFieldListModuleCount = customFieldListModuleCount;
        this.scriptDocumentCount = scriptDocumentCount;
        this.scriptDocumentFunctionCount = scriptDocumentFunctionCount;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCustomFieldListModuleCount() {
        return customFieldListModuleCount;
    }

    public void setCustomFieldListModuleCount(int customFieldListModuleCount) {
        this.customFieldListModuleCount = customFieldListModuleCount;
    }

    public int getScriptDocumentCount() {
        return scriptDocumentCount;
    }

    public void setScriptDocumentCount(int scriptDocumentCount) {
        this.scriptDocumentCount = scriptDocumentCount;
    }

    public int getScriptDocumentFunctionCount() {
        return scriptDocumentFunctionCount;
    }

    public void setScriptDocumentFunctionCount(int scriptDocumentFunctionCount) {
        this.scriptDocumentFunctionCount = scriptDocumentFunctionCount;
    }
}

