package com.intelizign.admin_custom_management.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intelizign.admin_custom_management.service.ModuleCustomizationService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IPrimitiveType;
import com.polarion.subterra.base.data.model.IType;

public class ModuleCustomizationServiceImpl implements ModuleCustomizationService {

	private static final Logger log = Logger.getLogger(WorkItemCustomizationServiceImpl.class);
	private static final String WORKFLOW_FUNCTION_KEY = "ScriptFunction";
	private static final String WORKFLOW_CONDITION_KEY = "ScriptCondition";
	private static final String MODULE_PROTOTYPE_KEY = "Module";
	private static final String SCRIPT_PARAMETER_KEY = "script";
	private static final String MODULE_TYPE_KEY = "type";
	private static final String PROJECT_ID_KEY = "projectId";

	private Map<Integer, Map<String, Object>> moduleCustomizationDetailsResponseData = new LinkedHashMap<>();

	private ITrackerService trackerService;
	private int moduleWorkflowConditionCount, moduleWorkflowFunctionCount, modulecustomfieldCount;

	public ModuleCustomizationServiceImpl(ITrackerService trackerService) {
		this.trackerService = trackerService;

	}

	//Below Method is base method to get Module Customization Count
	@Override
	public List<Map<String, Object>> getModuleCustomizationCountDetails(HttpServletRequest req,
	        HttpServletResponse resp) throws Exception {
	    try {
	        String projectId = req.getParameter(ModuleCustomizationServiceImpl.PROJECT_ID_KEY);
	        ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
	        List<ITypeOpt> moduleEnum = trackerService.getTrackerProject(projectId).getModuleTypeEnum()
	                .getAvailableOptions(MODULE_TYPE_KEY);

	        return moduleEnum.stream()
	                .map(moduleType -> {
	                    try {
	                        getModuleCustomizationCount(projectObject, moduleType);
	                        Map<String, Object> moduleCustomizationDetailsMap = new LinkedHashMap<>();
	                        moduleCustomizationDetailsMap.put("moduleType", moduleType.getId());
	                        moduleCustomizationDetailsMap.put("moduleName", moduleType.getName());
	                        moduleCustomizationDetailsMap.put("moduleCustomfieldCount", modulecustomfieldCount);
	                        moduleCustomizationDetailsMap.put("moduleWorkflowFunctionCount", moduleWorkflowFunctionCount);
	                        moduleCustomizationDetailsMap.put("moduleWorkflowConditionCount", moduleWorkflowConditionCount);
	                        return moduleCustomizationDetailsMap;
	                    } catch (Exception e) {
	                        log.error("Error while processing module customization details: " + e.getMessage());
	                        return null;
	                    }
	                })
	                .filter(map -> map != null)
	                .collect(Collectors.toList());

	    } catch (Exception e) {
	        log.error("Error while getting module customization count details: " + e.getMessage());
	        e.printStackTrace();
	        return null;
	    }
	}

	//Below Method is base method to get Module Customization Details
	@Override
	public Map<Integer, Map<String, Object>> getModuleCustomizationDetails(ITrackerProject trackerPro,
			ITypeOpt moduleTypeEnum, String heading) throws Exception {

		redirectModuleCustomization(trackerPro, moduleTypeEnum, heading);
		return moduleCustomizationDetailsResponseData;

	}

	//In UI User click the count (hyperlink) below method handle that action
	public void redirectModuleCustomization(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum, String heading)
			throws Exception {

		switch (heading) {
		case "moduleCustomfieldCount":

			getModuleCustomFieldDetails(moduleTypeEnum, trackerPro);
			break;
		case "moduleWorkflowFunctionCount":
			getModuleWorkFlowFunctionDetails(moduleTypeEnum, trackerPro);
			break;

		case "moduleWorkflowConditionCount":
			getModuleWorkFlowConditionDetails(moduleTypeEnum, trackerPro);
			break;
		default:
			break;
		}

	}


