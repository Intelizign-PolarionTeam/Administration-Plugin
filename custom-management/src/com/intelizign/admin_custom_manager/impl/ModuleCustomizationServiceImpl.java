package com.intelizign.admin_custom_manager.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intelizign.admin_custom_manager.service.ModuleCustomizationService;
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

	/**
	 * Retrieves  count Details related  to Module Customization
	 * 
	 * @param req  The HttpServletRequest object containing request parameters.
	 * @param resp The HttpServletResponse object used to send the response.
	 */
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
	                    	storeAllModuleCustomizationCount(projectObject, moduleType);
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

	/**
	 * Retrieves  module Details related  to all Module Customization
	 * 
	 * @param trackerPro  - The Tracker Project Object.
	 * @param moduleTypeEnum - The Module Type Enum.
	 * @param heading
	 */ 
	@Override
	public Map<Integer, Map<String, Object>> getModuleCustomizationDetails(ITrackerProject trackerPro,
			ITypeOpt moduleTypeEnum, String heading) throws Exception {

		redirectModuleCustomization(trackerPro, moduleTypeEnum, heading);
		return moduleCustomizationDetailsResponseData;

	}

	/**
	 * Redirects to the appropriate method based on the heading provided. From
	 * Frontend when user trigger the count respective heading passed
	 *
	 * @param heading      The heading indicating the type of customization.
	 * @param moduleTypeEnum The module enum Object.
	 * @param trackerPro   The tracker project.
	 * @throws Exception If an error occurs during the customization processing.
	 */
	public void redirectModuleCustomization(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum, String heading)
			throws Exception {

		switch (heading) {
		case "moduleCustomfieldCount":

			addModuleCustomFieldDetailsInMapObject(moduleTypeEnum, trackerPro);
			break;
		case "moduleWorkflowFunctionCount":
			addModuleWorkFlowFunctionDetailsInMapObject(moduleTypeEnum, trackerPro);
			break;

		case "moduleWorkflowConditionCount":
			addModuleWorkFlowConditionDetailsInMapObject(moduleTypeEnum, trackerPro);
			break;
		default:
			break;
		}

	}

	/**
	 * Retrieves all customization count related to module
	 * 
	 * @param pro        -The tracker project object
	 * @param moduleTypeEnum - The module enum object
	 */
	@Override
	public void storeAllModuleCustomizationCount(ITrackerProject trackerPro, ITypeOpt moduleTypeEnum) throws Exception {

		try {
			modulecustomfieldCount = 0;
			modulecustomfieldCount = getModuleCustomFieldCount(trackerPro, moduleTypeEnum);
			storeModuleWorkFlowConditionCount(trackerPro, moduleTypeEnum);

		} catch (UnresolvableObjectException e) {
			log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
	}

	/**
	 * Retrieves workflow condition count with respective module type stored in
	 * moduleWorkflowConditionCount variable
	 * 
	 * @param pro        -The tracker project object
	 * @param moduleTypeEnum - The module enum object
	 */
	@Override
	public void storeModuleWorkFlowConditionCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception {
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


	/**
	 * Retrieves workflow function count with respective module type stored in
	 * moduleWorkflowFunctionCount variable
	 * 
	 * @param actions    -collection of IAction object related to workitem workflow
	 * @param moduleTypeEnum - The module enum object
	 */
	@Override
	public void storeModuleWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt moduleTypeEnum) throws Exception {
	    try {
	        moduleWorkflowFunctionCount = (int) actions.stream()
	            .flatMap(action -> action.getFunctions().stream())
	            .filter(functionOperation -> functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY))
	            .count();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}



	/**
	 * Retrieves customField count with respective module type stored in
	 * moduleWorkflowConditionCount variable
	 * 
	 * @param pro        -The tracker project object
	 * @param moduleTypeEnum - The module type enum object
	 */
	@Override
	public int getModuleCustomFieldCount(ITrackerProject pro, ITypeOpt moduleTypeEnum) throws Exception {
	    try {
	        ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
	        return (int) customFieldService.getCustomFields(MODULE_PROTOTYPE_KEY, pro.getContextId(), moduleTypeEnum.getId())
	            .stream()
	            .count();
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0; 
	    }
	}

	/**
	 * Retrieves and maps module workflow Function details associated with a module
	 *  type and project. Attributes actionId, actionName, attachedJsFile
	 * 
	 * @param moduleType  -The module type
	 * @param project -The tracker project object
	 */
	@Override
	public void addModuleWorkFlowFunctionDetailsInMapObject(ITypeOpt moduleType, ITrackerProject project) throws Exception {
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

	
	/**
	 * Retrieves and maps module workflow condition details associated with a module
	 * type and project. Attributes actionId, actionName, attachedJsFile
	 * 
	 * @param moduleType  -The module type
	 * @param project -The tracker project object
	 */
	@Override
	public void addModuleWorkFlowConditionDetailsInMapObject(ITypeOpt moduleType, ITrackerProject project) throws Exception {
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


	/**
	 * Retrieves and maps module customField details associated with a module
	 * type and project. Attributes customId, customName, customType
	 * 
	 * @param moduleType  -The module type
	 * @param project -The tracker project object
	 */
	public void addModuleCustomFieldDetailsInMapObject(ITypeOpt moduleType, ITrackerProject projectId) throws Exception {
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



