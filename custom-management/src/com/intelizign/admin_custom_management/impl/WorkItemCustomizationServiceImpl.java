package com.intelizign.admin_custom_management.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelizign.admin_custom_management.service.ModuleCustomizationService;
import com.intelizign.admin_custom_management.service.WorkItemCustomizationService;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IOperation;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IPrimitiveType;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;


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
	public Map<Integer, Map<String, Object>> customizationDetailsResponseData = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> liveReportDetailsResponseMap = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> pluginDetailsMap  = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> prePostSaveScriptMap  = new LinkedHashMap<>();
	public  Map<Integer, Map<String, Object>> getVersionDetails  = new LinkedHashMap<>(); 
	private final ObjectMapper objectMapper = new ObjectMapper();

	private int wiWorkflowScriptConditionCount, wiWorkflowScriptFunctionCount, customEnumerationCount,
			wiCustomFieldCount;

	public WorkItemCustomizationServiceImpl(ITrackerService trackerService, ITransactionService transactionService,
			IRepositoryService repositoryService, ModuleCustomizationService moduleCustomizationService) {
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

	public void getCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {

			String projectId = req.getParameter("projectId");
			ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
			List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions(WORKITEM_TYPE);
			if(!liveReportDetailsResponseMap.isEmpty()) {
				liveReportDetailsResponseMap.clear();
			}  
			if(!pluginDetailsMap.isEmpty()) {
				pluginDetailsMap.clear();
			} 
			if(!prePostSaveScriptMap.isEmpty()) {
				prePostSaveScriptMap.clear();
			}
			
			if(!getVersionDetails.isEmpty()) {
				getVersionDetails.clear();
			}
			List<Map<String, Object>> customizationCountDetailsList = new ArrayList<>();

			for (ITypeOpt wiTypeEnum : workItemTypeEnum) {

				getWorkItemCustomizationCount(projectObject, wiTypeEnum);

				Map<String, Object> customizationCountDetailsMap = new LinkedHashMap<>();
				customizationCountDetailsMap.put("wiType", wiTypeEnum.getId());
				customizationCountDetailsMap.put("wiName", wiTypeEnum.getName());
				customizationCountDetailsMap.put("wiWorkflowScriptConditionCount", wiWorkflowScriptConditionCount);
				customizationCountDetailsMap.put("wiWorkflowScriptFunctionCount", wiWorkflowScriptFunctionCount);
				customizationCountDetailsMap.put("customEnumerationCount", customEnumerationCount);
				customizationCountDetailsMap.put("wiCustomFieldCount", wiCustomFieldCount);

				customizationCountDetailsList.add(customizationCountDetailsMap);
			}
			
			List<Map<String, Object>> moduleCustomizationCountDetailsList = moduleCustomizationService
					.getModuleCustomizationCountDetails(req, resp);
			
			getLiveReportDetails(req,resp);
			getPluginDetails(req,resp);
            getprePostSaveScript(req,resp);
            getVersionDetails(req,resp);
            
			Map<String, Object> jsonResponse = new LinkedHashMap<>();
			jsonResponse.put("customizationCountDetails", customizationCountDetailsList);
			jsonResponse.put("moduleCustomizationDetails", moduleCustomizationCountDetailsList);
			jsonResponse.put("liveReportDetailsResponse", liveReportDetailsResponseMap);
			jsonResponse.put("pluginDetailsMap", pluginDetailsMap);
			jsonResponse.put("prePostSaveScriptMap", prePostSaveScriptMap);
			jsonResponse.put("getVersionDetails", getVersionDetails);
			
			String jsonResponseString = objectMapper.writeValueAsString(jsonResponse);

			resp.setContentType("application/json");
			resp.getWriter().write(jsonResponseString);

		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
	}

	 private void getVersionDetails(HttpServletRequest req, HttpServletResponse resp) {
			try {
				
				  String folderPath = System.getProperty("com.polarion.home") + "/../polarion/license/";
				  File folder = new File(folderPath);
			        if (folder.exists() && folder.isDirectory()) {
			            File licFile = new File(folder, "polarion.lic");
			            if (licFile.exists() && licFile.isFile()) {
			            	AtomicInteger id = new AtomicInteger(0);
				            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				            NodeList nodeList = factory.newDocumentBuilder().parse(licFile).getDocumentElement().getChildNodes();
				            for (int i = 0; i < nodeList.getLength(); i++) {
				                Node node = nodeList.item(i);
				                if (node.getNodeType() == Node.ELEMENT_NODE) {
				                    String nodeName = node.getNodeName();
				                    String nodeValue = node.getTextContent().trim();
				                    getVersionDetails.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
				                    switch (nodeName) {
				                        case "licenseType":	      
				                 	        getVersionDetails.get(id.get()).put("licenseType", nodeValue);
				                 	       id.getAndIncrement();
				                            break;
				                        case "userCompany":
				                        	getVersionDetails.get(id.get()).put("userCompany", nodeValue);
				                        	id.getAndIncrement();
				                            break;
				                        case "userEmail":
				                        	getVersionDetails.get(id.get()).put("userEmail", nodeValue);
				                        
				                        	id.getAndIncrement();
				                            break;
				                        case "userName":
				                        	getVersionDetails.get(id.get()).put("userName", nodeValue);
				                        	id.getAndIncrement();
				                            break;
				                            
				                           
				                    }
				                    
				                }
				            }
				            
			            }		      
			        }			
			} catch (Exception e) {
				e.printStackTrace();
			}
		 
	    }

	private void getprePostSaveScript(HttpServletRequest req, HttpServletResponse resp) {
	    String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
        File folder = new File(folderPath);
try {
	 AtomicInteger id = new AtomicInteger(0);
        if (folder.exists() && folder.isDirectory()) {
            for (File jsFile : folder.listFiles()) {
                String content = readFileContent(jsFile);          
                Map<String, Object> scriptData = new HashMap<>();
                scriptData.put("jsName", jsFile.getName());             
                String functionNames = "";
                if (content.contains("function")) {               
                    functionNames = extractFunctionNames(content);                   
                } else {	                    
                    functionNames = "No functions found in the file.";
                }
                prePostSaveScriptMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
                prePostSaveScriptMap.get(id.get()).put("Name", jsFile.getName());
                prePostSaveScriptMap.get(id.get()).put("Extension", functionNames);
                id.getAndIncrement();
            }
        } else {
            System.out.println("The specified folder does not exist or is not a directory.");
        }	        
    } catch (Exception e) {
        e.printStackTrace();
    }
	}

	 private String extractFunctionNames(String content) {
	        StringBuilder functions = new StringBuilder();
	        Pattern pattern = Pattern.compile("function\\s+([\\w\\d_]+)\\s*\\(");
	        java.util.regex.Matcher matcher = pattern.matcher(content);
	        while (matcher.find()) {
	            functions.append(matcher.group(1)).append(", ");
	        }
	        if (functions.length() > 0) {
	            functions.setLength(functions.length() - 2); 
	            return functions.toString();
	        } else {
	            return "No functions found in the file.";
	        }
	    }

	private String readFileContent(File file) {
	    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
	        StringBuilder contentBuilder = new StringBuilder();
	        String line;
	        while ((line = br.readLine()) != null) {
	            contentBuilder.append(line).append("\n");
	        }
	        return contentBuilder.toString();
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}


	@SuppressWarnings("unchecked")
	public void getPluginDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		
		
		String path=System.getProperty("com.polarion.home") + "/extensions/";
		 File directory = new File(path);
		 AtomicInteger id = new AtomicInteger(0);
	        if (directory.exists() && directory.isDirectory()) {
	            File[] filesAndDirectories = directory.listFiles();
	            for (File fileOrDirectory : filesAndDirectories) {
	            	 if (fileOrDirectory.isDirectory()) {
	            	    JSONObject pluginDetails=new JSONObject();
	            		pluginDetails.put("pluginDeatils", fileOrDirectory.getName());
	            		pluginDetails.put("pluginPath", fileOrDirectory.getPath());
	            		pluginDetailsMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
	            		pluginDetailsMap.get(id.get()).put("pluginDeatils", fileOrDirectory.getName());
	            		pluginDetailsMap.get(id.get()).put("pluginPath", fileOrDirectory.getPath());
						id.getAndIncrement();
	            	 }
	            }
	           
				
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

	private void getWorkItemCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		try {
			wiCustomFieldCount = 0;
			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
					pro.getContextId(), wiTypeEnum.getId());
			wiCustomFieldCount = customFieldList.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getWorkItemWorkFlowSciptCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {

		try {
			wiWorkflowScriptConditionCount = 0;
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
								wiWorkflowScriptConditionCount++;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getWorkItemWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) {
		try {
			wiWorkflowScriptFunctionCount = 0;
			for (IAction action : actions) {

				Set<IOperation> functions = action.getFunctions();
				for (IOperation functionOperation : functions) {
					if (functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY)) {
						wiWorkflowScriptFunctionCount++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void getWorkItemCustomEnumerationCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
		try {
			customEnumerationCount = 0;
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

	public void getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String type = req.getParameter("type");
			String heading = req.getParameter("heading");
			String projectId = req.getParameter("projectId");

			if (!customizationDetailsResponseData.isEmpty()) {
				customizationDetailsResponseData.clear();
			}
			if (type != null && heading != null && projectId != null) {
				ITrackerProject trackerPro = trackerService.getTrackerProject(projectId);
				List<ITypeOpt> moduleTypeEnum = trackerPro.getModuleTypeEnum().getAvailableOptions(WORKITEM_TYPE);
				List<ITypeOpt> workItemTypeEnum = trackerPro.getWorkItemTypeEnum().getAvailableOptions(WORKITEM_TYPE);
				for (ITypeOpt wiType : workItemTypeEnum) {
					if (wiType.getId().equalsIgnoreCase(type)) {
						redirectWorkItemCustomization(heading, wiType, trackerPro);
					}
				}

				for (ITypeOpt moduleType : moduleTypeEnum) {
					if (moduleType.getId().equalsIgnoreCase(type)) {
						customizationDetailsResponseData = moduleCustomizationService
								.getModuleCustomizationDetails(trackerPro, moduleType, heading);
					}
				}

			} else {
				log.error("Passing Data is Not Acceptable");
			}
			System.out.println("customizationDetailsResponseData" + customizationDetailsResponseData + "\n");
			Map<String, Object> jsonResponse = new LinkedHashMap<>();
			jsonResponse.put("customizationDetailsResponseData", customizationDetailsResponseData);

			String customizationDetailsResponseJson = objectMapper.writeValueAsString(jsonResponse);

			resp.setContentType("application/json");
			resp.getWriter().write(customizationDetailsResponseJson);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void redirectWorkItemCustomization(String heading, ITypeOpt workItemType, ITrackerProject trackerPro) {
		switch (heading) {
		case "wiCustomFieldCount":
			getWorkItemCustomFieldDetails(workItemType, trackerPro);
			break;
		case "customEnumerationCount":
			getcustomEnumerationDetails(workItemType, trackerPro);
			break;
		case "wiWorkflowScriptFunctionCount":
			getWorkItemWorkFlowFunctionDetails(workItemType, trackerPro);
			break;
		case "wiWorkflowScriptConditionCount":
			getWorkItemWorkFlowConditionDetails(workItemType, trackerPro);
			break;
		default:
			break;
		}
	}

	private void getcustomEnumerationDetails(ITypeOpt wiType, ITrackerProject project) {
		try {
			String projectLocation = project.getLocation().getLastComponent();
			IEnumeration<ITypeOpt> wiEnumObj = project.getWorkItemTypeEnum();
			List<String> wiTypeList = wiEnumObj.getAllOptions().stream().map(ITypeOpt::getId)
					.collect(Collectors.toList());
			transactionService.beginTx();
			ILocation config = Location.getLocationWithRepository(DEFAULT_REPO,
					"/" + projectLocation + "/.polarion/tracker/fields/");
			IRepositoryReadOnlyConnection defaultRepo = repositoryService.getReadOnlyConnection(DEFAULT_REPO);
			InputStream inputStrm = defaultRepo.getContent(config);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStrm);
			BufferedReader readEnumerationXml = new BufferedReader(inputStreamReader);
			try {
				processEnumeration(readEnumerationXml, wiTypeList, wiType);
			} finally {
				closeResources(readEnumerationXml, inputStrm);
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

	private void processEnumeration(BufferedReader readEnumerationXml, List<String> wiTypeList, ITypeOpt wiType)
			throws IOException {
		String line;
		AtomicInteger id = new AtomicInteger(0);
		Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
		while ((line = readEnumerationXml.readLine()) != null) {
			java.util.regex.Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				String extractedText = matcher.group(2);
				if (wiTypeList.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
						&& !extractedText.contains("custom-fields") && !extractedText.contains("calculated-fields")) {
					String wiTypePrefix = extractedText.substring(0, extractedText.indexOf('-'));
					if (wiTypePrefix.equals(wiType.getId())) {
						customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
						customizationDetailsResponseData.get(id.get()).put("customEnumeration", extractedText);
						id.getAndIncrement();
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

	private void getWorkItemWorkFlowConditionDetails(ITypeOpt wiType, ITrackerProject project) {
		try {
			AtomicInteger id = new AtomicInteger(0);
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(WORKITEM_PROTOTYPE,
					wiType.getId(), project.getContextId());
			Collection<IAction> actions = workFlow.getActions();
			for (IAction action : actions) {
				Set<IOperation> conditions = action.getConditions();
				for (IOperation conditionsOperation : conditions) {
					if (conditionsOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
						for (Map.Entry<String, String> entry : conditionsOperation.getParams().entrySet()) {
							if (entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER)) {
								customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
								customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
								customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
								customizationDetailsResponseData.get(id.get()).put("attachedJsFile", entry.getValue());
								id.getAndIncrement();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error while Fetching Data in WorkItemWorkflow Condition" + e.getMessage());
		}
	}

	private void getWorkItemCustomFieldDetails(ITypeOpt wiType, ITrackerProject projectId) {
		AtomicInteger id = new AtomicInteger(0);
		ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
				projectId.getContextId(), wiType.getId());
		for (ICustomField cust : customFieldList) {
			IType getType = cust.getType();
			customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
			customizationDetailsResponseData.get(id.get()).put("customId", cust.getId());
			customizationDetailsResponseData.get(id.get()).put("customName", cust.getName());
			System.out.println("Get Type Is" + getType.getClass().getName() + "object is" + getType + "\n");
			 if(getType instanceof IPrimitiveType) {
	        	   IPrimitiveType modulePrimitiveType = (IPrimitiveType)getType;
	        	   customizationDetailsResponseData.get(id.get()).put("customType", modulePrimitiveType.getTypeName());
				}
			id.getAndIncrement();
		}
		System.out.println("customization Details Response Data" + customizationDetailsResponseData + "\n");
	}

	public void getLiveReportDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    String projectId = req.getParameter("projectId");
	    List<IFolder> spaces = trackerService.getFolderManager().getFolders(projectId);
	    AtomicInteger id = new AtomicInteger(0);

	    for (IFolder space : spaces) {
	        Collection<IRichPage> liveReportsObj = trackerService.getRichPageManager().getRichPages().project(projectId)
	                .space(space.getName());
	        System.out.println("live Reports" + liveReportsObj + "\n");

	        // Create a list to store report details for this space
	        List<Map<String, String>> spaceReportDetails = new ArrayList<>();

	        for (IRichPage report : liveReportsObj) {
	            System.out.println();
	            String reportId = report.getPageName();
	            if (!reportId.equals("Home")) {
	                Date created = report.getCreated();
	                Date updatedDate = report.getUpdated();
	                String pattern = "dd-MM-yyyy";
	                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	                String CreatedDate = simpleDateFormat.format(created);
	                String UpdatedDate = simpleDateFormat.format(updatedDate);

	                // Create a map to store report details
	                Map<String, String> reportDetails = new LinkedHashMap<>();
	                reportDetails.put("folderName", space.getName());
	                reportDetails.put("createdDate", CreatedDate.toString());
	                reportDetails.put("updatedDate", UpdatedDate.toString());
	                reportDetails.put("reportName", report.getId());

	                // Add report details to the list of reports for this space
	                spaceReportDetails.add(reportDetails);
	            }
	        }

	        // Add all report details for this space to the response map
	        for (Map<String, String> reportDetails : spaceReportDetails) {
	            liveReportDetailsResponseMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
	            liveReportDetailsResponseMap.get(id.get()).putAll(reportDetails);
	            id.getAndIncrement();
	        }
	    }
	}


	private void getWorkItemWorkFlowFunctionDetails(ITypeOpt wiType, ITrackerProject project) {
		AtomicInteger id = new AtomicInteger(0);
		IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig("WorkItem", wiType.getId(),
				project.getContextId());
		Collection<IAction> actions = workFlow.getActions();
		for (IAction action : actions) {
			Set<IOperation> functions = action.getFunctions();
			for (IOperation functionOperation : functions) {
				if (functionOperation.getName().equals("ScriptFunction")) {
					for (Map.Entry<String, String> entry : functionOperation.getParams().entrySet()) {
						if (entry.getKey().equalsIgnoreCase("script")) {
							System.out.println("action " + action.getId() + "," + entry.getValue());
							customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
							customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
							customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
							customizationDetailsResponseData.get(id.get()).put("attachedJsFile", entry.getValue());
							id.getAndIncrement();
						}
					}
				}
			}
		}

	}

}