	@Override
	public void getModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum) throws Exception {

		try {
			modulecustomfieldCount = 0;
			modulecustomfieldCount = getModuleCustomFieldCount(trackerPro, moduleTypeEnum);
			getModuleWorkFlowConditionCount(trackerPro, moduleTypeEnum);

		} catch (UnresolvableObjectException e) {
			log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
	}

	//Get Module Workflow Condition Count
	@Override
	public void getModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception {
	    try {
	        moduleWorkflowConditionCount = 0;
	        IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig(MODULE_PROTOTYPE_KEY,
	                moduleTypeEnum.getId(), pro.getContextId());
	        Collection<IAction> actions = workFlowModule.getActions();
	        
	        long conditionCount = actions.stream()
	            .flatMap(action -> action.getConditions().stream())
	            .filter(conditionOperation -> WORKFLOW_CONDITION_KEY.equals(conditionOperation.getName()))
	            .flatMap(conditionOperation -> conditionOperation.getParams().entrySet().stream())
	            .filter(entry -> entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER_KEY))
	            .count();
	        
	        moduleWorkflowConditionCount = (int) conditionCount;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}


	//Get Module Workflow Function Count
	@Override
	public void getModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) throws Exception {
	    try {
	        moduleWorkflowFunctionCount = (int) actions.stream()
	            .flatMap(action -> action.getFunctions().stream())
	            .filter(functionOperation -> functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY))
	            .count();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}


	//Get Module Custom Field Count
	@Override
	public int getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
	    try {
	        ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
	        return (int) customFieldService.getCustomFields(MODULE_PROTOTYPE_KEY, pro.getContextId(), wiTypeEnum.getId())
	            .stream()
	            .count();
	    } catch (Exception e) {
	        e.printStackTrace();
	        // Handle the exception as needed
	        return 0; // Return a default value in case of an exception
	    }
	}

	/*Get Module WorkFlow Function Details like below Attributes
	 *actionId, actionName, scriptFileName
	 */
	@Override
	public void getModuleWorkFlowFunctionDetails(ITypeOpt moduleType, ITrackerProject project) throws Exception {
	    IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig(MODULE_PROTOTYPE_KEY,
	            moduleType.getId(), project.getContextId());

	    AtomicInteger id = new AtomicInteger(0);

	    workFlowModule.getActions().forEach(action -> {
	        action.getFunctions().stream()
	            .filter(functionOperation -> functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY))
	            .flatMap(functionOperation -> functionOperation.getParams().entrySet().stream())
	            .filter(entry -> entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER_KEY))
	            .forEach(entry -> {
	                moduleCustomizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
	                moduleCustomizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
	                moduleCustomizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
	                moduleCustomizationDetailsResponseData.get(id.get()).put("attachedJsFile", entry.getValue());
	                id.getAndIncrement();
	            });
	    });
	}

	
	/*Get Module WorkFlow Condition Details like below Attributes 
	 *actionId, actionName, scriptFileName
	 */
	@Override
	public void getModuleWorkFlowConditionDetails(ITypeOpt moduleType, ITrackerProject project) throws Exception {
	    IWorkflowConfig workFlowModule = trackerService.getWorkflowManager().getWorkflowConfig(MODULE_PROTOTYPE_KEY,
	            moduleType.getId(), project.getContextId());
	    AtomicInteger id = new AtomicInteger(0);

	    workFlowModule.getActions().forEach(action -> {
	        action.getConditions().forEach(conditionsOperation -> {
	            if (conditionsOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
	                conditionsOperation.getParams().entrySet().stream()
	                    .filter(entry -> entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER_KEY))
	                    .forEach(entry -> {
	                        moduleCustomizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
	                        moduleCustomizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
	                        moduleCustomizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
	                        moduleCustomizationDetailsResponseData.get(id.get()).put("attachedJsFile", entry.getValue());
	                        id.getAndIncrement();
	                    });
	            }
	        });
	    });
	}


	/*Get Module WorkFlow Custom Field Details in below Attributes 
	 *customId, customName
	 */
	public void getModuleCustomFieldDetails(ITypeOpt moduleType, ITrackerProject projectId) throws Exception {
	    AtomicInteger id = new AtomicInteger(0);
	    ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();

	    Collection<ICustomField> moduleCustomFieldList = customFieldService.getCustomFields(MODULE_PROTOTYPE_KEY,
	            projectId.getContextId(), moduleType.getId());

	    moduleCustomFieldList.stream().forEach(cust -> {
	        IType getType = cust.getType();
	        moduleCustomizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
	        moduleCustomizationDetailsResponseData.get(id.get()).put("customId", cust.getId());
	        moduleCustomizationDetailsResponseData.get(id.get()).put("customName", cust.getName());
	        if (getType instanceof IPrimitiveType) {
	            IPrimitiveType modulePrimitiveType = (IPrimitiveType) getType;
	            moduleCustomizationDetailsResponseData.get(id.get()).put("customType", modulePrimitiveType.getTypeName());
	        }
	        id.getAndIncrement();
	    });
	}

}



