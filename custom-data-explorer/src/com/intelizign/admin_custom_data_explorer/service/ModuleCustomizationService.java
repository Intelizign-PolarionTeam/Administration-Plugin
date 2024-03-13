package com.intelizign.admin_custom_data_explorer.service;

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
	
	void storeAllModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum) throws Exception;
	
	Map<Integer,Map<String, Object>>  getModuleCustomizationDetails(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum, String heading) throws Exception;

	void  storeModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception;

	void  storeModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt moduleTypeEnum) throws Exception;

	int   getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception;

	void  addModuleWorkFlowFunctionDetailsInMapObject(ITypeOpt moduleType, ITrackerProject project) throws Exception;
	
	void  addModuleWorkFlowConditionDetailsInMapObject(ITypeOpt moduleType, ITrackerProject projectId) throws Exception;
	
	void  addModuleCustomFieldDetailsInMapObject(ITypeOpt moduleType, ITrackerProject projectid) throws Exception;

}
