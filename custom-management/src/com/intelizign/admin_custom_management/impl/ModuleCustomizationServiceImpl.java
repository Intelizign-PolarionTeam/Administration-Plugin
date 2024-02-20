package com.intelizign.admin_custom_management.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelizign.admin_custom_management.service.ModuleCustomizationService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IOperation;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.subterra.base.data.model.ICustomField;

public class ModuleCustomizationServiceImpl implements ModuleCustomizationService {

	private static final Logger log = Logger.getLogger(WorkItemCustomizationServiceImpl.class);
	private static final String WORKFLOW_FUNCTION_KEY = "ScriptFunction";
	private static final String WORKFLOW_CONDITION_KEY = "ScriptCondition";
	private static final String MODULE_PROTOTYPE_KEY = "Module";
	private static final String SCRIPT_PARAMETER_KEY = "script";
	private static final String MODULE_TYPE_KEY = "type";
	private static final String PROJECT_ID_KEY = "projectId";
	private final ObjectMapper objectMapper = new ObjectMapper();

	private ITrackerService trackerService;
	private int moduleWorkflowConditionCount, moduleWorkflowFunctionCount, modulecustomFieldCount;

	public ModuleCustomizationServiceImpl(ITrackerService trackerService) {
		this.trackerService = trackerService;

	}

	public List<Map<String, Object>> getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		try {

			String projectId = req.getParameter(ModuleCustomizationServiceImpl.PROJECT_ID_KEY);
			ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
			List<ITypeOpt> moduleEnum = trackerService.getTrackerProject(projectId).getModuleTypeEnum()
					.getAvailableOptions(MODULE_TYPE_KEY);
			List<Map<String, Object>> moduleCustomizationDetailsList = new ArrayList<>();
			if (!moduleCustomizationDetailsList.isEmpty()) {
				moduleCustomizationDetailsList.clear();
			}
			for (ITypeOpt moduleType : moduleEnum) {
				getModuleCustomizationCount(projectObject, moduleType);

				Map<String, Object> moduleCustomizationDetailsMap = new LinkedHashMap<>();
				moduleCustomizationDetailsMap.put("moduleType", moduleType.getId());
				moduleCustomizationDetailsMap.put("moduleCustomFieldCount", modulecustomFieldCount);
				moduleCustomizationDetailsMap.put("moduleWorkflowFunctionCount", moduleWorkflowFunctionCount);
				moduleCustomizationDetailsMap.put("moduleWorkflowConditionCount", moduleWorkflowConditionCount);

				moduleCustomizationDetailsList.add(moduleCustomizationDetailsMap);

			}
			return moduleCustomizationDetailsList;

		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
		return null;

	}

	public void getModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt wiTypeEnum) throws Exception {

		try {
			modulecustomFieldCount = 0;
			modulecustomFieldCount = getModuleCustomFieldCount(trackerPro, wiTypeEnum);
			getModuleWorkFlowConditionCount(trackerPro, wiTypeEnum);

		} catch (UnresolvableObjectException e) {
			log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
	}

	public void getModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {

		try {
			moduleWorkflowConditionCount = 0;
			IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig(MODULE_PROTOTYPE_KEY,
					wiTypeEnum.getId(), pro.getContextId());
			Collection<IAction> actions = workFlowModule.getActions();
			getModuleWorkFlowFunctionCount(actions, wiTypeEnum);
			for (IAction action : actions) {
				Set<IOperation> conditions = action.getConditions();
				for (IOperation conditionOperation : conditions) {
					if (conditionOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
						Map<String, String> conditionData = conditionOperation.getParams();
						for (Map.Entry<String, String> entry : conditionData.entrySet()) {
							if (entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER_KEY)) {
								moduleWorkflowConditionCount++;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) throws Exception {

		try {
			moduleWorkflowFunctionCount = 0;
			for (IAction action : actions) {

				Set<IOperation> functions = action.getFunctions();
				for (IOperation functionOperation : functions) {
					if (functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY)) {
						moduleWorkflowFunctionCount++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public int getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
		Collection<ICustomField> moduleCustomFieldList = null;
		try {
			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			moduleCustomFieldList = customFieldService.getCustomFields(MODULE_PROTOTYPE_KEY, pro.getContextId(),
					wiTypeEnum.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return moduleCustomFieldList.size();

	}

	public void documentworkflowcondition(ITypeOpt work, ITrackerProject project,
			CustomizationWorkItemCount custFieldObject) {
		IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig("Module", work.getId(),
				project.getContextId());
		Collection<IAction> actions = workFlowModule.getActions();
		for (IAction action : actions) {
			Set<IOperation> conditions = action.getConditions();
			for (IOperation conditionsOperation : conditions) {
				if (conditionsOperation.getName().equals("ScriptCondition")) {
					System.out.println("functionOperation.getName() " + conditionsOperation.getName());
					for (Map.Entry<String, String> entry : conditionsOperation.getParams().entrySet()) {
						if (entry.getKey().equalsIgnoreCase("script")) {
							System.out.println("entry " + entry.getValue());
							custFieldObject.addActionScript(action.getId(), entry.getValue());
						}
					}
				}
			}
		}

	}

	public void documentworkflowfunction(ITypeOpt work, ITrackerProject project,
			CustomizationWorkItemCount custFieldObject) {
		IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig("Module", work.getId(),
				project.getContextId());
		Collection<IAction> actions = workFlowModule.getActions();
		for (IAction action : actions) {
			Set<IOperation> functions = action.getFunctions();
			for (IOperation functionOperation : functions) {
				if (functionOperation.getName().equals("ScriptFunction")) {
					// System.out.println("functionOperation.getName()
					// "+functionOperation.getName());
					for (Map.Entry<String, String> entry : functionOperation.getParams().entrySet()) {
						if (entry.getKey().equalsIgnoreCase("script")) {
							System.out.println("action " + action.getId() + "," + entry.getValue());
							custFieldObject.addActionScript(action.getId(), entry.getValue());
						}
					}
				}
			}
		}

	}

	public void documentCustomFields(ITypeOpt work, ITrackerProject projectid,
			CustomizationWorkItemCount custFieldObject) {
		ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		System.out.println("documentCustomFields ");
		Collection<ICustomField> moduleCustomFieldList = customFieldService.getCustomFields("Module",
				projectid.getContextId(), work.getId());
		for (ICustomField cust : moduleCustomFieldList) {
			custFieldObject.addCustomField(cust.getId(), cust.getName());
			System.out.println("cust.getId(), cust.getName() " + cust.getId() + " " + cust.getName());
		}

	}

}
