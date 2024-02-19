package com.intelizign.admin_custom_management.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.intelizign.admin_custom_management.model.CustomizationDocumentCount;
import com.intelizign.admin_custom_management.model.CustomizationWorkItemCount;
import com.intelizign.admin_custom_management.model.CustomizationWorkItemData;
import com.intelizign.admin_custom_management.service.ModuleCustomizationService;
import com.intelizign.admin_custom_management.service.WorkItemCustomizationService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IOperation;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkItemCustomizationServiceImpl implements WorkItemCustomizationService {

	private static final Logger log = Logger.getLogger(WorkItemCustomizationServiceImpl.class);
	private static final String WORKFLOW_FUNCTION_KEY = "ScriptFunction";
	private static final String WORKFLOW_CONDITION_KEY = "ScriptCondition";
	private static final String WORKITEM_PROTOTYPE = "WorkItem";
	private static final String SCRIPT_PARAMETER = "script";
	private static final String DEFAULT_REPO = "default";
	private static final String WORKITEM_TYPE = "type";

	private ITrackerService trackerService;
	private ITransactionService transactionService;
	private IRepositoryService repositoryService;
	private ModuleCustomizationService moduleCustomizationService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private int scriptCount, scriptFunctionCount, customEnumerationCount, customFieldCount;
	public WorkItemCustomizationServiceImpl(ITrackerService trackerService, ITransactionService transactionService,
			IRepositoryService repositoryService,ModuleCustomizationService moduleCustomizationService) {
		super();
		this.trackerService = trackerService;
		this.transactionService = transactionService;
		this.repositoryService = repositoryService;
		this.moduleCustomizationService = moduleCustomizationService;
	}



	public void getProjectList(HttpServletRequest req, HttpServletResponse resp) throws UnresolvableObjectException {
	    try {
	        Map<String, String> projectsObjMap = new LinkedHashMap<>();
	        IPObjectList<IProject> getProjectList = trackerService.getProjectsService().searchProjects("", "id");

	        for (IProject pro : getProjectList) {
	            try {
	                projectsObjMap.put(pro.getId(), pro.getName());
	            } catch (UnresolvableObjectException e) {
	                log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
	            } catch (Exception e) {
	                log.error("Exception occurred: " + e.getMessage());
	                continue;
	            }
	        }

	        Map<String, String> prePostHookMap = getPrePostSaveScript(resp);

	        // Create a response object
	        Map<String, Object> responseObject = new LinkedHashMap<>();
	        responseObject.put("projectsList", projectsObjMap);
	        responseObject.put("prePostHookMapObj", prePostHookMap);

	        // Serialize the response object to JSON
	        ObjectMapper objectMapper = new ObjectMapper();
	        String jsonResponse = objectMapper.writeValueAsString(responseObject);

	        resp.setContentType("application/json");
	        resp.getWriter().write(jsonResponse);
	    } catch (Exception e) {
	        log.error("Error occurred while fetching project list: " + e.getMessage());
	        e.printStackTrace();
	    }
	}


	public void getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			
			String projectId = req.getParameter("projectId");
			ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
			List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions(WORKITEM_TYPE);
		
			List<Map<String, Object>> customizationDetailsList = new ArrayList<>();

			for (ITypeOpt wiTypeEnum : workItemTypeEnum) {
	
			    getWorkItemCustomizationCount(projectObject, wiTypeEnum);
			    
			  
			    Map<String, Object> customizationDetailsMap = new LinkedHashMap<>();
			    customizationDetailsMap.put("wiType", wiTypeEnum.getId());
			    customizationDetailsMap.put("wiName", wiTypeEnum.getName());
			    customizationDetailsMap.put("scriptcount", scriptCount);
			    customizationDetailsMap.put("scriptFunctionCount", scriptFunctionCount);
			    customizationDetailsMap.put("customEnumerationCount", customEnumerationCount);
			    customizationDetailsMap.put("customFieldCount", customFieldCount);
			    
			    customizationDetailsList.add(customizationDetailsMap);
			}
			List<Map<String, Object>> moduleCustomizationDetailsList = moduleCustomizationService.getCustomizationDetails(req, resp);

			//System.out.println("Module CustomizationDetails List" + moduleCustomizationDetailsList);
			Map<String, Object> jsonResponse = new LinkedHashMap<>();
	        jsonResponse.put("customizationDetails", customizationDetailsList);
	        jsonResponse.put("moduleCustomizationDetails", moduleCustomizationDetailsList);
	        String jsonResponseString = objectMapper.writeValueAsString(jsonResponse);
			
			resp.setContentType("application/json");
			resp.getWriter().write(jsonResponseString);


		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void getWorkItemCustomizationCount(ITrackerProject trackerPro, ITypeOpt wiTypeEnum) {
		try {
			getWorkItemCustomFieldCount(trackerPro, wiTypeEnum);
			getWorkItemWorkFlowSciptCount(trackerPro, wiTypeEnum);
			getWorkItemCustomEnumerationCount(trackerPro, wiTypeEnum);
		} catch (UnresolvableObjectException e) {
			log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
	}

	public void getWorkItemCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		try {

			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
					pro.getContextId(), wiTypeEnum.getId());
			customFieldCount = customFieldList.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getWorkItemWorkFlowSciptCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
	
		try {
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(
					WorkItemCustomizationServiceImpl.WORKITEM_PROTOTYPE, wiTypeEnum.getId(), pro.getContextId());
			Collection<IAction> actions = workFlow.getActions();
			getWorkItemWorkFlowFunctionCount(actions, wiTypeEnum);
			for (IAction action : actions) {
				Set<IOperation> conditions = action.getConditions();
				for (IOperation conditionOperation : conditions) {
					if (conditionOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
						Map<String, String> conditionData = conditionOperation.getParams();
						for (Map.Entry<String, String> entry : conditionData.entrySet()) {
							if (entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER)) {
								scriptCount++;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getWorkItemWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) {
		try {
			for (IAction action : actions) {

				Set<IOperation> functions = action.getFunctions();
				for (IOperation functionOperation : functions) {
					if (functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY)) {
						scriptFunctionCount++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getWorkItemCustomEnumerationCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
		try {
			String projectLocation = pro.getLocation().getLastComponent();
			IEnumeration<ITypeOpt> wiType = trackerService.getTrackerProject(pro).getWorkItemTypeEnum();
			List<String> typeIds = wiType.getAllOptions().stream().map(ITypeOpt::getId).collect(Collectors.toList());
			transactionService.beginTx();
			ILocation config = Location.getLocationWithRepository(DEFAULT_REPO,
					"/" + projectLocation + "/.polarion/tracker/fields/");
			IRepositoryReadOnlyConnection defaultRepo = repositoryService.getReadOnlyConnection(DEFAULT_REPO);
			InputStream inputStrm = defaultRepo.getContent(config);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStrm);
			BufferedReader reader = new BufferedReader(inputStreamReader);
			try {
				String line;
				Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
				while ((line = reader.readLine()) != null) {
					java.util.regex.Matcher matcher = pattern.matcher(line);
					while (matcher.find()) {
						String extractedText = matcher.group(2);
						if (typeIds.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
								&& !extractedText.contains("custom-fields")
								&& !extractedText.contains("calculated-fields")) {
							String typeIdPrefix = extractedText.substring(0, extractedText.indexOf('-'));
							if (typeIdPrefix.equals(wiTypeEnum.getId())) {
								customEnumerationCount++;
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					reader.close();
					inputStrm.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			System.out.println("Exception is" + e.getMessage());
		} finally {
			transactionService.endTx(false);
		}
	}

	
	private Map<String, String> getPrePostSaveScript(HttpServletResponse resp) {
		try {
			Map<String, String> hookMap = new HashMap<>();
			String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
			File folder = new File(folderPath);
			if (folder.exists() && folder.isDirectory()) {
				for (File jsFile : folder.listFiles()) {
					hookMap.put(jsFile.getName(), folderPath);
				}
			} else {
				System.out.println("The specified folder does not exist or is not a directory.");
			}
			if (!(hookMap.isEmpty())) {

			}
			return hookMap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void getCustomizationDetailsPop(HttpServletRequest req, HttpServletResponse resp) {
		try {

			String type = req.getParameter("type");
			String selectedWorkItemType = req.getParameter("column");
			String projectId = req.getParameter("projectid");
			CustomizationWorkItemCount custFieldObject = new CustomizationWorkItemCount();
			ITrackerProject projectid = trackerService.getTrackerProject(projectId);
			List<ITypeOpt> moduleEnum = trackerService.getTrackerProject(projectId).getModuleTypeEnum()
					.getAvailableOptions("type");
			List<ITypeOpt> workItemTyp = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions("type");

			for (ITypeOpt work : moduleEnum) {
				if (work.getName().equalsIgnoreCase(type)) {

					switch (selectedWorkItemType.toLowerCase()) {
					case "document custom fields":
						System.out.println("work");
						documentCustomFields(work, projectid, custFieldObject);
						break;
					case "document workflow function":
						documentworkflowfunction(work, projectid, custFieldObject);
						break;
					case "document workflow condition":
						documentworkflowcondition(work, projectid, custFieldObject);
						break;
					default:
						break;
					}
				}
			}

			for (ITypeOpt work : workItemTyp) {
				if (work.getName().equalsIgnoreCase(type)) {

					switch (selectedWorkItemType.toLowerCase()) {
					case "custom fields":
						customField(work, projectid, custFieldObject);
						break;
					case "custom enumeration":
						customEnumeration(work, projectid, custFieldObject);
						break;
					case "workflow function":
						workflowFunction(work, projectid, custFieldObject);
						break;
					case "workflow condition":
						workflowCondition(work, projectid, custFieldObject);
						break;
					default:
						break;
					}
				}
			}

			custFieldObject.setType(type);
			custFieldObject.setColumn(selectedWorkItemType);
			custFieldObject.setProjectId(projectId);

			ObjectMapper objectMapper = new ObjectMapper();
			String jsonResponse = objectMapper.writeValueAsString(custFieldObject);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write(jsonResponse);
			resp.setStatus(HttpServletResponse.SC_OK);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void documentworkflowcondition(ITypeOpt work, ITrackerProject project,
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

	private void documentworkflowfunction(ITypeOpt work, ITrackerProject project,
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

	private void documentCustomFields(ITypeOpt work, ITrackerProject projectid,
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

	private void customEnumeration(ITypeOpt work, ITrackerProject project, CustomizationWorkItemCount custFieldObject) {
		try {
			String projectLocation = project.getLocation().getLastComponent();
			IEnumeration<ITypeOpt> wiType = trackerService.getTrackerProject(project.getId()).getWorkItemTypeEnum();
			List<String> typeIds = wiType.getAllOptions().stream().map(ITypeOpt::getId).collect(Collectors.toList());
			transactionService.beginTx();
			ILocation config = Location.getLocationWithRepository("default",
					"/" + projectLocation + "/.polarion/tracker/fields/");
			IRepositoryReadOnlyConnection defaultRepo = repositoryService.getReadOnlyConnection("default");
			InputStream inputStrm = defaultRepo.getContent(config);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStrm);
			BufferedReader reader = new BufferedReader(inputStreamReader);
			try {
				processEnumeration(reader, typeIds, work, custFieldObject);
			} finally {
				closeResources(reader, inputStrm);
			}
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e.getMessage());
		} finally {
			try {
				transactionService.endTx(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void processEnumeration(BufferedReader reader, List<String> typeIds, ITypeOpt work,
			CustomizationWorkItemCount custFieldObject) throws IOException {
		String line;
		Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
		while ((line = reader.readLine()) != null) {
			java.util.regex.Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				String extractedText = matcher.group(2);
				if (typeIds.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
						&& !extractedText.contains("custom-fields") && !extractedText.contains("calculated-fields")) {
					String typeIdPrefix = extractedText.substring(0, extractedText.indexOf('-'));
					if (typeIdPrefix.equals(work.getId())) {
						custFieldObject.addTypeIdPrefix(extractedText);
					}
				}
			}
		}
	}

	private void closeResources(BufferedReader reader, InputStream inputStrm) {
		try {
			if (reader != null)
				reader.close();
			if (inputStrm != null)
				inputStrm.close();
		} catch (IOException e) {
			System.out.println("Error closing resources: " + e.getMessage());
		}
	}

	private void workflowCondition(ITypeOpt work, ITrackerProject project, CustomizationWorkItemCount custFieldObject) {
		IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig("WorkItem", work.getId(),
				project.getContextId());
		Collection<IAction> actions = workFlow.getActions();
		for (IAction action : actions) {
			Set<IOperation> conditions = action.getConditions();
			for (IOperation conditionsOperation : conditions) {
				if (conditionsOperation.getName().equals("ScriptCondition")) {
					System.out.println("functionOperation.getName() " + conditionsOperation.getName());
					for (Map.Entry<String, String> entry : conditionsOperation.getParams().entrySet()) {
						if (entry.getKey().equalsIgnoreCase("script")) {
							custFieldObject.addActionScript(action.getId(), entry.getValue());
						}
					}
				}
			}
		}

	}

	private void customField(ITypeOpt work, ITrackerProject projectid, CustomizationWorkItemCount custFieldObject) {
		ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		Collection<ICustomField> customFieldList = customFieldService.getCustomFields("WorkItem",
				projectid.getContextId(), work.getId());
		for (ICustomField cust : customFieldList) {

			custFieldObject.addCustomField(cust.getId(), cust.getName());
		}

	}

	private void workflowFunction(ITypeOpt work, ITrackerProject project, CustomizationWorkItemCount custFieldObject) {

		IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig("WorkItem", work.getId(),
				project.getContextId());
		Collection<IAction> actions = workFlow.getActions();
		for (IAction action : actions) {
			Set<IOperation> functions = action.getFunctions();
			for (IOperation functionOperation : functions) {
				if (functionOperation.getName().equals("ScriptFunction")) {
					System.out.println("functionOperation.getName() " + functionOperation.getName());
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

}