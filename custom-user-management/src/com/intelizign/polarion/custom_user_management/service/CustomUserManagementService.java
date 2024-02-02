package com.intelizign.polarion.custom_user_management.service;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.intelizign.polarion.custom_user_management.model.CustomDetailsPojo;
import com.intelizign.polarion.custom_user_management.model.CustomEnumerationPojo;
import com.intelizign.polarion.custom_user_management.model.CustomPostAndPreSaveScriptPojo;
import com.intelizign.polarion.custom_user_management.model.ScriptConditionPojo;
import com.intelizign.polarion.custom_user_management.model.ScriptFunctionPojo;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IOperation;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IEnumObjectFactory;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.data.model.ICustomField;

public class CustomUserManagementService {

	private static final ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform()
			.lookupService(ITrackerService.class);
	private static final Logger log = Logger.getLogger(CustomUserManagementService.class);
	// private static final IWorkflowObject workflowObj = (IWorkflowObject)
	// PlatformContext.getPlatform()
	// .lookupService(IWorkflowObject.class);
	private List<CustomDetailsPojo> customDetailsList = new ArrayList<>();
	
	private List<CustomPostAndPreSaveScriptPojo> customPostAndPreSaveScriptList = new ArrayList<>();
	private List<ScriptConditionPojo> scriptConditionList = new ArrayList<>();
	private List<ScriptFunctionPojo> scriptFunctionList = new ArrayList<>();
	private Set<CustomEnumerationPojo> customEnumerationList = new HashSet<>();
	
	private Gson gson = new Gson();
	private Map<String, String> projectMap = new HashMap<String, String>();
	private Map<String, String> prepostscriptMap = new HashMap<String, String>();
	private Map<String, Object> responseData = new HashMap<>();

	public CustomUserManagementService() {

	}

	// Get all Project From the server
	public void getProjectList(HttpServletRequest req, HttpServletResponse resp) throws UnresolvableObjectException {
	    try {
	        if (!(customPostAndPreSaveScriptList.isEmpty())) {
	            customPostAndPreSaveScriptList.clear();
	        }

	        PrintWriter out = resp.getWriter();
	        IPObjectList<IProject> getProjectList = trackerService.getProjectsService().searchProjects("", "id");

	        for (IProject pro : getProjectList) {
	            try {
	                projectMap.put(pro.getId(), pro.getName());
	            } catch (UnresolvableObjectException e) {
	                log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
	            } catch (Exception e) {
	                log.error("Exception is" + e.getMessage());
	                continue;
	            }
	        }

	        getPreAndPostSaveScript(responseData);
	        responseData.put("projectMap", projectMap);
	        String projectResponsObj = gson.toJson(responseData);
	        out.println(projectResponsObj);

	    } catch (Exception e) {
	        System.out.println("Error Message is" + e.getMessage());
	        e.printStackTrace();
	    }
	}

