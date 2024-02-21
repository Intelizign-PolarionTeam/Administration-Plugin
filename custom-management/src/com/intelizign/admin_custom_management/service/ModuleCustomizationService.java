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
	
	List<Map<String, Object>> getModuleCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp)throws Exception;
	
	void getModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum) throws Exception;
	
	Map<String, Object>  getModuleCustomizationDetails(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum, String heading) throws Exception;

	void getModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception;

	void getModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt moduleTypeEnum) throws Exception;

	int getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception;

	void getModuleWorkFlowFunctionDetails(ITypeOpt moduleType, ITrackerProject project) throws Exception;
	
	void  getModuleWorkFlowConditionDetails(ITypeOpt moduleType, ITrackerProject projectId) throws Exception;
	
	void  getModuleCustomFieldDetails(ITypeOpt moduleType, ITrackerProject projectid) throws Exception;

}
