package com.intelizign.admin_custom_management.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

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
	public Map<Integer, Map<String, Object>> getLicenseDetailsMap  = new LinkedHashMap<>(); 
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

	//Get all Project From the Current Server
	@Override

	public void getProjectList(HttpServletRequest req, HttpServletResponse resp) {
	    try {
	        IPObjectList<IProject> getProjectList = trackerService.getProjectsService().searchProjects("", "id");

	        Map<String, String> projectsObjMap = getProjectList.stream()
	                .map(pro -> {
	                    try {
	                        return Map.entry(pro.getId(), pro.getName());
	                    } catch (UnresolvableObjectException e) {
	                        log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
	                        return null;
	                    }
	                })
	                .filter(entry -> entry != null)
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> existing, LinkedHashMap::new));
	        
	        Map<String, Object> responseObject = new LinkedHashMap<>();
	        responseObject.put("projectsList", projectsObjMap);

	        ObjectMapper objectMapper = new ObjectMapper();
	        String jsonResponse = objectMapper.writeValueAsString(responseObject);

	        resp.setContentType("application/json");
	        resp.getWriter().write(jsonResponse);
	    } catch (Exception e) {
	        log.error("Error occurred while fetching project list: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

	//Base Method For get ModuleCustomizationCount and WorkItem Customization Count
	@Override
	public void getCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
	    try {
	        String projectId = req.getParameter("projectId");
	        ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
	        List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
	                .getAvailableOptions(WORKITEM_TYPE);

	       
	      
	        Stream.of(liveReportDetailsResponseMap, pluginDetailsMap, prePostSaveScriptMap, getLicenseDetailsMap)
	            .filter(Objects::nonNull)
	            .forEach(Map::clear);

	        List<Map<String, Object>> customizationCountDetailsList = workItemTypeEnum.stream()
	                .map(wiTypeEnum -> {
	                    getWorkItemCustomizationCount(projectObject, wiTypeEnum);

	                    Map<String, Object> customizationCountDetailsMap = new LinkedHashMap<>();
	                    customizationCountDetailsMap.put("wiType", wiTypeEnum.getId());
	                    customizationCountDetailsMap.put("wiName", wiTypeEnum.getName());
	                    customizationCountDetailsMap.put("wiWorkflowScriptConditionCount", wiWorkflowScriptConditionCount);
	                    customizationCountDetailsMap.put("wiWorkflowScriptFunctionCount", wiWorkflowScriptFunctionCount);
	                    customizationCountDetailsMap.put("customEnumerationCount", customEnumerationCount);
	                    customizationCountDetailsMap.put("wiCustomFieldCount", wiCustomFieldCount);

	                    return customizationCountDetailsMap;
	                })
	                .collect(Collectors.toList());


	        List<Map<String, Object>> moduleCustomizationCountDetailsList = moduleCustomizationService
	                .getModuleCustomizationCountDetails(req, resp);

	       
	        getLiveReportDetails(req, resp);
	        getPluginDetails(req, resp);
	        getprePostSaveScriptDetails(req, resp);
	        getLicenseDetails(req, resp);

	       
	        Map<String, Object> jsonResponse = new LinkedHashMap<>();
	        jsonResponse.put("customizationCountDetails", customizationCountDetailsList);
	        jsonResponse.put("moduleCustomizationDetails", moduleCustomizationCountDetailsList);
	        jsonResponse.put("liveReportDetailsResponse", liveReportDetailsResponseMap);
	        jsonResponse.put("pluginDetails", pluginDetailsMap);
	        jsonResponse.put("prePostSaveScriptDetails", prePostSaveScriptMap);
	        jsonResponse.put("licenseDetails", getLicenseDetailsMap);

	  
	        String jsonResponseString = objectMapper.writeValueAsString(jsonResponse);

	        resp.setContentType("application/json");
	        resp.getWriter().write(jsonResponseString);

	    } catch (Exception e) {
	        System.out.println("Error Message in customization Details is" + e.getMessage());
	        e.printStackTrace();
	    }
	}

	//Read Polarion License File
	 private void getLicenseDetails(HttpServletRequest req, HttpServletResponse resp)  {
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
				                    getLicenseDetailsMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
				                    switch (nodeName) {
				                        case "licenseType":	      
				                        	getLicenseDetailsMap.get(id.get()).put("licenseType", nodeValue);
				                 	       id.getAndIncrement();
				                            break;
				                        case "userCompany":
				                        	getLicenseDetailsMap.get(id.get()).put("userCompany", nodeValue);
				                        	id.getAndIncrement();
				                            break;
				                        case "userEmail":
				                        	getLicenseDetailsMap.get(id.get()).put("userEmail", nodeValue);
				                        	id.getAndIncrement();
				                            break;
				                        case "userName":
				                        	getLicenseDetailsMap.get(id.get()).put("userName", nodeValue);
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
	    
	 //Get Extension Details From the Current Server
	 private void getPluginDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		    String path = System.getProperty("com.polarion.home") + "/extensions/";
		    File directory = new File(path);
		    AtomicInteger id = new AtomicInteger(0);
		    if (directory.exists() && directory.isDirectory()) {
		    	 Arrays.stream(directory.listFiles())
		                .forEach(dir -> {
		                    pluginDetailsMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                    pluginDetailsMap.get(id.get()).put("pluginDetails", dir.getName());
		                    pluginDetailsMap.get(id.get()).put("pluginPath", dir.getAbsolutePath());
		                    id.getAndIncrement();
		                });
		    }else{
		    	log.error("The Specified directory not exists");
		    }
		    
		}
	 
	 //Check Function Content in PrePost Save Script File
	 private String extractFunctionNames(String content) {
		    Pattern pattern = Pattern.compile("function\\s+([\\w\\d_]+)\\s*\\(");
		    Matcher matcher = pattern.matcher(content);
		    List<String> functionNames = new ArrayList<>();
		    while (matcher.find()) {
		        functionNames.add(matcher.group(1));
		    }
		    return functionNames.isEmpty() ? "No functions found in the file." :
		            functionNames.stream().collect(Collectors.joining(", "));
		}

	//Read PrePost Save Script File Content
	 private String readFileContent(File file) {
		    try (Stream<String> lines = Files.lines(file.toPath())) {
		        return lines.collect(Collectors.joining("\n"));
		    } catch (IOException ex) {
		        ex.printStackTrace();
		        return null;
		    }
		}


	//Get PrePost save Hook Details
	 private void getprePostSaveScriptDetails(HttpServletRequest req, HttpServletResponse resp) {
		    String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
		    File folder = new File(folderPath);
		    try {
		        if (folder.exists() && folder.isDirectory()) {
		            AtomicInteger id = new AtomicInteger(0);
		            Arrays.stream(folder.listFiles())
		                    .forEach(jsFile -> {
		                        try {
		                            String content = readFileContent(jsFile);
		                            Map<String, Object> scriptData = new HashMap<>();
		                            scriptData.put("jsName", jsFile.getName());
		                            String functionNames = content.contains(WORKFLOW_FUNCTION_KEY) ?
		                                    extractFunctionNames(content) : "No functions found in the file.";
		                            prePostSaveScriptMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                            prePostSaveScriptMap.get(id.get()).put("Name", jsFile.getName());
		                            prePostSaveScriptMap.get(id.get()).put("Extension", functionNames);
		                            id.getAndIncrement();
		                        } catch (Exception e) {
		                            e.printStackTrace();
		                        }
		                    });
		        } else {
		            log.error("The specified folder does not exist or is not a directory.");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
	 

	 private void getLiveReportDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		    String projectId = req.getParameter("projectId");
		    List<IFolder> spaces = trackerService.getFolderManager().getFolders(projectId);
		    AtomicInteger id = new AtomicInteger(0);

		    spaces.forEach(space -> {
		        Collection<IRichPage> liveReportsObj = trackerService.getRichPageManager().getRichPages().project(projectId)
		                .space(space.getName());
		        liveReportsObj.stream()
		                .filter(report -> !report.getPageName().equals("Home"))
		                .forEach(report -> {
		                    Date created = report.getCreated();
		                    Date updatedDate = report.getUpdated();
		                    String pattern = "dd-MM-yyyy";
		                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		                    String CreatedDate = simpleDateFormat.format(created);
		                    String UpdatedDate = simpleDateFormat.format(updatedDate);
		                    liveReportDetailsResponseMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                    liveReportDetailsResponseMap.get(id.get()).put("folderName", space.getName());
		                    liveReportDetailsResponseMap.get(id.get()).put("createdDate", CreatedDate.toString());
		                    liveReportDetailsResponseMap.get(id.get()).put("updatedDate", UpdatedDate.toString());
		                    liveReportDetailsResponseMap.get(id.get()).put("reportName", report.getTitle());
		                    id.getAndIncrement();
		                });
		    });
		}


	
	/*Get WorkItem Customization Count
	  --Enumeration Count
	  --Script ConditionCount with Attached Js FileName
	  --Script FunctionCount  with Attached Js FileName
	  */
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

		//Get WorkItem CustomField Count
		private void getWorkItemCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		    try {
		        ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		        Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
		                pro.getContextId(), wiTypeEnum.getId());
		        wiCustomFieldCount = customFieldList.size();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		//Get WorkItem Script Count
		private void getWorkItemWorkFlowSciptCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		    try {
		        IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(
		                WorkItemCustomizationServiceImpl.WORKITEM_PROTOTYPE, wiTypeEnum.getId(), pro.getContextId());
		        Collection<IAction> actions = workFlow.getActions();
		        getWorkItemWorkFlowFunctionCount(actions, wiTypeEnum);
		        wiWorkflowScriptConditionCount = (int) actions.stream()
		                .flatMap(action -> action.getConditions().stream())
		                .filter(condition -> condition.getName().equals(WORKFLOW_CONDITION_KEY))
		                .flatMap(condition -> condition.getParams().entrySet().stream())
		                .filter(entry -> entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER))
		                .count();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		//Get WorkItem Function Count
		private void getWorkItemWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) {
		    try {
		        wiWorkflowScriptFunctionCount = (int) actions.stream()
		                .flatMap(action -> action.getFunctions().stream())
		                .filter(function -> function.getName().equals(WORKFLOW_FUNCTION_KEY))
		                .count();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		//Get WorkItem Enumeration Count
		private void getWorkItemCustomEnumerationCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
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
		            Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
		            long count = reader.lines()
		                    .flatMap(line -> {
		                        java.util.regex.Matcher matcher = pattern.matcher(line);
		                        List<String> matches = new ArrayList<>();
		                        while (matcher.find()) {
		                            String extractedText = matcher.group(2);
		                            if (typeIds.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
		                                    && !extractedText.contains("custom-fields")
		                                    && !extractedText.contains("calculated-fields")) {
		                                String typeIdPrefix = extractedText.substring(0, extractedText.indexOf('-'));
		                                if (typeIdPrefix.equals(wiTypeEnum.getId())) {
		                                    matches.add(extractedText);
		                                }
		                            }
		                        }
		                        return matches.stream();
		                    }).count();
		            customEnumerationCount = (int) count;
		            System.out.println("Custom Enumeration Count is"+ customEnumerationCount +"\n");
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

		/*Get Customization Details In WorkItem
		 * Custom Fields  -- custom Id, CustomType
		 * Script Condition -- actionId, actionName, FileName
		 * Script Function -- actionId, actionName, FileName
		 * Custom Enumeration -- EnumerationId
		 */
		@Override
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
		            workItemTypeEnum.stream()
		                    .filter(wiType -> wiType.getId().equalsIgnoreCase(type))
		                    .forEach(wiType -> {
								try {
									redirectWorkItemCustomization(heading, wiType, trackerPro);
								} catch (Exception e) {
									e.printStackTrace();
								}
							});

		            moduleTypeEnum.stream()
		                    .filter(moduleType -> moduleType.getId().equalsIgnoreCase(type))
		                    .forEach(moduleType -> {
								try {
									customizationDetailsResponseData = moduleCustomizationService
									        .getModuleCustomizationDetails(trackerPro, moduleType, heading);
								} catch (Exception e) {
									e.printStackTrace();
								}
							});

		        } else {
		            log.error("Passing Data is Not Acceptable");
		        }

		        Map<String, Object> jsonResponse = new LinkedHashMap<>();
		        jsonResponse.put("customizationDetailsResponseData", customizationDetailsResponseData);

		        String customizationDetailsResponseJson = objectMapper.writeValueAsString(jsonResponse);

		        resp.setContentType("application/json");
		        resp.getWriter().write(customizationDetailsResponseJson);

		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}

		private void redirectWorkItemCustomization(String heading, ITypeOpt workItemType, ITrackerProject trackerPro) throws Exception {
		    switch (heading) {
		        case "wiCustomFieldCount":
		            getWorkItemCustomFieldDetails(workItemType, trackerPro);
		            break;
		        case "customEnumerationCount":
		            getWorkItemCustomEnumerationCount(trackerPro, workItemType);
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

		//Get Enumeration Details
		@SuppressWarnings("unused")
		private void getcustomEnumerationDetails(ITypeOpt wiType, ITrackerProject project) throws Exception {
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
		private void processEnumeration(BufferedReader readEnumerationXml, List<String> wiTypeList, ITypeOpt wiType) throws IOException {
		    AtomicInteger id = new AtomicInteger(0);
		    Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
		    readEnumerationXml.lines()
		            .flatMap(line -> {
		                Matcher matcher = pattern.matcher(line);
		                List<String> matches = new ArrayList<>();
		                while (matcher.find()) {
		                    String extractedText = matcher.group(2);
		                    if (wiTypeList.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
		                            && !extractedText.contains("custom-fields") && !extractedText.contains("calculated-fields")) {
		                        String wiTypePrefix = extractedText.substring(0, extractedText.indexOf('-'));
		                        if (wiTypePrefix.equals(wiType.getId())) {
		                            matches.add(extractedText);
		                        }
		                    }
		                }
		                return matches.stream();
		            })
		            .forEach(extractedText -> {
		                customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                customizationDetailsResponseData.get(id.get()).put("customEnumeration", extractedText);
		                id.getAndIncrement();
		            });
		}

		private void getWorkItemWorkFlowConditionDetails(ITypeOpt wiType, ITrackerProject project) {
		    AtomicInteger id = new AtomicInteger(0);
		    try {
		        IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(WORKITEM_PROTOTYPE,
		                wiType.getId(), project.getContextId());
		        workFlow.getActions().forEach(action -> {
		            action.getConditions().forEach(conditionsOperation -> {
		                if (conditionsOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
		                    conditionsOperation.getParams().forEach((key, value) -> {
		                        if (key.equalsIgnoreCase(SCRIPT_PARAMETER)) {
		                            customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                            customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
		                            customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
		                            customizationDetailsResponseData.get(id.get()).put("attachedJsFile", value);
		                            id.getAndIncrement();
		                        }
		                    });
		                }
		            });
		        });
		    } catch (Exception e) {
		        log.error("Error while Fetching Data in WorkItemWorkflow Condition" + e.getMessage());
		    }
		}

		private void getWorkItemCustomFieldDetails(ITypeOpt wiType, ITrackerProject projectId) {
		    AtomicInteger id = new AtomicInteger(0);
		    ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		    Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
		            projectId.getContextId(), wiType.getId());
		    customFieldList.forEach(cust -> {
		        customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		        customizationDetailsResponseData.get(id.get()).put("customId", cust.getId());
		        customizationDetailsResponseData.get(id.get()).put("customName", cust.getName());
		        if (cust.getType() instanceof IPrimitiveType) {
		            IPrimitiveType modulePrimitiveType = (IPrimitiveType) cust.getType();
		            customizationDetailsResponseData.get(id.get()).put("customType", modulePrimitiveType.getTypeName());
		        }
		        id.getAndIncrement();
		    });
		}

		private void getWorkItemWorkFlowFunctionDetails(ITypeOpt wiType, ITrackerProject project) {
		    AtomicInteger id = new AtomicInteger(0);
		    IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(WORKITEM_PROTOTYPE, wiType.getId(),
		            project.getContextId());
		    workFlow.getActions().forEach(action -> {
		        action.getFunctions().forEach(functionOperation -> {
		            if (functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY)) {
		                functionOperation.getParams().forEach((key, value) -> {
		                    if (key.equalsIgnoreCase(SCRIPT_PARAMETER)) {
		                        customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
		                        customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
		                        customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
		                        customizationDetailsResponseData.get(id.get()).put("attachedJsFile", value);
		                        id.getAndIncrement();
		                    }
		                });
		            }
		        });
		    });
		}


}