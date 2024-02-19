package com.intelizign.admin_custom_management.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;

public interface ModuleCustomizationService {
	
	List<Map<String, Object>> getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp)throws Exception;
	
	void getModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt wiTypeEnum) throws Exception;

	void getModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception;

	void getModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) throws Exception;

	int getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception;

}
