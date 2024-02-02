package com.intelizign.polarion.custom_user_management.model;


public class CustomPostAndPreSaveScriptPojo {
	private String folderPath;
	private String jsFileNames;

	// Constructor
	public CustomPostAndPreSaveScriptPojo( String folderPath, String jsFileNames) {
		this.folderPath = folderPath;
		this.jsFileNames = jsFileNames;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public String getJsFileNames() {
		return jsFileNames;
	}

	public void setJsFileNames(String jsFileNames) {
		this.jsFileNames = jsFileNames;
	}
}