	// Get Customization Details From the respective Project
	public void getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp)
			throws UnresolvableObjectException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String projectId = req.getParameter("projectId");
			ITrackerProject trackerPro = trackerService.getTrackerProject(projectId);
			if (!(customDetailsList.isEmpty())) {
				customDetailsList.clear();
			}
			if (!(scriptConditionList.isEmpty())) {
				scriptConditionList.clear();
			}
			if (!(scriptFunctionList.isEmpty())) {
				scriptFunctionList.clear();
			}
			if (!(customEnumerationList.isEmpty())) {
				customEnumerationList.clear();
			}
			
			List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions("type");
			for (ITypeOpt wiTypeEnum : workItemTypeEnum) {
				try {

					getCustomFieldDetails(trackerPro, wiTypeEnum);
					getCustomWorkFlowScript(trackerPro, wiTypeEnum);
					getCustomEnumerationDetails(trackerPro, wiTypeEnum);
					
					// getPreAndPostSaveScript(trackerPro,wiTypeEnum);
				} catch (UnresolvableObjectException e) {
					log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
				} catch (Exception e) {
					log.error("Exception is" + e.getMessage());
					continue;
				}
			}
			String customDetailsJson = objectMapper.writeValueAsString(customDetailsList);
			System.out.println("Script Condition List is" + scriptConditionList + "\n");
			String scriptConditionJson = objectMapper.writeValueAsString(scriptConditionList);
			String scriptFunctionJson = objectMapper.writeValueAsString(scriptFunctionList);
			String customEnumerationJson = objectMapper.writeValueAsString(customEnumerationList);
			String customPostAndPreSaveScriptJson = objectMapper.writeValueAsString(customPostAndPreSaveScriptList);
			
			
			resp.setContentType("application/json");
			resp.getWriter()
	        .write("{\"customDetailsList\":" + customDetailsJson + ",\"scriptConditionList\":"
	                + scriptConditionJson + ",\"scriptFunctionList\":" + scriptFunctionJson
	                + ",\"customEnumerationList\":" + customEnumerationJson + ",\"customPostAndPreSaveScriptList\":"
	                + customPostAndPreSaveScriptJson + "}");
		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void getPreAndPostSaveScript(Map<String, Object> responseData) {
	    try {
	        String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
	        File folder = new File(folderPath);

	        Map<String, String> scriptMap = new HashMap<>();

	        if (folder.exists() && folder.isDirectory()) {
	            for (File jsFile : folder.listFiles()) {
	                // Assuming jsFile.getName() represents the JS filename
	                // and folderPath represents the folder path
	                scriptMap.put(jsFile.getName(), folderPath);
	            }
	        } else {
	            System.out.println("The specified folder does not exist or is not a directory.");
	        }

	        // Add the scriptMap to the responseData or use it as needed
	        responseData.put("scriptMap", scriptMap);

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	
	

	// Get custom Field Count with Respective WorkItem Type
	public void getCustomFieldDetails(ITrackerProject pro, ITypeOpt wiTypeEnum) throws UnresolvableObjectException {
		try {
			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields("WorkItem",
					pro.getContextId(), wiTypeEnum.getId());
			
			int customFieldsCount = customFieldList.size();
			customDetailsList.add(new CustomDetailsPojo(customFieldsCount, wiTypeEnum));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get customScriptCondition Count with Respective WorkItem Type
	public void getCustomWorkFlowScript(ITrackerProject pro, ITypeOpt wiTypeEnum) throws UnresolvableObjectException {
		try {
			Map<String, Integer> conditionScriptCountMap = new HashMap<>();
			int scriptCount = 0;
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig("WorkItem",
					wiTypeEnum.getId(), pro.getContextId());
			Collection<IAction> actions = workFlow.getActions();

			for (IAction action : actions) {
System.out.println("action "+action.getId());

				Set<IOperation> conditions = action.getConditions();
				getCustomWorkFlowScriptFunction(action, wiTypeEnum);
				for (IOperation conditionOperation : conditions) {

					Map<String, String> conditionData = conditionOperation.getParams();
					System.out.println("conditionData "+conditionData);
					for (Map.Entry<String, String> entry : conditionData.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("script")) {

							scriptCount++;
							  conditionScriptCountMap.put(wiTypeEnum.getId(), conditionScriptCountMap.getOrDefault(entry.getValue(), 0) + 1);   
							scriptConditionList
									.add(new ScriptConditionPojo(scriptCount, action, wiTypeEnum, entry.getValue()));
						}
					}
				}

			}
ArrayList<Integer> wiTypeCountsList = new ArrayList<>(conditionScriptCountMap.values());
			
			System.out.println("wiTypeCountMap "+conditionScriptCountMap);
			int wiTypeEnumerationCount = conditionScriptCountMap.values().iterator().next();

			System.out.println("conditionScriptCountMap: " + wiTypeEnumerationCount);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get CustomScript FunctionCount with Respective WorkItmType
	public void getCustomWorkFlowScriptFunction(IAction action, ITypeOpt wiTypeEnum)
			throws UnresolvableObjectException {
		try {
			int scriptFunctionCount = 0;
			Set<IOperation> functions = action.getFunctions();
			System.out.println("witype:"+wiTypeEnum.getId());
			for (IOperation functionOperation : functions) {
				for (Map.Entry<String, String> entry : functionOperation.getParams().entrySet()) {
					if (entry.getKey().equalsIgnoreCase("script")) {
						scriptFunctionCount++;
						System.out.println("script function name "+entry.getValue());
						scriptFunctionList.add(new ScriptFunctionPojo(scriptFunctionCount, action, wiTypeEnum, entry.getValue()));
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get customEnumerationCount with Respective WorkItem Type
	public void getCustomEnumerationDetails(ITrackerProject pro, ITypeOpt wiTypeEnum)
			throws UnresolvableObjectException {
		try {
			int customEnumerationCount = 0;
			Map<String, IEnumObjectFactory> enumFactory = trackerService.getDataService().getEnumerationObjectFactories();

			for (Map.Entry<String, IEnumObjectFactory> entry : enumFactory.entrySet()) {
			    String key = entry.getKey();
			    IEnumObjectFactory value = entry.getValue();
			    List<IEnumeration> Enum= value.getEnumeration("stakeholderRequirement", pro.getContextId()).getAllOptions();
			System.out.println("Enum "+Enum);
			  
			  
			    // Now you can use 'key' and 'value' as needed
			    System.out.println("Key: " + key + ", Value: " + value);
			}

			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields("WorkItem",
					pro.getContextId(), wiTypeEnum.getId());
			for (ICustomField custom : customFieldList) {
				//System.out.println("Custom Enum type" + custom.getType() +"custom Name is"+ custom.getName());
				if (custom.getType().toString().contains("enumId")) {
					customEnumerationCount++;
				
				}

			}
			customEnumerationList.add(new CustomEnumerationPojo(customEnumerationCount, wiTypeEnum));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}